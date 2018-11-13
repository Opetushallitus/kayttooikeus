package fi.vm.sade.kayttooikeus.service.impl;

import com.querydsl.core.Tuple;
import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.*;
import fi.vm.sade.kayttooikeus.repositories.criteria.KayttooikeusCriteria;
import fi.vm.sade.kayttooikeus.repositories.dto.ExpiringKayttoOikeusDto;
import fi.vm.sade.kayttooikeus.service.KayttoOikeusService;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService;
import fi.vm.sade.kayttooikeus.service.LocalizationService;
import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import fi.vm.sade.kayttooikeus.service.exception.DataInconsistencyException;
import fi.vm.sade.kayttooikeus.service.exception.InvalidKayttoOikeusException;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import fi.vm.sade.kayttooikeus.util.OrganisaatioMyontoPredicate;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.CollectionUtils.isEmpty;

@Service
@RequiredArgsConstructor
public class KayttoOikeusServiceImpl extends AbstractService implements KayttoOikeusService {
    public static final String FI = "FI";
    public static final String SV = "SV";
    public static final String EN = "EN";

    private static final Logger LOGGER = LoggerFactory.getLogger(KayttoOikeusServiceImpl.class);
    private final OrganisaatioClient organisaatioClient;
    private final KayttoOikeusRyhmaRepository kayttoOikeusRyhmaRepository;
    private final KayttoOikeusRepository kayttoOikeusRepository;
    private final LocalizationService localizationService;
    private final OrikaBeanMapper mapper;
    private final KayttoOikeusRyhmaMyontoViiteRepository kayttoOikeusRyhmaMyontoViiteRepository;
    private final MyonnettyKayttoOikeusRyhmaTapahtumaRepository myonnettyKayttoOikeusRyhmaTapahtumaRepository;
    private final KayttoOikeusRyhmaTapahtumaHistoriaRepository kayttoOikeusRyhmaTapahtumaHistoriaRepository;
    private final PalveluRepository palveluRepository;
    private final OrganisaatioViiteRepository organisaatioViiteRepository;
    private final LdapSynchronizationService ldapSynchronizationService;
    private final PermissionCheckerService permissionCheckerService;
    private final OppijanumerorekisteriClient oppijanumerorekisteriClient;
    private final CommonProperties commonProperties;
    private final HenkiloDataRepository henkiloDataRepository;
    private final KayttoOikeusRyhmaTapahtumaHistoriaDataRepository kayttoOikeusRyhmaTapahtumaHistoriaDataRepository;

    @Override
    public KayttoOikeusDto findKayttoOikeusById(long kayttoOikeusId) {
        return mapper.map(kayttoOikeusRepository.findById(kayttoOikeusId).orElseThrow(()
                -> new NotFoundException("kayttöoikeus not found")), KayttoOikeusDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KayttoOikeusRyhmaDto> listAllKayttoOikeusRyhmas() {
        return localizationService.localize(findAllKayttoOikeusRyhmasAsDtos());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PalveluKayttoOikeusDto> listKayttoOikeusByPalvelu(String palveluName) {
        return localizationService.localize(kayttoOikeusRepository.listKayttoOikeusByPalvelu(palveluName));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KayttoOikeusHistoriaDto> listMyonnettyKayttoOikeusHistoriaForCurrentUser() {
        return localizationService.localize(kayttoOikeusRepository.listMyonnettyKayttoOikeusHistoriaForHenkilo(getCurrentUserOid()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KayttooikeusPerustiedotDto> listMyonnettyKayttoOikeusForUser(KayttooikeusCriteria criteria, Long limit, Long offset) {
        return this.myonnettyKayttoOikeusRyhmaTapahtumaRepository.listCurrentKayttooikeusForHenkilo(criteria, limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpiringKayttoOikeusDto> findToBeExpiringMyonnettyKayttoOikeus(LocalDate at, Period... expirationPeriods) {
        return localizationService.localize(kayttoOikeusRepository.findSoonToBeExpiredTapahtumas(at, expirationPeriods));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<Integer>> findKayttooikeusryhmatAndOrganisaatioByHenkiloOid(String henkiloOid)  {
        List<Tuple> results = this.kayttoOikeusRyhmaRepository.findOrganisaatioOidAndRyhmaIdByHenkiloOid(henkiloOid);

        HashMap<String, List<Integer>> kayttooikeusRyhmasByOrganisation = new HashMap<>();
        QOrganisaatioHenkilo organisaatioHenkilo = QOrganisaatioHenkilo.organisaatioHenkilo;
        QMyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma
                = QMyonnettyKayttoOikeusRyhmaTapahtuma.myonnettyKayttoOikeusRyhmaTapahtuma;
        for(Tuple result : results) {
            String organisaatioOid = result.get(organisaatioHenkilo.organisaatioOid);
            Integer ryhmaId = Optional.ofNullable(result.get(myonnettyKayttoOikeusRyhmaTapahtuma.kayttoOikeusRyhma.id))
                    .orElseThrow(() -> new NullPointerException("null_ryhma_id")).intValue();

            List<Integer> ryhmasInOrganisaatio = kayttooikeusRyhmasByOrganisation
                    .computeIfAbsent(organisaatioOid, absentOrganisaatioOid -> new ArrayList<>());
            ryhmasInOrganisaatio.add(ryhmaId);
        }
        return kayttooikeusRyhmasByOrganisation;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KayttoOikeusRyhmaDto> listPossibleRyhmasByOrganization(String organisaatioOid) {
        return localizationService.localize(getRyhmasWithoutOrganizationLimitations(
                organisaatioOid, findAllKayttoOikeusRyhmasAsDtos()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyonnettyKayttoOikeusDto> listMyonnettyKayttoOikeusRyhmasMergedWithHenkilos(String henkiloOid, String organisaatioOid, String myontajaOid) {
        List<KayttoOikeusRyhmaDto> allRyhmas = getGrantableRyhmasWithoutOrgLimitations(organisaatioOid, myontajaOid);
        if (allRyhmas.isEmpty()){
            return Collections.emptyList();
        }

        List<MyonnettyKayttoOikeusDto> kayttoOikeusForHenkilo = myonnettyKayttoOikeusRyhmaTapahtumaRepository.findByHenkiloInOrganisaatio(henkiloOid, organisaatioOid);
        List<MyonnettyKayttoOikeusDto> all = allRyhmas.stream()
                .map(kayttoOikeusRyhmaDto -> {
            MyonnettyKayttoOikeusDto dto = new MyonnettyKayttoOikeusDto();
            dto.setRyhmaId(kayttoOikeusRyhmaDto.getId());
            dto.setRyhmaTunniste(kayttoOikeusRyhmaDto.getTunniste());
            if (kayttoOikeusRyhmaDto.getNimi() != null) {
                dto.setRyhmaNamesId(kayttoOikeusRyhmaDto.getNimi().getId());
            }
            if (kayttoOikeusRyhmaDto.getKuvaus() != null) {
                dto.setRyhmaKuvausId(kayttoOikeusRyhmaDto.getKuvaus().getId());
            }
            dto.setSelected(false);
            kayttoOikeusForHenkilo.stream()
                    .filter(myonnettyKayttoOikeusDto -> myonnettyKayttoOikeusDto.getRyhmaId().equals(dto.getRyhmaId()))
                    .findFirst().ifPresent(myonnettyKayttoOikeusDto -> {
                        dto.setMyonnettyTapahtumaId(myonnettyKayttoOikeusDto.getMyonnettyTapahtumaId());
                        dto.setAlkuPvm(myonnettyKayttoOikeusDto.getAlkuPvm());
                        dto.setVoimassaPvm(myonnettyKayttoOikeusDto.getVoimassaPvm());
                        dto.setSelected(true);
                    });
            return dto;
        }).collect(Collectors.toList());

        return localizationService.localize(all);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyonnettyKayttoOikeusDto> listMyonnettyKayttoOikeusRyhmasByHenkiloAndOrganisaatio(String henkiloOid, String organisaatioOid) {

        List<MyonnettyKayttoOikeusDto> all = this.myonnettyKayttoOikeusRyhmaTapahtumaRepository.findByHenkiloInOrganisaatio(henkiloOid, organisaatioOid);
        /* History data must be fetched also since it's additional information for admin users
         * if they need to solve possible conflicts with users' access rights
         */
        List<MyonnettyKayttoOikeusDto> ryhmaTapahtumaHistorias = this.kayttoOikeusRyhmaTapahtumaHistoriaRepository
                .findByHenkiloInOrganisaatio(henkiloOid, organisaatioOid);
        all.addAll(ryhmaTapahtumaHistorias);
        return this.mapKasittelijaNimiToMyonnettyKayttooikeusDto(this.localizationService.localize(all));
    }

    private List<MyonnettyKayttoOikeusDto> mapKasittelijaNimiToMyonnettyKayttooikeusDto(
            List<MyonnettyKayttoOikeusDto> myonnettyKayttoOikeusDtoList) {
        Set<String> kasittelijaOids = myonnettyKayttoOikeusDtoList.stream()
                .map(MyonnettyKayttoOikeusDto::getKasittelijaOid)
                .collect(Collectors.toSet());
        Map<String, String> kasittelijasMap = this.oppijanumerorekisteriClient.getHenkilonPerustiedot(kasittelijaOids).stream()
                .collect(Collectors.toMap(HenkiloPerustietoDto::getOidHenkilo,
                        henkiloPerustietoDto -> henkiloPerustietoDto.getKutsumanimi() + " "
                                + henkiloPerustietoDto.getSukunimi()));
        myonnettyKayttoOikeusDtoList.forEach(myonnettyKayttoOikeusDto ->
                myonnettyKayttoOikeusDto.setKasittelijaNimi(kasittelijasMap
                        .getOrDefault(myonnettyKayttoOikeusDto.getKasittelijaOid(), myonnettyKayttoOikeusDto.getKasittelijaOid())));
        return myonnettyKayttoOikeusDtoList;
    }

    @Override
    @Transactional(readOnly = true)
    public KayttoOikeusRyhmaDto findKayttoOikeusRyhma(long id) {
        KayttoOikeusRyhma kayttoOikeusRyhma = kayttoOikeusRyhmaRepository.findByRyhmaId(id).orElseThrow(()
                -> new NotFoundException("kayttooikeusryhma not found"));
        KayttoOikeusRyhmaDto ryhma = mapper.map(kayttoOikeusRyhma, KayttoOikeusRyhmaDto.class);
        return localizationService.localize(ryhma);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KayttoOikeusRyhmaDto> findSubRyhmasByMasterRyhma(long id) {
        List<Long> slaveIds = kayttoOikeusRyhmaMyontoViiteRepository.getSlaveIdsByMasterIds(Collections.singletonList(id));
        return localizationService.localize(kayttoOikeusRyhmaRepository.findByIdList(slaveIds));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PalveluRooliDto> findPalveluRoolisByKayttoOikeusRyhma(long ryhmaId) {
        return localizationService.localize(kayttoOikeusRepository.findPalveluRoolitByKayttoOikeusRyhmaId(ryhmaId));
    }

    @Override
    @Transactional(readOnly = true)
    public RyhmanHenkilotDto findHenkilotByKayttoOikeusRyhma(long id) {
        RyhmanHenkilotDto resp = new RyhmanHenkilotDto();
        resp.setPersonOids(kayttoOikeusRepository.findHenkilosByRyhma(id));
        return resp;
    }

    @Override
    @Transactional
    public long createKayttoOikeusRyhma(KayttoOikeusRyhmaModifyDto uusiRyhma) {
        if (kayttoOikeusRyhmaRepository.ryhmaNameFiExists(uusiRyhma.getNimi().get(FI))) {
            throw new IllegalArgumentException("Group name already in use");
        }

        KayttoOikeusRyhma kayttoOikeusRyhma = new KayttoOikeusRyhma();
        kayttoOikeusRyhma.setTunniste(uusiRyhma.getNimi().get(FI) + "_" + System.currentTimeMillis());
        TextGroup tg = createRyhmaDescription(uusiRyhma.getNimi());
        kayttoOikeusRyhma.setNimi(tg);
        if (uusiRyhma.getKuvaus() != null) {
            kayttoOikeusRyhma.setKuvaus(createRyhmaDescription(uusiRyhma.getKuvaus()));
        }
        kayttoOikeusRyhma.setRooliRajoite(uusiRyhma.getRooliRajoite());

        kayttoOikeusRyhma.getKayttoOikeus().addAll(uusiRyhma.getPalvelutRoolit().stream()
                .map(palveluRoooliDto ->
                        ofNullable(kayttoOikeusRepository.findByRooliAndPalvelu(palveluRoooliDto.getRooli(),
                                palveluRoooliDto.getPalveluName()))
                    .orElseGet(() -> kayttoOikeusRepository.persist(new KayttoOikeus(palveluRoooliDto.getRooli(),
                            palveluRepository.findByName(palveluRoooliDto.getPalveluName())
                                    .orElseThrow(() -> new NotFoundException("palvelu not found"))
                    )))).collect(toList()));

        kayttoOikeusRyhma = kayttoOikeusRyhmaRepository.persist(kayttoOikeusRyhma);

        // Organization limitation must be set only if the Organizatio OIDs are defined
        if (!isEmpty(uusiRyhma.getOrganisaatioTyypit())) {
            for (String orgTyyppi : uusiRyhma.getOrganisaatioTyypit()) {
                OrganisaatioViite ov = new OrganisaatioViite();
                ov.setOrganisaatioTyyppi(orgTyyppi);
                kayttoOikeusRyhma.addOrganisaatioViite(ov);
                organisaatioViiteRepository.persist(ov);
            }
        }

        // Group limitations must be set only if slave IDs have been defined
        if (!isEmpty(uusiRyhma.getSlaveIds())) {
            checkAndInsertSlaveGroups(uusiRyhma, kayttoOikeusRyhma);
        }

        return kayttoOikeusRyhma.getId();
    }

    @Override
    @Transactional
    public long createKayttoOikeus(KayttoOikeusCreateDto kayttoOikeus) {
        KayttoOikeus ko = mapper.map(kayttoOikeus, KayttoOikeus.class);
        ko.setPalvelu(palveluRepository.findByName(kayttoOikeus.getPalveluName()).orElseThrow(()
                -> new NotFoundException("palvelu not found")));
        return kayttoOikeusRepository.persist(ko).getId();
    }

    @Override
    @Transactional
    public void updateKayttoOikeusForKayttoOikeusRyhma(long id, KayttoOikeusRyhmaModifyDto ryhmaData) {
        KayttoOikeusRyhma kayttoOikeusRyhma = kayttoOikeusRyhmaRepository.findById(id).orElseThrow(()
                -> new NotFoundException("kayttooikeusryhma not found"));

        // UI must always send the list of group names even if they don't change!!
        setRyhmaDescription(ryhmaData, kayttoOikeusRyhma);
        if (ryhmaData.getKuvaus() != null) {
            setRyhmaKuvaus(ryhmaData, kayttoOikeusRyhma);
        }

        kayttoOikeusRyhma.setRyhmaRestriction(ryhmaData.isRyhmaRestriction());

        for (KayttoOikeusRyhmaMyontoViite viite : kayttoOikeusRyhmaMyontoViiteRepository.getMyontoViites(kayttoOikeusRyhma.getId())) {
            kayttoOikeusRyhmaMyontoViiteRepository.remove(viite);
        }

        if (!isEmpty(ryhmaData.getSlaveIds())) {
            checkAndInsertSlaveGroups(ryhmaData, kayttoOikeusRyhma);
        }

        kayttoOikeusRyhma.setRooliRajoite(ryhmaData.getRooliRajoite());
        // UI must always send the list of organization restrictions even if they don't change!!
        setRyhmaOrganisaatioViites(ryhmaData, kayttoOikeusRyhma);

        setKayttoOikeusRyhmas(ryhmaData, kayttoOikeusRyhma);

        ldapSynchronizationService.updateKayttoOikeusRyhma(id);
    }

    @Override
    @Transactional
    public void passivoiKayttooikeusryhma(long id) {
        LOGGER.info("Aloitetaan käyttöoikeusryhmän " + id + " passivointi.");
        KayttoOikeusRyhma kayttoOikeusRyhma = kayttoOikeusRyhmaRepository.findById(id).orElseThrow(()
                -> new NotFoundException("kayttooikeusryhma not found"));
        String currentUser = permissionCheckerService.getCurrentUserOid();
        Henkilo kasittelija = henkiloDataRepository.findByOidHenkilo(currentUser)
                .orElseThrow(() -> new DataInconsistencyException("Henkilöä ei löydy käyttäjän OID:lla " + currentUser));
        kayttoOikeusRyhma.setPassivoitu(true);
        List<MyonnettyKayttoOikeusRyhmaTapahtuma> kayttooikeudet = myonnettyKayttoOikeusRyhmaTapahtumaRepository.findByKayttoOikeusRyhmaId(id);

        for (MyonnettyKayttoOikeusRyhmaTapahtuma kayttoOikeus : kayttooikeudet) {
            String henkiloOid = kayttoOikeus.getOrganisaatioHenkilo().getHenkilo().getOidHenkilo();

            KayttoOikeusRyhmaTapahtumaHistoria historia = kayttoOikeus.toHistoria(
                    kasittelija, KayttoOikeudenTila.SULJETTU,
                    LocalDateTime.now(), "Oikeudet poistetaan käyttöoikeusryhmän passivoinnin myötä");
            kayttoOikeusRyhmaTapahtumaHistoriaDataRepository.save(historia);
            myonnettyKayttoOikeusRyhmaTapahtumaRepository.delete(kayttoOikeus);
            ldapSynchronizationService.updateHenkilo(henkiloOid);
        }
        LOGGER.info("Käyttöoikeusryhmän passivoinnin myötä poistettiin {} käyttöoikeutta", kayttooikeudet.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<KayttoOikeusRyhmaDto> findKayttoOikeusRyhmasByKayttoOikeusList(Map<String, String> kayttoOikeusList) {
        List<Long> kayttoOikeusIds = kayttoOikeusList.keySet().stream()
                .map(key -> kayttoOikeusRepository.findByRooliAndPalvelu(kayttoOikeusList.get(key), key))
                .filter(Objects::nonNull).map(IdentifiableAndVersionedEntity::getId).collect(Collectors.toList());

        List<KayttoOikeusRyhmaDto> ryhmas = addOrganisaatioViitteesToRyhmas(kayttoOikeusRyhmaRepository
                .findKayttoOikeusRyhmasByKayttoOikeusIds(kayttoOikeusIds));
        return localizationService.localize(ryhmas);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthorizationDataDto findAuthorizationDataByOid(String oid) {
        AccessRightListTypeDto accessRights = new AccessRightListTypeDto(myonnettyKayttoOikeusRyhmaTapahtumaRepository
                .findValidAccessRightsByOid(oid));
        GroupListTypeDto groups = new GroupListTypeDto(myonnettyKayttoOikeusRyhmaTapahtumaRepository
                .findValidGroupsByHenkilo(oid));
        return new AuthorizationDataDto(accessRights, groups);
    }

    private void setKayttoOikeusRyhmas(KayttoOikeusRyhmaModifyDto ryhmaData, KayttoOikeusRyhma kayttoOikeusRyhma) {
        Set<KayttoOikeus> givenKOs = new HashSet<>();
        for (PalveluRooliModifyDto prDto : ryhmaData.getPalvelutRoolit()) {
            Palvelu palvelu = palveluRepository.findByName(prDto.getPalveluName()).orElseThrow(()
                    -> new NotFoundException("palvelu not found"));
            KayttoOikeus tempKo = kayttoOikeusRepository.findByRooliAndPalvelu(prDto.getRooli(), palvelu.getName());
            givenKOs.add(tempKo);
        }

        // removed KayttoOikeus objects that haven't been passed in the request
        kayttoOikeusRyhma.getKayttoOikeus().stream()
                .filter(kayttoOikeus -> !givenKOs.contains(kayttoOikeus))
                .collect(toList())
                .forEach(kayttoOikeusRyhma.getKayttoOikeus()::remove);
        kayttoOikeusRyhma.getKayttoOikeus().addAll(givenKOs);
    }

    private void setRyhmaOrganisaatioViites(KayttoOikeusRyhmaModifyDto ryhmaData, KayttoOikeusRyhma kayttoOikeusRyhma) {
        if (!isEmpty(kayttoOikeusRyhma.getOrganisaatioViite())) {
            for (OrganisaatioViite orgV : kayttoOikeusRyhma.getOrganisaatioViite()) {
                organisaatioViiteRepository.remove(orgV);
            }
            kayttoOikeusRyhma.removeAllOrganisaatioViites();
        }

        ofNullable(ryhmaData.getOrganisaatioTyypit())
                .orElseGet(Collections::emptyList)
                .forEach(orgTyyppi -> {
                    OrganisaatioViite ov = new OrganisaatioViite();
                    ov.setOrganisaatioTyyppi(orgTyyppi);
                    kayttoOikeusRyhma.addOrganisaatioViite(ov);
                });
    }

    private void setRyhmaDescription(KayttoOikeusRyhmaModifyDto ryhmaData, KayttoOikeusRyhma kayttoOikeusRyhma) {
        if (kayttoOikeusRyhma.getNimi() == null) {
            kayttoOikeusRyhma.setNimi(createRyhmaDescription(ryhmaData.getNimi()));
        } else {
            TextGroup description = kayttoOikeusRyhma.getNimi();
            Set<Text> ryhmaNames = description.getTexts();

            updateOrAddTextForLang(ryhmaNames, FI, ryhmaData.getNimi().get(FI));
            updateOrAddTextForLang(ryhmaNames, SV, ryhmaData.getNimi().get(SV));
            updateOrAddTextForLang(ryhmaNames, EN, ryhmaData.getNimi().get(EN));
        }
    }

    private void setRyhmaKuvaus(KayttoOikeusRyhmaModifyDto ryhmaData, KayttoOikeusRyhma kayttoOikeusRyhma) {
        if (kayttoOikeusRyhma.getKuvaus()== null) {
            kayttoOikeusRyhma.setKuvaus(createRyhmaDescription(ryhmaData.getKuvaus()));
        } else {
            TextGroup kuvaus = kayttoOikeusRyhma.getKuvaus();
            Set<Text> ryhmaKuvaukset = kuvaus.getTexts();

            updateOrAddTextForLang(ryhmaKuvaukset, FI, ryhmaData.getKuvaus().get(FI));
            updateOrAddTextForLang(ryhmaKuvaukset, SV, ryhmaData.getKuvaus().get(SV));
            updateOrAddTextForLang(ryhmaKuvaukset, EN, ryhmaData.getKuvaus().get(EN));
        }
    }

    private List<KayttoOikeusRyhmaDto> addOrganisaatioViitteesToRyhmas(List<KayttoOikeusRyhmaDto> ryhmas){
        Map<Long, KayttoOikeusRyhmaDto> byIds = ryhmas.stream()
                .collect(Collectors.toMap(KayttoOikeusRyhmaDto::getId, Function.identity()));

        organisaatioViiteRepository.findByKayttoOikeusRyhmaIds(byIds.keySet())
                .forEach(fetched -> byIds.get(fetched.getKayttoOikeusRyhmaId())
                        .getOrganisaatioViite().add(fetched));

        return new ArrayList<>(byIds.values());
    }

    private List<KayttoOikeusRyhmaDto> findAllKayttoOikeusRyhmasAsDtos() {
        return addOrganisaatioViitteesToRyhmas(kayttoOikeusRyhmaRepository.listAll());
    }

    private List<KayttoOikeusRyhmaDto> getGrantableRyhmasWithoutOrgLimitations(String organisaatioOid, String myontajaOid) {
        // Get all master ids for current user and use them to get a list of all slave ids
        List<Long> slaveIds =  kayttoOikeusRyhmaMyontoViiteRepository.getSlaveIdsByMasterIds(
                myonnettyKayttoOikeusRyhmaTapahtumaRepository.findMasterIdsByHenkilo(myontajaOid));


        List<KayttoOikeusRyhmaDto> allRyhmas = (isEmpty(slaveIds) || this.permissionCheckerService.isCurrentUserAdmin()) ?
                kayttoOikeusRyhmaRepository.listAll() : kayttoOikeusRyhmaRepository.findByIdList(slaveIds);
        return getRyhmasWithoutOrganizationLimitations(organisaatioOid,
                addOrganisaatioViitteesToRyhmas(allRyhmas));
    }

    private void checkAndInsertSlaveGroups(KayttoOikeusRyhmaModifyDto ryhmaData, KayttoOikeusRyhma koRyhma) {
        if (isEmpty(ryhmaData.getSlaveIds())) {
            return;
        }
        if (kayttoOikeusRyhmaMyontoViiteRepository.isCyclicMyontoViite(koRyhma.getId(), ryhmaData.getSlaveIds())) {
            throw new InvalidKayttoOikeusException("Cyclic master-slave dependency");
        }

        for (Long slaveId : ryhmaData.getSlaveIds()) {
            KayttoOikeusRyhmaMyontoViite myontoViite = new KayttoOikeusRyhmaMyontoViite();
            myontoViite.setMasterId(koRyhma.getId());
            myontoViite.setSlaveId(slaveId);
            kayttoOikeusRyhmaMyontoViiteRepository.persist(myontoViite);
        }
    }

    private void updateOrAddTextForLang(Set<Text> ryhmaNames, String lang, String newText) {
        Optional<Text> name = ryhmaNames.stream().filter(text -> text.getLang().equals(lang)).findFirst();
        if (name.isPresent()) {
            name.get().setText(newText);
        } else {
            ryhmaNames.add(new Text(null, lang, newText));
        }
    }

    private TextGroup createRyhmaDescription(TextGroupDto name) {
        TextGroup tg = new TextGroup();
        tg.addText(new Text(tg, FI, name.get(FI)));
        tg.addText(new Text(tg, SV, name.get(SV)));
        tg.addText(new Text(tg, EN, name.get(EN)));
        return tg;
    }

    private List<KayttoOikeusRyhmaDto> getRyhmasWithoutOrganizationLimitations(String organisaatioOid, List<KayttoOikeusRyhmaDto> allRyhmas) {
        boolean isOphOrganisation = organisaatioOid.equals(commonProperties.getRootOrganizationOid());
        List<OrganisaatioPerustieto> hakuTulos = organisaatioClient.listWithParentsAndChildren(organisaatioOid,
                new OrganisaatioMyontoPredicate());
        return allRyhmas.stream()
                .filter(kayttoOikeusRyhma -> {

                    if (organisaatioOid.startsWith(this.commonProperties.getGroupOrganizationId()) && kayttoOikeusRyhma.isRyhmaRestriction())  {
                        return true;
                    }

                    if (isEmpty(kayttoOikeusRyhma.getOrganisaatioViite()) && !isOphOrganisation) {
                        return false;
                    }

                    boolean noOrgLimits = !isEmpty(kayttoOikeusRyhma.getOrganisaatioViite())
                            && !isOphOrganisation && !checkOrganizationLimitations(organisaatioOid, hakuTulos, kayttoOikeusRyhma);
                    return !noOrgLimits;
                }).collect(toList());
    }

    private boolean checkOrganizationLimitations(String organisaatioOid, List<OrganisaatioPerustieto> hakuTulos, KayttoOikeusRyhmaDto kayttoOikeusRyhma) {
        Set<String> tyyppis = kayttoOikeusRyhma.getOrganisaatioViite().stream().map(OrganisaatioViiteDto::getOrganisaatioTyyppi).collect(toSet());
        return permissionCheckerService.organisaatioLimitationCheck(organisaatioOid, hakuTulos, tyyppis);
    }

}
