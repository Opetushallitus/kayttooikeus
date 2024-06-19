package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.CasUserAttributes;
import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.*;
import fi.vm.sade.kayttooikeus.service.IdentificationService;
import fi.vm.sade.kayttooikeus.service.exception.DataInconsistencyException;
import fi.vm.sade.kayttooikeus.service.exception.LoginTokenNotFoundException;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.exception.ValidationException;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static fi.vm.sade.kayttooikeus.model.Identification.*;
import static fi.vm.sade.kayttooikeus.util.FunctionalUtils.ifPresentOrElse;
import static java.util.stream.Collectors.joining;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentificationServiceImpl implements IdentificationService {

    private final IdentificationRepository identificationRepository;
    private final HenkiloDataRepository henkiloDataRepository;
    private final KutsuRepository kutsuRepository;
    private final TunnistusTokenDataRepository tunnistusTokenDataRepository;
    private final OrikaBeanMapper mapper;

    private final OppijanumerorekisteriClient oppijanumerorekisteriClient;
    private final KayttajatiedotRepository kayttajatiedotRepository;

    @Override
    @Transactional
    public String generateAuthTokenForHenkilo(String oid, String idpKey, String idpIdentifier) {
        log.info("generateAuthTokenForHenkilo henkilo:[{}] idp:[{}] identifier:[{}]", oid, idpKey, idpIdentifier);
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
        if ("mpassid".equals(idpKey)) {
            return getHenkiloByOppijanumero(idpIdentifier)
                .map(HenkiloDto::getOidHenkilo)
                .filter(oid -> kayttajatiedotRepository.findByHenkiloOidHenkilo(oid).isPresent())
                .orElseGet(() -> getHenkiloOidFromExplicityIdpIdentifierMapping(idpKey, idpIdentifier));
        }
        return getHenkiloOidFromExplicityIdpIdentifierMapping(idpKey, idpIdentifier);
    }

    public String getHenkiloOidFromExplicityIdpIdentifierMapping(String idpKey, String idpIdentifier) {
        return this.identificationRepository.findByidpEntityIdAndIdentifier(idpKey, idpIdentifier)
                .orElseThrow(() -> new NotFoundException("Identification not found"))
                .getHenkilo()
                .getOidHenkilo();
    }

    private Optional<HenkiloDto> getHenkiloByOppijanumero(String oppijanumero) {
        Map<String, HenkiloDto> masters = oppijanumerorekisteriClient.getMasterHenkilosByOidList(List.of(oppijanumero));
        return Optional.ofNullable(masters.get(oppijanumero));
    }

    @Override
    @Transactional
    public CasUserAttributes findByTokenAndInvalidateToken(String token) {
        log.info("validateAuthToken:[{}]", token);
        Identification identification = identificationRepository.findByAuthtokenIsValid(token)
                .orElseThrow(() -> new NotFoundException("identification not found or token is invalid"));
        identification.setAuthtoken(null);
        return CasUserAttributes.fromIdentification(identification);
    }

    @Override
    @Transactional
    public String updateIdentificationAndGenerateTokenForHenkiloByOid(String oidHenkilo) {
        Henkilo henkilo = this.henkiloDataRepository.findByOidHenkilo(oidHenkilo)
                .orElseThrow(() -> new NotFoundException("Henkilo not found with oid " + oidHenkilo));
        return generateAuthTokenForHenkilo(henkilo, STRONG_AUTHENTICATION_IDP, henkilo.getKayttajatiedot().getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getTunnisteetByHenkiloAndIdp(String identityProvider, String oid) {
        validateLinkitettyIdentityProvider(identityProvider);
        List<Identification> identifications = findIdentificationsByHenkiloAndIdp(oid, identityProvider);
        return identifications.stream().map(Identification::getIdentifier).collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public Set<String> updateTunnisteetByHenkiloAndIdp(String identityProvider, String oid, Set<String> tunnukset) {
        validateLinkitettyIdentityProvider(identityProvider);
        Henkilo henkilo = henkiloDataRepository.findByOidHenkilo(oid)
                .orElseThrow(() -> new NotFoundException("Henkilo not found"));

        // tunniste tulee olla uniikki
        if (!tunnukset.isEmpty()) {
            Set<String> duplikaatit = identificationRepository
                    .findByidpEntityIdAndIdentifierIn(identityProvider, tunnukset).stream()
                    .filter(identification -> !identification.getHenkilo().equals(henkilo))
                    .map(Identification::getIdentifier)
                    .collect(Collectors.toSet());
            if (!duplikaatit.isEmpty()) {
                throw new ValidationException(String.format("Tunnisteet '%s' ovat jo käytössä",
                        duplikaatit.stream().collect(joining(", "))));
            }
        }

        List<Identification> identifications = findIdentificationsByHenkiloAndIdp(oid, identityProvider);
        List<String> identifiers = identifications.stream().map(Identification::getIdentifier).collect(Collectors.toList());
        // poistot
        identifications.stream()
                .filter(identification -> !tunnukset.contains(identification.getIdentifier()))
                .forEach(identificationRepository::delete);
        // lisäykset
        tunnukset.stream()
                .filter(tunnus -> !identifiers.contains(tunnus))
                .map(tunnus -> new Identification(henkilo, identityProvider, tunnus))
                .forEach(identificationRepository::save);

        return tunnukset;
    }
    private void validateLinkitettyIdentityProvider(String identityProvider) {
        Set<String> validIdentityProviders = Set.of(HAKA_AUTHENTICATION_IDP, MPASSID_AUTHENTICATION_IDP);
        if (!validIdentityProviders.contains(identityProvider)) {
            throw new ValidationException(String.format("IdP '%s' ei ole tunnettu", identityProvider));
        }
    }

    @Override
    @Transactional
    public Optional<String> updateKutsuAndGenerateTemporaryKutsuToken(String kutsuToken, String hetu, String etunimet, String sukunimi) {
        return this.kutsuRepository.findBySalaisuusIsValid(kutsuToken)
                .map(kutsu -> updateKutsuAndGenerateTemporaryKutsuToken(kutsu, hetu, etunimet, sukunimi));
    }

    private String updateKutsuAndGenerateTemporaryKutsuToken(Kutsu kutsu, String hetu, String etunimet, String sukunimi) {
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
    public Optional<TunnistusToken> updateLoginToken(String loginToken, String hetu) {
        return tunnistusTokenDataRepository.findByValidLoginToken(loginToken)
                .map(tunnistusToken -> updateLoginToken(tunnistusToken, hetu));
    }

    public TunnistusToken updateLoginToken(TunnistusToken tunnistusToken, String hetu) {
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
    public String consumeLoginToken(String loginToken, String authentication_idp) {
        TunnistusToken tunnistusToken = this.tunnistusTokenDataRepository.findByLoginToken(loginToken)
                .orElseThrow(() -> new DataInconsistencyException("Login token not found " + loginToken));
        Henkilo henkilo = tunnistusToken.getHenkilo();
        tunnistusToken.setKaytetty(LocalDateTime.now());
        Kayttajatiedot kayttajatiedot = henkilo.getKayttajatiedot();
        return generateAuthTokenForHenkilo(henkilo, authentication_idp, kayttajatiedot.getUsername());
    }

    @Override
    public String getIdpEntityIdForCurrentSession() {
        CasAuthenticationToken token = (CasAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> attributes = token.getAssertion().getPrincipal().getAttributes();
        return (String) attributes.get("idpEntityId");
    }

    private List<Identification> findIdentificationsByHenkiloAndIdp(String oid, String idp) {
        return identificationRepository.findByHenkiloOidHenkiloAndIdpEntityId(oid, idp);
    }

    private void createIdentification(Henkilo henkilo, String token, String identifier, String idpKey) {
        log.info("creating new identification token:[{}]", token);
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
        log.info("old identification found, setting new token:[{}]", token);
    }

    private String generateToken() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(128, random).toString(32);
    }

}
