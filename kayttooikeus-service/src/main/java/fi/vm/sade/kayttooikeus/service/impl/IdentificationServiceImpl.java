package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.dto.AsiointikieliDto;
import fi.vm.sade.kayttooikeus.dto.IdentifiedHenkiloTypeDto;
import fi.vm.sade.kayttooikeus.dto.YhteystietojenTyypit;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.Identification;

import static fi.vm.sade.kayttooikeus.model.Identification.HAKA_AUTHENTICATION_IDP;
import static fi.vm.sade.kayttooikeus.model.Identification.STRONG_AUTHENTICATION_IDP;

import fi.vm.sade.kayttooikeus.model.Kutsu;
import fi.vm.sade.kayttooikeus.model.TunnistusToken;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.repositories.IdentificationRepository;
import fi.vm.sade.kayttooikeus.repositories.KutsuDataRepository;
import fi.vm.sade.kayttooikeus.repositories.TunnistusTokenDataRepository;
import fi.vm.sade.kayttooikeus.service.IdentificationService;
import fi.vm.sade.kayttooikeus.service.KayttoOikeusService;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.kayttooikeus.util.YhteystietoUtil;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloVahvaTunnistusDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoTyyppi;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService;

@Service
@RequiredArgsConstructor
public class IdentificationServiceImpl extends AbstractService implements IdentificationService {

    private final IdentificationRepository identificationRepository;
    private final HenkiloDataRepository henkiloDataRepository;
    private final KutsuDataRepository kutsuDataRepository;
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

        Optional<Identification> identification = henkilo.getIdentifications().stream()
                .filter(henkiloIdentification ->
                        idpIdentifier.equals(henkiloIdentification.getIdentifier()) && idpKey.equals(henkiloIdentification.getIdpEntityId()))
                .findFirst();

        String token = generateToken();
        if (identification.isPresent()) {
            updateToken(identification.get(), token);
        } else {
            createIdentification(henkilo, token, idpIdentifier, idpKey);
        }

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
        dto.setHenkiloTyyppi(perustiedot.getHenkiloTyyppi().name());
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
    public String updateIdentificationAndGenerateTokenForHenkiloByHetu(String hetu) {
        String oid = oppijanumerorekisteriClient.getOidByHetu(hetu);
        Henkilo henkilo = henkiloDataRepository.findByOidHenkilo(oid).orElseThrow(()
                -> new NotFoundException("henkilo not found with oid " + oid));
        String token = generateToken();
        Identification identification = henkilo.getIdentifications().stream()
                .filter(ident -> STRONG_AUTHENTICATION_IDP.equals(ident.getIdpEntityId()))
                .findFirst().orElseGet(() -> {
                    Identification ident = new Identification();
                    henkilo.getIdentifications().add(ident);
                    ident.setHenkilo(henkilo);
                    return ident;
                });
        identification.setIdpEntityId(STRONG_AUTHENTICATION_IDP);
        identification.setIdentifier(henkilo.getKayttajatiedot().getUsername());
        identification.setAuthtoken(token);
        identification.setAuthTokenCreated(LocalDateTime.now());

        return token;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getHakatunnuksetByHenkiloAndIdp(String oid, String ipdKey) {
        List<Identification> identifications = findIdentificationsByHenkiloAndIdp(oid, HAKA_AUTHENTICATION_IDP);
        return identifications.stream().map(Identification::getIdentifier).collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public Set<String> updateHakatunnuksetByHenkiloAndIdp(String oid, Set<String> hakatunnukset) {
        Henkilo henkilo = henkiloDataRepository.findByOidHenkilo(oid)
                .orElseThrow(() -> new NotFoundException("Henkilo not found"));
        List<Identification> identifications = findIdentificationsByHenkiloAndIdp(oid, HAKA_AUTHENTICATION_IDP);
        identificationRepository.delete(identifications);
        List<Identification> updatedIdentifications = hakatunnukset.stream()
                .map(hakatunnus -> new Identification(henkilo, HAKA_AUTHENTICATION_IDP, hakatunnus)).collect(Collectors.toList());
        identificationRepository.save(updatedIdentifications);
        ldapSynchronizationService.updateHenkiloAsap(oid);
        return hakatunnukset;
    }

    @Override
    @Transactional
    public String updateKutsuAndGenerateTemporaryKutsuToken(String kutsuToken, String hetu, String etunimet, String sukunimi) {
        Kutsu kutsu = this.kutsuDataRepository.findBySalaisuusIsValid(kutsuToken)
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
    public String createLoginToken(String oidHenkilo) {
        Henkilo henkilo = this.henkiloDataRepository.findByOidHenkilo(oidHenkilo)
                .orElseThrow(() -> new NotFoundException("Henkilo not found with oid " + oidHenkilo));
        TunnistusToken tunnistusToken = new TunnistusToken(this.generateToken(), henkilo, LocalDateTime.now(), null);
        this.tunnistusTokenDataRepository.save(tunnistusToken);
        return tunnistusToken.getLoginToken();
    }

    // Hae henkilön tiedot jotka liittyvät logintokeniin
    // Päivitä henkilölle hetu jos ei ole ennestään olemassa ja merkitse se vahvistetuksi. Muuten tarkista, että hetu täsmää.
    // Luo auth token
    // Redirectaa CAS:iin auth tokenin kanssa.
    @Override
    @Transactional
    public String handleStrongIdentification(String hetu, String etunimet, String sukunimi, String loginToken) {
        TunnistusToken tunnistusToken = this.tunnistusTokenDataRepository.findByValidLoginToken(loginToken)
                .orElseThrow(() -> new NotFoundException("Login token not found " + loginToken));
        Henkilo henkilo = tunnistusToken.getHenkilo();

        this.oppijanumerorekisteriClient.setStrongIdentifiedHetu(henkilo.getOidHenkilo(),
                new HenkiloVahvaTunnistusDto(hetu, etunimet, sukunimi));

        tunnistusToken.setKaytetty(LocalDateTime.now());
        return this.generateAuthTokenForHenkilo(henkilo, STRONG_AUTHENTICATION_IDP, henkilo.getKayttajatiedot().getUsername());
    }

    private List<Identification> findIdentificationsByHenkiloAndIdp(String oid, String idp) {
        return identificationRepository.findByHenkiloOidHenkiloAndIdpEntityId(oid, idp);
    }

    private void createIdentification(Henkilo henkilo, String token, String identifier, String idpKey) {
        logger.info("creating new identification token:[{}]", token);
        Identification identification = new Identification();
        identification.setHenkilo(henkilo);

        if (henkilo.getIdentifications() == null) {
            henkilo.setIdentifications(new HashSet<>());
        }

        henkilo.getIdentifications().add(identification);
        identification.setIdentifier(identifier);
        identification.setIdpEntityId(idpKey);
        identification.setAuthtoken(token);
        identification.setAuthTokenCreated(LocalDateTime.now());
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
