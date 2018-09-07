package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.dto.AsiointikieliDto;
import fi.vm.sade.kayttooikeus.dto.IdentifiedHenkiloTypeDto;
import fi.vm.sade.kayttooikeus.dto.YhteystietojenTyypit;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.repositories.IdentificationRepository;
import fi.vm.sade.kayttooikeus.repositories.KutsuRepository;
import fi.vm.sade.kayttooikeus.repositories.TunnistusTokenDataRepository;
import fi.vm.sade.kayttooikeus.service.IdentificationService;
import fi.vm.sade.kayttooikeus.service.KayttoOikeusService;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService;
import fi.vm.sade.kayttooikeus.service.exception.DataInconsistencyException;
import fi.vm.sade.kayttooikeus.service.exception.LoginTokenNotFoundException;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.exception.ValidationException;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.kayttooikeus.util.YhteystietoUtil;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoTyyppi;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static fi.vm.sade.kayttooikeus.model.Identification.*;
import static fi.vm.sade.kayttooikeus.util.FunctionalUtils.ifPresentOrElse;
import static java.util.stream.Collectors.joining;

@Service
@RequiredArgsConstructor
public class IdentificationServiceImpl extends AbstractService implements IdentificationService {

    private final IdentificationRepository identificationRepository;
    private final HenkiloDataRepository henkiloDataRepository;
    private final KutsuRepository kutsuRepository;
    private final TunnistusTokenDataRepository tunnistusTokenDataRepository;

    private final KayttoOikeusService kayttoOikeusService;
    private final LdapSynchronizationService ldapSynchronizationService;

    private final OrikaBeanMapper mapper;

    private final OppijanumerorekisteriClient oppijanumerorekisteriClient;

    @Override
    @Transactional
    public String generateAuthTokenForHenkilo(String oid, String idpKey, String idpIdentifier) {
        logger.info("generateAuthTokenForHenkilo henkilo:[{}] idp:[{}] identifier:[{}]", oid, idpKey, idpIdentifier);
        Henkilo henkilo = henkiloDataRepository.findByOidHenkilo(oid)
                .orElseThrow(() -> new NotFoundException("no henkilo found with oid:[" + oid + "]"));
        return this.generateAuthTokenForHenkilo(henkilo, idpKey, idpIdentifier);
    }

    @Override
    @Transactional
    public String generateAuthTokenForHenkilo(Henkilo henkilo, String idpKey, String idpIdentifier) {
        String token = generateToken();
        ifPresentOrElse(identificationRepository.findByidpEntityIdAndIdentifier(idpKey, idpIdentifier),
                identification -> updateIdentification(henkilo, token, identification),
                () -> createIdentification(henkilo, token, idpIdentifier, idpKey));
        return token;
    }

    @Override
    @Transactional(readOnly = true)
    public String getHenkiloOidByIdpAndIdentifier(String idpKey, String idpIdentifier) {
        return this.identificationRepository.findByidpEntityIdAndIdentifier(idpKey, idpIdentifier)
                .orElseThrow(() -> new NotFoundException("Identification not found"))
                .getHenkilo()
                .getOidHenkilo();
    }

    @Override
    @Transactional
    public IdentifiedHenkiloTypeDto findByTokenAndInvalidateToken(String token) {
        logger.info("validateAuthToken:[{}]", token);
        Identification identification = identificationRepository.findByAuthtokenIsValid(token)
                .orElseThrow(() -> new NotFoundException("identification not found or token is invalid"));
        identification.setAuthtoken(null);

        HenkiloDto perustiedot = oppijanumerorekisteriClient.getHenkiloByOid(identification.getHenkilo().getOidHenkilo());
        IdentifiedHenkiloTypeDto dto = mapper.map(identification, IdentifiedHenkiloTypeDto.class);
        dto.setHenkiloTyyppi(identification.getHenkilo().getKayttajaTyyppi());
        dto.setPassivoitu(perustiedot.isPassivoitu());
        dto.setAuthorizationData(kayttoOikeusService.findAuthorizationDataByOid(dto.getOidHenkilo()));

        dto.setEtunimet(perustiedot.getEtunimet());
        dto.setKutsumanimi(perustiedot.getKutsumanimi());
        dto.setSukunimi(perustiedot.getSukunimi());
        dto.setHetu(perustiedot.getHetu());
        if (!StringUtils.isEmpty(perustiedot.getSukupuoli())) {
            dto.setSukupuoli(perustiedot.getSukupuoli().equals("1") ? "MIES" : "NAINEN");
        }
        if (perustiedot.getAsiointiKieli() != null) {
            dto.setAsiointiKieli(new AsiointikieliDto(perustiedot.getAsiointiKieli().getKieliKoodi(), perustiedot.getAsiointiKieli().getKieliTyyppi()));
        }

        YhteystietoUtil.getYhteystietoArvo(perustiedot.getYhteystiedotRyhma(),
                YhteystietoTyyppi.YHTEYSTIETO_SAHKOPOSTI,
                YhteystietojenTyypit.PRIORITY_ORDER).ifPresent(email -> {
                    dto.setEmail(email);
                    identification.setEmail(email);
                });
        return dto;
    }

    @Override
    @Transactional
    public String updateIdentificationAndGenerateTokenForHenkiloByOid(String oidHenkilo) {
        Henkilo henkilo = this.henkiloDataRepository.findByOidHenkilo(oidHenkilo)
                .orElseThrow(() -> new NotFoundException("Henkilo not found with oid " + oidHenkilo));
        return generateAuthTokenForHenkilo(henkilo, STRONG_AUTHENTICATION_IDP, henkilo.getKayttajatiedot().getUsername());
    }

    @Override
    @Transactional
    public String updateIdentificationAndGenerateTokenForHenkiloByHetu(String hetu) {
        return this.updateIdentificationAndGenerateTokenForHenkiloByOid(this.oppijanumerorekisteriClient.getOidByHetu(hetu));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getHakatunnuksetByHenkiloAndIdp(String oid) {
        return this.getHenkiloIdentificationsByIdp(oid, HAKA_AUTHENTICATION_IDP);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getEmailIdentifications(String oid) {
        return this.getHenkiloIdentificationsByIdp(oid, WEAK_AUTHENTICATION_IDP);
    }

    private Set<String> getHenkiloIdentificationsByIdp(String oid, String idp) {
        List<Identification> identifications = findIdentificationsByHenkiloAndIdp(oid, idp);
        return identifications.stream().map(Identification::getIdentifier).collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public Set<String> updateSahkopostitunnisteet(String oid, Set<String> sahkopostitunnisteet) {
        return this.updateHenkiloIdentificationsByIdp(oid, sahkopostitunnisteet, WEAK_AUTHENTICATION_IDP);
    }

    @Override
    @Transactional
    public Set<String> updateHakatunnuksetByHenkiloAndIdp(String oid, Set<String> hakatunnukset) {
        return this.updateHenkiloIdentificationsByIdp(oid, hakatunnukset, HAKA_AUTHENTICATION_IDP);
    }

    private Set<String> updateHenkiloIdentificationsByIdp(String henkiloOid, Set<String> tunnisteet, String idp) {
        Henkilo henkilo = henkiloDataRepository.findByOidHenkilo(henkiloOid)
                .orElseThrow(() -> new NotFoundException("Henkilo not found"));

        // tunniste tulee olla uniikki
        if (!tunnisteet.isEmpty()) {
            Set<String> duplikaatit = identificationRepository
                    .findByidpEntityIdAndIdentifierIn(idp, tunnisteet).stream()
                    .filter(identification -> !identification.getHenkilo().equals(henkilo))
                    .map(Identification::getIdentifier)
                    .collect(Collectors.toSet());
            if (!duplikaatit.isEmpty()) {
                throw new ValidationException(String.format("Tunnisteet '%s' ovat jo käytössä",
                        duplikaatit.stream().collect(joining(", "))));
            }
        }

        List<Identification> identifications = findIdentificationsByHenkiloAndIdp(henkiloOid, idp);
        List<String> identifiers = identifications.stream().map(Identification::getIdentifier).collect(Collectors.toList());
        // poistot
        identifications.stream()
                .filter(identification -> !tunnisteet.contains(identification.getIdentifier()))
                .forEach(identificationRepository::delete);
        // lisäykset
        tunnisteet.stream()
                .filter(tunniste -> !identifiers.contains(tunniste))
                .map(tunniste -> new Identification(henkilo, idp, tunniste))
                .forEach(identificationRepository::save);

        ldapSynchronizationService.updateHenkiloAsap(henkiloOid);
        return tunnisteet;
    }

    @Override
    @Transactional
    public String updateKutsuAndGenerateTemporaryKutsuToken(String kutsuToken, String hetu, String etunimet, String sukunimi) {
        Kutsu kutsu = this.kutsuRepository.findBySalaisuusIsValid(kutsuToken)
                .orElseThrow(() -> new NotFoundException("Kutsu not found with token " + kutsuToken + " or token is invalid"));
        kutsu.setHetu(hetu);
        kutsu.setEtunimi(etunimet);
        kutsu.setSukunimi(sukunimi);
        kutsu.setTemporaryToken(this.generateToken());
        kutsu.setTemporaryTokenCreated(LocalDateTime.now());
        return kutsu.getTemporaryToken();
    }

    @Override
    @Transactional
    public String createLoginToken(String oidHenkilo, Boolean salasananVaihto, String hetu) {
        Henkilo henkilo = this.henkiloDataRepository.findByOidHenkilo(oidHenkilo)
                .orElseThrow(() -> new NotFoundException("Henkilo not found with oid " + oidHenkilo));
        TunnistusToken tunnistusToken = new TunnistusToken(this.generateToken(), henkilo, LocalDateTime.now(), null, hetu, salasananVaihto);
        this.tunnistusTokenDataRepository.save(tunnistusToken);
        return tunnistusToken.getLoginToken();
    }

    @Override
    @Transactional
    public TunnistusToken updateLoginToken(String loginToken, String hetu) {
        TunnistusToken tunnistusToken = tunnistusTokenDataRepository.findByValidLoginToken(loginToken)
                .orElseThrow(() -> new LoginTokenNotFoundException("Login token not found " + loginToken));
        tunnistusToken.setHetu(hetu);
        return tunnistusTokenDataRepository.save(tunnistusToken);
    }

    @Override
    @Transactional(readOnly = true)
    public TunnistusToken getByValidLoginToken(String loginToken) {
        return tunnistusTokenDataRepository.findByValidLoginToken(loginToken)
                .orElseThrow(() -> new LoginTokenNotFoundException("Login token not found " + loginToken));
    }

    @Override
    @Transactional
    public String consumeLoginToken(String loginToken) {
        TunnistusToken tunnistusToken = this.tunnistusTokenDataRepository.findByLoginToken(loginToken)
                .orElseThrow(() -> new DataInconsistencyException("Login token not found " + loginToken));
        Henkilo henkilo = tunnistusToken.getHenkilo();
        henkilo.setVahvastiTunnistettu(true);

        tunnistusToken.setKaytetty(LocalDateTime.now());

        Kayttajatiedot kayttajatiedot = henkilo.getKayttajatiedot();
        return generateAuthTokenForHenkilo(henkilo, STRONG_AUTHENTICATION_IDP, kayttajatiedot.getUsername());
    }

    private List<Identification> findIdentificationsByHenkiloAndIdp(String oid, String idp) {
        return identificationRepository.findByHenkiloOidHenkiloAndIdpEntityId(oid, idp);
    }

    private void createIdentification(Henkilo henkilo, String token, String identifier, String idpKey) {
        logger.info("creating new identification token:[{}]", token);
        Identification identification = new Identification();
        identification.setHenkilo(henkilo);
        identification.setIdentifier(identifier);
        identification.setIdpEntityId(idpKey);
        identification.setAuthtoken(token);
        identification.setAuthTokenCreated(LocalDateTime.now());
        identificationRepository.save(identification);
    }

    private void updateIdentification(Henkilo henkilo, String token, Identification identification) {
        if (!henkilo.equals(identification.getHenkilo())) {
            throw new ValidationException(String.format("Tunniste %s=%s kuuluu toiselle käyttäjälle",
                    identification.getIdpEntityId(), identification.getIdentifier()));
        }
        updateToken(identification, token);
    }

    private void updateToken(Identification identification, String token) {
        identification.setAuthtoken(token);
        identification.setAuthTokenCreated(LocalDateTime.now());
        logger.info("old identification found, setting new token:[{}]", token);
    }

    private String generateToken() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(128, random).toString(32);
    }

}
