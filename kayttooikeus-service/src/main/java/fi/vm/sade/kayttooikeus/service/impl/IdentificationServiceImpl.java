package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.dto.AsiointikieliDto;
import fi.vm.sade.kayttooikeus.dto.IdentifiedHenkiloTypeDto;
import fi.vm.sade.kayttooikeus.dto.YhteystietojenTyypit;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.Identification;
import fi.vm.sade.kayttooikeus.repositories.HenkiloRepository;
import fi.vm.sade.kayttooikeus.repositories.IdentificationRepository;
import fi.vm.sade.kayttooikeus.service.IdentificationService;
import fi.vm.sade.kayttooikeus.service.KayttoOikeusService;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkilonYhteystiedotViewDto;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Optional;

@Service
public class IdentificationServiceImpl extends AbstractService implements IdentificationService {

    private static final String STRONG_AUTHENTICATION_IDP = "vetuma";

    private IdentificationRepository identificationRepository;
    private HenkiloRepository henkiloRepository;
    private KayttoOikeusService kayttoOikeusService;
    private OrikaBeanMapper mapper;
    private OppijanumerorekisteriClient oppijanumerorekisteriClient;

    @Autowired
    public IdentificationServiceImpl(IdentificationRepository identificationRepository,
                                     HenkiloRepository henkiloRepository,
                                     KayttoOikeusService kayttoOikeusService,
                                     OrikaBeanMapper mapper,
                                     OppijanumerorekisteriClient oppijanumerorekisteriClient){
        this.identificationRepository = identificationRepository;
        this.henkiloRepository = henkiloRepository;
        this.kayttoOikeusService = kayttoOikeusService;
        this.mapper = mapper;
        this.oppijanumerorekisteriClient = oppijanumerorekisteriClient;
    }

    @Override
    @Transactional
    public String generateAuthTokenForHenkilo(String oid, String idpKey, String idpIdentifier) {
        logger.info("generateAuthTokenForHenkilo henkilo:[{}] idp:[{}] identifier:[{}]", oid, idpKey, idpIdentifier);
        Henkilo henkilo = henkiloRepository.findByOidHenkilo(oid).orElseThrow(()
                -> new NotFoundException("no henkilo found with oid:[" + oid + "]"));

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
        Identification identification = identificationRepository.findByAuthtoken(token)
                .orElseThrow(() -> new NotFoundException("identification not found"));
        identification.setAuthtoken(null);

        HenkiloPerustietoDto perustiedot = oppijanumerorekisteriClient.getPerustietoByOid(identification.getHenkilo().getOidHenkilo());
        HenkilonYhteystiedotViewDto yhteystiedotDto = oppijanumerorekisteriClient.getYhteystiedotByOid(identification.getHenkilo().getOidHenkilo());
        IdentifiedHenkiloTypeDto dto = mapper.map(identification, IdentifiedHenkiloTypeDto.class);
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

        if (yhteystiedotDto != null) {
            String email = yhteystiedotDto.get(YhteystietojenTyypit.PRIORITY_ORDER).getSahkoposti();
            if (!StringUtils.isEmpty(email)) {
                dto.setEmail(email);
                identification.setEmail(email);
            }
        }
        return dto;
    }

    @Override
    @Transactional
    public String updateIdentificationAndGenerateTokenForHenkiloByHetu(String hetu) {
        String oid = oppijanumerorekisteriClient.getOidByHetu(hetu);
        Henkilo henkilo = henkiloRepository.findByOidHenkilo(oid).orElseThrow(()
                -> new NotFoundException("henkilo not found"));
        String token = generateToken();
        Optional<Identification> henkiloIdentification = henkilo.getIdentifications().stream()
                .filter(identification -> STRONG_AUTHENTICATION_IDP.equals(identification.getIdpEntityId()))
                .findFirst();

        if (henkiloIdentification.isPresent()) {
            Identification identification = henkiloIdentification.get();
            identification.setIdpEntityId(STRONG_AUTHENTICATION_IDP);
            identification.setIdentifier(henkilo.getKayttajatiedot().getUsername());
            identification.setAuthtoken(token);
        } else {
            Identification identification = new Identification();
            henkilo.getIdentifications().add(identification);
            identification.setHenkilo(henkilo);
            identification.setIdpEntityId(STRONG_AUTHENTICATION_IDP);
            identification.setIdentifier(henkilo.getKayttajatiedot().getUsername());
            identification.setAuthtoken(token);
        }

        return token;
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
    }

    private void updateToken(Identification identification, String token) {
        identification.setAuthtoken(token);
        logger.info("old identification found, setting new token:[{}]", token);
    }

    private String generateToken() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(128, random).toString(32);
    }

}
