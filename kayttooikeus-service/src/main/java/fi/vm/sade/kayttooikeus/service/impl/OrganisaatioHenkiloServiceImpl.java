package fi.vm.sade.kayttooikeus.service.impl;

import com.google.common.collect.Sets;
import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloWithOrganisaatioDto.OrganisaatioDto;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.*;
import fi.vm.sade.kayttooikeus.repositories.criteria.AnomusCriteria;
import fi.vm.sade.kayttooikeus.repositories.criteria.OrganisaatioHenkiloCriteria;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService;
import fi.vm.sade.kayttooikeus.service.OrganisaatioHenkiloService;
import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import fi.vm.sade.kayttooikeus.util.OrganisaatioMyontoPredicate;
import fi.vm.sade.kayttooikeus.util.UserDetailsUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ValidationException;
import java.time.LocalDateTime;
import java.util.*;

import static fi.vm.sade.kayttooikeus.dto.KayttajaTyyppi.PALVELU;
import static fi.vm.sade.kayttooikeus.dto.KayttajaTyyppi.VIRKAILIJA;
import static fi.vm.sade.kayttooikeus.dto.Localizable.comparingPrimarlyBy;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Service
@RequiredArgsConstructor
public class OrganisaatioHenkiloServiceImpl extends AbstractService implements OrganisaatioHenkiloService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrganisaatioHenkiloServiceImpl.class);

    private final String FALLBACK_LANGUAGE = "fi";

    private final OrganisaatioHenkiloRepository organisaatioHenkiloRepository;
    private final KayttoOikeusRepository kayttoOikeusRepository;
    private final HenkiloDataRepository henkiloDataRepository;
    private final MyonnettyKayttoOikeusRyhmaTapahtumaRepository myonnettyKayttoOikeusRyhmaTapahtumaRepository;
    private final KayttoOikeusRyhmaTapahtumaHistoriaDataRepository kayttoOikeusRyhmaTapahtumaHistoriaDataRepository;
    private final HaettuKayttooikeusRyhmaRepository haettuKayttooikeusRyhmaRepository;

    private final LdapSynchronizationService ldapSynchronizationService;
    private final PermissionCheckerService permissionCheckerService;

    private final OrikaBeanMapper mapper;

    private final OrganisaatioClient organisaatioClient;

    private final CommonProperties commonProperties;

    @Override
    public List<OrganisaatioHenkiloWithOrganisaatioDto> listOrganisaatioHenkilos(String henkiloOid, String compareByLang) {
        return listOrganisaatioHenkilos(henkiloOid, compareByLang, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganisaatioHenkiloWithOrganisaatioDto> listOrganisaatioHenkilos(String henkiloOid, String compareByLang, PalveluRooliGroup requiredRoles) {
        return organisaatioHenkiloRepository.findActiveOrganisaatioHenkiloListDtos(henkiloOid, requiredRoles)
                .stream()
                .peek(organisaatioHenkilo ->
                    organisaatioHenkilo.setOrganisaatio(
                        mapOrganisaatioDtoRecursive(
                                this.organisaatioClient
                                        .getOrganisaatioPerustiedotCached(organisaatioHenkilo.getOrganisaatio().getOid())
                                        .orElseGet(() -> {
                                            String organisaatioOid = organisaatioHenkilo.getOrganisaatio().getOid();
                                            LOGGER.warn("Henkilön {} organisaatiota {} ei löytynyt", henkiloOid, organisaatioOid);
                                            return UserDetailsUtil.createUnknownOrganisation(organisaatioOid);
                                        }),
                                compareByLang))
                ).sorted(Comparator.comparing(dto -> dto.getOrganisaatio().getNimi(),
                        comparingPrimarlyBy(ofNullable(compareByLang).orElse(FALLBACK_LANGUAGE)))).collect(toList());
    }

    private OrganisaatioDto mapOrganisaatioDtoRecursive(OrganisaatioPerustieto perustiedot, String compareByLang) {
        OrganisaatioDto dto = new OrganisaatioDto();
        dto.setOid(perustiedot.getOid());
        dto.setNimi(new TextGroupMapDto(null, perustiedot.getNimi()));
        dto.setParentOidPath(perustiedot.getParentOidPath());
        dto.setTyypit(perustiedot.getTyypit());
        dto.setStatus(perustiedot.getStatus());
        dto.setChildren(perustiedot.getChildren().stream()
               .filter(new OrganisaatioMyontoPredicate())
                .map(child -> mapOrganisaatioDtoRecursive(child, compareByLang))
                .sorted(Comparator.comparing(OrganisaatioDto::getNimi, comparingPrimarlyBy(ofNullable(compareByLang).orElse(FALLBACK_LANGUAGE))))
                .collect(toList()));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KayttajaTyyppi> listPossibleHenkiloTypesAccessibleForCurrentUser() {
        if (kayttoOikeusRepository.isHenkiloMyonnettyKayttoOikeusToPalveluInRole(getCurrentUserOid(),
                PALVELU_HENKILONHALLINTA, ROLE_ADMIN)) {
            return asList(VIRKAILIJA, PALVELU);
        }
        if (kayttoOikeusRepository.isHenkiloMyonnettyKayttoOikeusToPalveluInRole(getCurrentUserOid(),
                PALVELU_HENKILONHALLINTA, ROLE_CRUD)) {
            return singletonList(VIRKAILIJA);
        }
        return emptyList();
    }

    @Override
    public Collection<String> listOrganisaatioOidBy(OrganisaatioHenkiloCriteria criteria) {
        String kayttajaOid = permissionCheckerService.getCurrentUserOid();
        List<String> organisaatioOids = organisaatioHenkiloRepository.findUsersOrganisaatioHenkilosByPalveluRoolis(
                kayttajaOid, PalveluRooliGroup.KAYTTAJAHAKU);
        if (!organisaatioOids.contains(commonProperties.getRootOrganizationOid())) {
            criteria.setOrRetainOrganisaatioOids(organisaatioOids);
        }
        return organisaatioHenkiloRepository.findOrganisaatioOidBy(criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganisaatioHenkiloDto findOrganisaatioHenkiloByHenkiloAndOrganisaatio(String henkiloOid, String organisaatioOid) {
        return organisaatioHenkiloRepository.findByHenkiloOidAndOrganisaatioOid(henkiloOid, organisaatioOid)
                .orElseThrow(() -> new NotFoundException("Could not find organisaatiohenkilo"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganisaatioHenkiloDto> findOrganisaatioByHenkilo(String henkiloOid) {
        return organisaatioHenkiloRepository.findOrganisaatioHenkilosForHenkilo(henkiloOid);
    }

    @Override
    @Transactional
    public List<OrganisaatioHenkiloDto> addOrganisaatioHenkilot(String henkiloOid,
                                                                List<OrganisaatioHenkiloCreateDto> organisaatioHenkilot) {
        Henkilo henkilo = henkiloDataRepository.findByOidHenkilo(henkiloOid)
                .orElseThrow(() -> new NotFoundException("Henkilöä ei löytynyt OID:lla " + henkiloOid));
        return findOrCreateOrganisaatioHenkilos(organisaatioHenkilot, henkilo);
    }

    private List<OrganisaatioHenkiloDto> findOrCreateOrganisaatioHenkilos(List<OrganisaatioHenkiloCreateDto> organisaatioHenkilot,
                                                                          Henkilo henkilo) {
        organisaatioHenkilot.stream()
                .filter((OrganisaatioHenkiloCreateDto createDto) ->
                    henkilo.getOrganisaatioHenkilos().stream()
                            .noneMatch((OrganisaatioHenkilo u) -> u.getOrganisaatioOid().equals(createDto.getOrganisaatioOid()))
                )
                .forEach((OrganisaatioHenkiloCreateDto createDto) -> {
                    validateOrganisaatioOid(createDto.getOrganisaatioOid());
                    OrganisaatioHenkilo organisaatioHenkilo = mapper.map(createDto, OrganisaatioHenkilo.class);
                    organisaatioHenkilo.setHenkilo(henkilo);
                    henkilo.getOrganisaatioHenkilos().add(organisaatioHenkiloRepository.save(organisaatioHenkilo));
                });

        return mapper.mapAsList(henkilo.getOrganisaatioHenkilos(), OrganisaatioHenkiloDto.class);
    }

    @Override
    @Transactional
    public List<OrganisaatioHenkiloDto> createOrUpdateOrganisaatioHenkilos(String henkiloOid,
                                                                           List<OrganisaatioHenkiloUpdateDto> organisaatioHenkiloDtoList) {
        Henkilo henkilo = this.henkiloDataRepository.findByOidHenkilo(henkiloOid)
                .orElseThrow(() -> new NotFoundException("Henkilöä ei löytynyt OID:lla " + henkiloOid));
        this.findOrCreateOrganisaatioHenkilos(this.mapper.mapAsList(organisaatioHenkiloDtoList, OrganisaatioHenkiloCreateDto.class),
                henkilo);

        organisaatioHenkiloDtoList.stream()
                .filter((OrganisaatioHenkiloUpdateDto t) ->
                        henkilo.getOrganisaatioHenkilos().stream()
                                .anyMatch((OrganisaatioHenkilo u) -> u.getOrganisaatioOid().equals(t.getOrganisaatioOid()))
                )
                .forEach(organisaatioHenkiloUpdateDto -> {
                    if (!this.getCurrentUserOid().equals(henkiloOid)) {
                        Map<String, List<String>> allowedRoles = new LinkedHashMap<>();
                        allowedRoles.put(PALVELU_ANOMUSTENHALLINTA, asList("CRUD", "READ_UPDATE"));
                        allowedRoles.put(PALVELU_KAYTTOOIKEUS, asList("CRUD"));
                        this.permissionCheckerService.hasRoleForOrganisations(Collections.singletonList(organisaatioHenkiloUpdateDto),
                                allowedRoles);
                    }
                    // Make sure organisation exists.
                    validateOrganisaatioOid(organisaatioHenkiloUpdateDto.getOrganisaatioOid());
                    OrganisaatioHenkilo savedOrgHenkilo = this.findFirstMatching(organisaatioHenkiloUpdateDto,
                            henkilo.getOrganisaatioHenkilos());
                    // Do not allow updating organisation oid (should never happen since organisaatiohenkilo is found by this value)
                    if (!savedOrgHenkilo.getOrganisaatioOid().equals(organisaatioHenkiloUpdateDto.getOrganisaatioOid())) {
                        throw new InternalError("Trying to update organisaatio henkilo organisation oid");
                    }
                    this.mapper.map(organisaatioHenkiloUpdateDto, savedOrgHenkilo);
                });
        return mapper.mapAsList(henkilo.getOrganisaatioHenkilos(), OrganisaatioHenkiloDto.class);
    }

    @Transactional
    @Override
    public void passivoiHenkiloOrganisation(String oidHenkilo, String henkiloOrganisationOid) {
        Henkilo kasittelija = this.henkiloDataRepository.findByOidHenkilo(UserDetailsUtil.getCurrentUserOid())
                .orElseThrow(() -> new NotFoundException("Could not find current henkilo with oid " + UserDetailsUtil.getCurrentUserOid()));
        OrganisaatioHenkilo organisaatioHenkilo = this.organisaatioHenkiloRepository
                .findByHenkiloOidHenkiloAndOrganisaatioOid(oidHenkilo, henkiloOrganisationOid)
                .orElseThrow(() -> new NotFoundException("Unknown organisation" + henkiloOrganisationOid + "for henkilo" + oidHenkilo));
        this.passivoiOrganisaatioHenkiloJaPoistaKayttooikeudet(organisaatioHenkilo, kasittelija, "Henkilön passivointi");
    }

    private OrganisaatioHenkilo findFirstMatching(OrganisaatioHenkiloUpdateDto organisaatioHenkilo,
                                                           Set<OrganisaatioHenkilo> organisaatioHenkiloCreateDtoList) {
        return organisaatioHenkiloCreateDtoList.stream().filter((OrganisaatioHenkilo t) ->
                organisaatioHenkilo.getOrganisaatioOid().equals(t.getOrganisaatioOid())
        ).findFirst().orElseThrow(() -> new NotFoundException("Could not update organisaatiohenkilo with oid "
                + organisaatioHenkilo.getOrganisaatioOid()));
    }

    // passivoi organisaatiohenkilön ja poistaa käyttöoikeudet
    private void passivoiOrganisaatioHenkiloJaPoistaKayttooikeudet(OrganisaatioHenkilo organisaatioHenkilo, Henkilo kasittelija, String selite) {
        organisaatioHenkilo.setPassivoitu(true);
        Set<KayttoOikeusRyhmaTapahtumaHistoria> historia = organisaatioHenkilo.getMyonnettyKayttoOikeusRyhmas().stream()
                .map(myonnettyKayttoOikeusRyhmaTapahtuma -> myonnettyKayttoOikeusRyhmaTapahtuma
                        .toHistoria(kasittelija, KayttoOikeudenTila.SULJETTU, LocalDateTime.now(), selite))
                .collect(toSet());
        organisaatioHenkilo.setKayttoOikeusRyhmaHistorias(historia);
        this.kayttoOikeusRyhmaTapahtumaHistoriaDataRepository.saveAll(historia);
        this.myonnettyKayttoOikeusRyhmaTapahtumaRepository.deleteAll(organisaatioHenkilo.getMyonnettyKayttoOikeusRyhmas());
        organisaatioHenkilo.setMyonnettyKayttoOikeusRyhmas(Sets.newHashSet());
        ldapSynchronizationService.updateHenkiloAsap(organisaatioHenkilo.getHenkilo().getOidHenkilo());

        this.henkiloDataRepository.findByOidHenkilo(organisaatioHenkilo.getHenkilo().getOidHenkilo())
                .ifPresent(this::disableNonValidVarmennettavas);
    }

    // HenkiloVarmentaja suhde on validi jos löytyy yhä yhteinen aktiivinen organisaatio
    private void disableNonValidVarmennettavas(Henkilo henkilo) {
        henkilo.getHenkiloVarmennettavas().forEach(henkiloVarmentaja -> {
            boolean isValid = henkiloVarmentaja.getVarmennettavaHenkilo().getOrganisaatioHenkilos().stream()
                    .filter(OrganisaatioHenkilo::isAktiivinen)
                    .map(OrganisaatioHenkilo::getOrganisaatioOid)
                    .anyMatch(organisaatioHenkilo -> henkilo.getOrganisaatioHenkilos().stream()
                            .filter(OrganisaatioHenkilo::isAktiivinen)
                            .map(OrganisaatioHenkilo::getOrganisaatioOid)
                            .anyMatch(organisaatioOid -> organisaatioOid.equals(organisaatioHenkilo)));
            henkiloVarmentaja.setTila(isValid);
        });
    }

    @Transactional
    @Override
    public void kasitteleOrganisaatioidenLakkautus(String kasittelijaOid) {
        LOGGER.info("Aloitetaan passivoitujen organisaatioiden organisaatiohenkilöiden passivointi sekä käyttöoikeuksien ja anomusten poisto");
        Henkilo kasittelija = this.henkiloDataRepository.findByOidHenkilo(kasittelijaOid)
                .orElseThrow(() -> new NotFoundException("Could not find henkilo with oid " + kasittelijaOid));
        Set<String> passiivisetOids = organisaatioClient.getLakkautetutOids();
        List<OrganisaatioHenkilo> aktiivisetOrganisaatioHenkilosInLakkautetutOrganisaatios = this.organisaatioHenkiloRepository.findByOrganisaatioOidIn(passiivisetOids)
                .stream().filter(oh -> oh.isAktiivinen()).collect(toList());
        LOGGER.info("Passivoidaan {} aktiivista organisaatiohenkilöä ja näiden voimassa olevat käyttöoikeudet.", aktiivisetOrganisaatioHenkilosInLakkautetutOrganisaatios.size());
        aktiivisetOrganisaatioHenkilosInLakkautetutOrganisaatios.forEach(organisaatioHenkilo -> this.passivoiOrganisaatioHenkiloJaPoistaKayttooikeudet(organisaatioHenkilo, kasittelija, "Passivoidun organisaation organisaatiohenkilön passivointi ja käyttöoikeuksien poisto"));

        AnomusCriteria anomusCriteria = AnomusCriteria.builder().organisaatioOids(passiivisetOids).onlyActive(true).build();
        this.poistaAnomuksetOrganisaatioista(anomusCriteria);
        LOGGER.info("Lopetetaan passivoitujen organisaatioiden organisaatiohenkilöiden passivointi sekä käyttöoikeuksien ja anomusten poisto");
    }

    private void poistaAnomuksetOrganisaatioista(AnomusCriteria criteria) {
        List<HaettuKayttoOikeusRyhma> haettuKayttoOikeusRyhmas = this.haettuKayttooikeusRyhmaRepository.findBy(criteria.createAnomusSearchCondition(this.organisaatioClient));

        logger.info("Poistetaan {} anomusta ja {} niihin liittyvää haettua käyttöoikeusryhmää",
                haettuKayttoOikeusRyhmas.stream().map(h -> h.getAnomus().getId()).distinct().count(), haettuKayttoOikeusRyhmas.size());
        haettuKayttoOikeusRyhmas.stream().forEach(h -> {
            Anomus anomus = h.getAnomus();
            if(h.getAnomus().getHaettuKayttoOikeusRyhmas().size() == 1) {
//                 Asetetaan anomus hylätyksi, jos ollaan poistamassa viimeistä siihen liitettyä haettua käyttöoikeusryhmä
                anomus.setAnomuksenTila(AnomuksenTila.HYLATTY);
                anomus.setHylkaamisperuste("Hylätään lakkautetun organisaation anomuksena");
            }
            anomus.getHaettuKayttoOikeusRyhmas().remove(h);
            anomus.setAnomusTilaTapahtumaPvm(LocalDateTime.now());
            this.haettuKayttooikeusRyhmaRepository.delete(h);
        });
    }

    private void validateOrganisaatioOid(String organisaatioOid) {
        organisaatioClient.getOrganisaatioPerustiedotCached(organisaatioOid)
                .filter(new OrganisaatioMyontoPredicate())
                .orElseThrow(() -> new ValidationException("Active organisation not found with oid " + organisaatioOid));
    }

}
