package fi.vm.sade.kayttooikeus.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloWithOrganisaatioDto.OrganisaatioDto;
import fi.vm.sade.kayttooikeus.model.KayttoOikeusRyhmaTapahtumaHistoria;
import fi.vm.sade.kayttooikeus.repositories.*;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService;
import fi.vm.sade.kayttooikeus.service.OrganisaatioHenkiloService;
import fi.vm.sade.kayttooikeus.service.OrganisaatioService;
import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import fi.vm.sade.kayttooikeus.util.UserDetailsUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.OrganisaatioHenkilo;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import static fi.vm.sade.kayttooikeus.dto.Localizable.comparingPrimarlyBy;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloTyyppi;

import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.PALVELU_HENKILONHALLINTA;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.ROLE_ADMIN;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.ROLE_CRUD;
import static fi.vm.sade.oppijanumerorekisteri.dto.HenkiloTyyppi.PALVELU;
import static fi.vm.sade.oppijanumerorekisteri.dto.HenkiloTyyppi.VIRKAILIJA;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Service
@RequiredArgsConstructor
public class OrganisaatioHenkiloServiceImpl extends AbstractService implements OrganisaatioHenkiloService {
    private final String FALLBACK_LANGUAGE = "fi";

    private final OrganisaatioHenkiloRepository organisaatioHenkiloRepository;
    private final KayttoOikeusRepository kayttoOikeusRepository;
    private final HenkiloDataRepository henkiloDataRepository;
    private final MyonnettyKayttoOikeusRyhmaTapahtumaRepository myonnettyKayttoOikeusRyhmaTapahtumaRepository;
    private final KayttoOikeusRyhmaTapahtumaHistoriaDataRepository kayttoOikeusRyhmaTapahtumaHistoriaDataRepository;

    private final LdapSynchronizationService ldapSynchronizationService;
    private final OrganisaatioService organisaatioService;
    private final PermissionCheckerService permissionCheckerService;

    private final OrikaBeanMapper mapper;

    private final OrganisaatioClient organisaatioClient;

    @Override
    @Transactional(readOnly = true)
    public List<OrganisaatioHenkiloWithOrganisaatioDto> listOrganisaatioHenkilos(String henkiloOid, String compareByLang) {
        return organisaatioHenkiloRepository.findActiveOrganisaatioHenkiloListDtos(henkiloOid)
                .stream().peek(organisaatioHenkilo ->
                    organisaatioHenkilo.setOrganisaatio(
                        mapOrganisaatioDtoRecursive(
                                this.organisaatioClient
                                        .getOrganisaatioPerustiedotCached(organisaatioHenkilo.getOrganisaatio().getOid())
                                        .orElseThrow(() -> new NotFoundException("Organisation not found with oid "
                                                + organisaatioHenkilo.getOrganisaatio().getOid())),
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
        dto.setChildren(perustiedot.getChildren().stream().map(child -> mapOrganisaatioDtoRecursive(child, compareByLang))
            .sorted(Comparator.comparing(OrganisaatioDto::getNimi, comparingPrimarlyBy(ofNullable(compareByLang).orElse(FALLBACK_LANGUAGE))))
            .collect(toList()));
        return dto;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<OrganisaatioPerustieto> listOrganisaatioPerustiedotForCurrentUser() {
        return organisaatioClient.listActiveOrganisaatioPerustiedotByOidRestrictionList(
                organisaatioHenkiloRepository.findDistinctOrganisaatiosForHenkiloOid(getCurrentUserOid()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HenkiloTyyppi> listPossibleHenkiloTypesAccessibleForCurrentUser() {
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
                    this.organisaatioService.throwIfActiveNotFound(createDto.getOrganisaatioOid());
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
                        this.permissionCheckerService.hasRoleForOrganisations(Collections.singletonList(organisaatioHenkiloUpdateDto),
                                Lists.newArrayList("CRUD", "READ_UPDATE"));
                    }
                    // Make sure organisation exists.
                    this.organisaatioService.throwIfActiveNotFound(organisaatioHenkiloUpdateDto.getOrganisaatioOid());
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
        organisaatioHenkilo.setPassivoitu(true);
        Set<KayttoOikeusRyhmaTapahtumaHistoria> historia = organisaatioHenkilo.getMyonnettyKayttoOikeusRyhmas().stream()
                .map(myonnettyKayttoOikeusRyhmaTapahtuma -> myonnettyKayttoOikeusRyhmaTapahtuma
                        .toHistoria(kasittelija, KayttoOikeudenTila.SULJETTU, LocalDateTime.now(), "Henkilön passivointi"))
                .collect(toSet());
        organisaatioHenkilo.setKayttoOikeusRyhmaHistorias(historia);
        this.kayttoOikeusRyhmaTapahtumaHistoriaDataRepository.save(historia);
        this.myonnettyKayttoOikeusRyhmaTapahtumaRepository.delete(organisaatioHenkilo.getMyonnettyKayttoOikeusRyhmas());
        organisaatioHenkilo.setMyonnettyKayttoOikeusRyhmas(Sets.newHashSet());
        ldapSynchronizationService.updateHenkiloAsap(oidHenkilo);
    }

    private OrganisaatioHenkilo findFirstMatching(OrganisaatioHenkiloUpdateDto organisaatioHenkilo,
                                                           Set<OrganisaatioHenkilo> organisaatioHenkiloCreateDtoList) {
        return organisaatioHenkiloCreateDtoList.stream().filter((OrganisaatioHenkilo t) ->
                organisaatioHenkilo.getOrganisaatioOid().equals(t.getOrganisaatioOid())
        ).findFirst().orElseThrow(() -> new NotFoundException("Could not update organisaatiohenkilo with oid "
                + organisaatioHenkilo.getOrganisaatioOid()));
    }

}
