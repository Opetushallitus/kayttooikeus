package fi.vm.sade.kayttooikeus.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fi.vm.sade.generic.rest.CachingRestClient;
import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.dto.AnomusDto;
import fi.vm.sade.kayttooikeus.dto.HaettuKayttooikeusryhmaDto;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloCreateDto;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloUpdateDto;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.ExternalPermissionService;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.PermissionCheckDto;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.PermissionCheckRequestDto;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.PermissionCheckResponseDto;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.*;
import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import fi.vm.sade.kayttooikeus.util.UserDetailsUtil;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloTyyppi;
import fi.vm.sade.organisaatio.api.model.types.OrganisaatioStatus;
import fi.vm.sade.properties.OphProperties;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.*;
import static java.util.Collections.singletonMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PermissionCheckerServiceImpl implements PermissionCheckerService {
    private static final Logger LOG = LoggerFactory.getLogger(PermissionCheckerService.class);
    private static CachingRestClient restClient = new CachingRestClient().setClientSubSystemCode("henkilo.authentication-service");
    private static ObjectMapper objectMapper = new ObjectMapper();
    public static final String ROLE_HENKILONHALLINTA_PREFIX = "ROLE_APP_HENKILONHALLINTA_";
    private static final String ROLE_ANOMUSTENHALLINTA_PREFIX = "ROLE_APP_ANOMUSTENHALLINTA_";
    public static final String PALVELU_HENKILONHALLINTA = "HENKILONHALLINTA";
    public static final String PALVELU_ANOMUSTENHALLINTA = "ANOMUSTENHALLINTA";
    public static final String ROLE_ADMIN = "OPHREKISTERI";
    public static final String ROLE_CRUD = "CRUD";
    public static final String ROLE_PREFIX = "ROLE_APP_";


    private final HenkiloDataRepository henkiloDataRepository;
    private final MyonnettyKayttoOikeusRyhmaTapahtumaRepository myonnettyKayttoOikeusRyhmaTapahtumaRepository;
    private final KayttoOikeusRyhmaMyontoViiteRepository kayttoOikeusRyhmaMyontoViiteRepository;
    private final OrganisaatioHenkiloRepository organisaatioHenkiloRepository;
    private final KayttooikeusryhmaDataRepository kayttooikeusryhmaDataRepository;

    private OppijanumerorekisteriClient oppijanumerorekisteriClient;
    private OrganisaatioClient organisaatioClient;

    private CommonProperties commonProperties;

    private static Map<ExternalPermissionService, String> SERVICE_URIS = new HashMap<>();

    @Autowired
    public PermissionCheckerServiceImpl(OphProperties ophProperties,
                                        HenkiloDataRepository henkiloDataRepository,
                                        OrganisaatioClient organisaatioClient,
                                        OppijanumerorekisteriClient oppijanumerorekisteriClient,
                                        MyonnettyKayttoOikeusRyhmaTapahtumaRepository myonnettyKayttoOikeusRyhmaTapahtumaRepository,
                                        KayttoOikeusRyhmaMyontoViiteRepository kayttoOikeusRyhmaMyontoViiteRepository,
                                        CommonProperties commonProperties,
                                        OrganisaatioHenkiloRepository organisaatioHenkiloRepository,
                                        KayttooikeusryhmaDataRepository kayttooikeusryhmaDataRepository) {
        SERVICE_URIS.put(ExternalPermissionService.HAKU_APP, ophProperties.url("haku-app.external-permission-check"));
        SERVICE_URIS.put(ExternalPermissionService.SURE, ophProperties.url("suoritusrekisteri.external-permission-check"));
        SERVICE_URIS.put(ExternalPermissionService.ATARU, ophProperties.url("ataru-editori.external-permission-check"));
        this.henkiloDataRepository = henkiloDataRepository;
        this.organisaatioClient = organisaatioClient;
        this.oppijanumerorekisteriClient = oppijanumerorekisteriClient;
        this.myonnettyKayttoOikeusRyhmaTapahtumaRepository = myonnettyKayttoOikeusRyhmaTapahtumaRepository;
        this.kayttoOikeusRyhmaMyontoViiteRepository = kayttoOikeusRyhmaMyontoViiteRepository;
        this.commonProperties = commonProperties;
        this.organisaatioHenkiloRepository = organisaatioHenkiloRepository;
        this.kayttooikeusryhmaDataRepository = kayttooikeusryhmaDataRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAllowedToAccessPerson(String personOid, List<String> allowedRoles, ExternalPermissionService permissionService) {
        return isAllowedToAccessPerson(getCurrentUserOid(), personOid, singletonMap(PALVELU_HENKILONHALLINTA, allowedRoles), permissionService, this.getCasRoles());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAllowedToAccessPerson(String personOid, Map<String, List<String>> allowedRoles, ExternalPermissionService permissionService) {
        return isAllowedToAccessPerson(getCurrentUserOid(), personOid, allowedRoles, permissionService, this.getCasRoles());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAllowedToAccessPersonOrSelf(String personOid, List<String> allowedRoles, ExternalPermissionService permissionService) {
        return isAllowedToAccessPersonOrSelf(personOid, singletonMap(PALVELU_HENKILONHALLINTA, allowedRoles), permissionService);
    }

    @Override
    public boolean isAllowedToAccessPersonOrSelf(String personOid, Map<String, List<String>> allowedRoles, ExternalPermissionService permissionService) {
        String currentUserOid = getCurrentUserOid();
        return personOid.equals(currentUserOid)
                || isAllowedToAccessPerson(currentUserOid, personOid, allowedRoles, permissionService, this.getCasRoles());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAllowedToAccessPerson(PermissionCheckDto permissionCheckDto) {
        handleAllowedRoles(permissionCheckDto);
        return isAllowedToAccessPerson(permissionCheckDto.getCallingUserOid(),
                permissionCheckDto.getUserOid(), permissionCheckDto.getAllowedPalveluRooli(),
                permissionCheckDto.getExternalPermissionService(), permissionCheckDto.getCallingUserRoles());
    }

    public static void handleAllowedRoles(PermissionCheckDto permissionCheckDto) {
        // muutetaan vanha "allowedRoles" uuteen "allowedPalveluRooli"-formaattiin
        if (permissionCheckDto.getAllowedRoles() != null) {
            if (permissionCheckDto.getAllowedPalveluRooli() == null) {
                permissionCheckDto.setAllowedPalveluRooli(new HashMap<>());
            }
            permissionCheckDto.getAllowedPalveluRooli().merge(PALVELU_HENKILONHALLINTA, permissionCheckDto.getAllowedRoles(), (vanhaArvo, uusiArvo) -> {
                vanhaArvo.addAll(uusiArvo);
                return vanhaArvo;
            });
            permissionCheckDto.setAllowedRoles(null);
        }
    }

    /*
     * Check internally and externally whether currentuser has any of the palvelu/rooli pair combination given in allowedPalveluRooli
     * that grants access to the given user (personOidToAccess)
     */
    private boolean isAllowedToAccessPerson(String callingUserOid, String personOidToAccess, Map<String, List<String>> allowedPalveluRooli,
                                          ExternalPermissionService permissionCheckService, Set<String> callingUserRoles) {
        if (this.hasInternalAccess(personOidToAccess, allowedPalveluRooli, callingUserRoles)) {
            return true;
        }

        // If no internal access -> try to check permission from external service
        String serviceUri = SERVICE_URIS.get(permissionCheckService);

        if (StringUtils.isBlank(personOidToAccess) || StringUtils.isBlank(serviceUri)) {
            LOG.error("isAllowedToAccess() called with empty personOid or invalid permissionCheckService");
            return false;
        }

        // Get orgs for logged in user
        if (callingUserOid == null) {
            LOG.error("isAllowedToAccess(): no logged in user found -> return no permission");
            return false;
        }

        List<OrganisaatioPerustieto> orgs = this.listOrganisaatiosByHenkiloOid(callingUserOid);
        Set<String> flattedOrgs = Sets.newHashSet();

        if (!orgs.isEmpty()) {
            for (OrganisaatioPerustieto org : orgs) {
                flattedOrgs.addAll(getOidsRecursive(org));
            }
        }

        if (flattedOrgs.isEmpty()) {
            LOG.error("No organisations found for logged in user with oid: " + callingUserOid);
            return false;
        }

        Set<String> personOidsForSamePerson = oppijanumerorekisteriClient.getAllOidsForSamePerson(personOidToAccess);

        PermissionCheckResponseDto response = checkPermissionFromExternalService(serviceUri, personOidsForSamePerson, flattedOrgs, callingUserRoles);


//        SURElla ei kaikissa tapauksissa (esim. jos YO tutkinto ennen 90-lukua) ole tietoa
//        henkilösta, joten pitää kysyä varmuuden vuoksi myös haku-appilta
        if (!response.isAccessAllowed() && ExternalPermissionService.SURE.equals(permissionCheckService)) {
            return checkPermissionFromExternalService(
                    SERVICE_URIS.get(ExternalPermissionService.HAKU_APP), personOidsForSamePerson, flattedOrgs, callingUserRoles
            ).isAccessAllowed();
        }

        if (!response.isAccessAllowed()) {
            LOG.error("Insufficient roles. permission check done from external service:"+ permissionCheckService + " Logged in user:" + callingUserOid + " accessed personId:" + personOidToAccess + " loginuser orgs:" + flattedOrgs.stream().collect(Collectors.joining(",")) + " palveluroles needed:" + getPrefixedRolesByPalveluRooli(allowedPalveluRooli).stream().collect(Collectors.joining(",")), " user cas roles:" + callingUserRoles.stream().collect(Collectors.joining(",")) + " personOidsForSamePerson:" + personOidsForSamePerson.stream().collect(Collectors.joining(",")) + " external service error message:" + response.getErrorMessage());
        }

        return response.isAccessAllowed();
    }

    /**
     * Checks if the logged in user has HENKILONHALLINTA roles that
     * grants access to the wanted person (personOid)
    */
    @Override
    @Transactional(readOnly = true)
    public boolean hasInternalAccess(String personOid, List<String> allowedRolesWithoutPrefix, Set<String> callingUserRoles) {
        return hasInternalAccess(personOid, singletonMap(PALVELU_HENKILONHALLINTA, allowedRolesWithoutPrefix), callingUserRoles);
    }

    /**
     * Checks if the logged in user has HENKILONHALLINTA roles that
     * grants access to the wanted person (personOid)
     */
    private boolean hasInternalAccess(String personOid, Map<String, List<String>> allowedPalveluRooli, Set<String> callingUserRoles) {
        if (this.isUserAdmin(callingUserRoles)) {
            return true;
        }

        Set<String> allowedRoles = getPrefixedRolesByPalveluRooli(allowedPalveluRooli);

        Optional<Henkilo> henkilo = henkiloDataRepository.findByOidHenkilo(personOid);
        if (!henkilo.isPresent()) {
            return false;
        }

        // If person doesn't have any organisation (and is not of type "OPPIJA") -> access is granted
        // Otherwise creating persons wouldn't work, as first the person is created and only after that
        // the person is attached to an organisation
        if (henkilo.get().getOrganisaatioHenkilos().isEmpty()) {
            HenkiloDto henkiloDto = oppijanumerorekisteriClient.getHenkiloByOid(personOid);
            if (!HenkiloTyyppi.OPPIJA.equals(henkiloDto.getHenkiloTyyppi())
                    && CollectionUtils.containsAny(callingUserRoles, allowedRoles)) {
                return true;
            }
        }

        Set<String> candidateRoles = new HashSet<>();
        for (OrganisaatioHenkilo orgHenkilo : henkilo.get().getOrganisaatioHenkilos()) {
            List<String> orgWithParents = this.organisaatioClient.getActiveParentOids(orgHenkilo.getOrganisaatioOid());
            for (String allowedRole : allowedRoles) {
                candidateRoles.addAll(getPrefixedRoles(allowedRole + "_", Lists.newArrayList(orgWithParents)));
            }
        }

        return CollectionUtils.containsAny(callingUserRoles, candidateRoles);
    }

    public static Set<String> getPrefixedRolesByPalveluRooli(Map<String, List<String>> palveluRoolit) {
        return palveluRoolit.keySet().stream().flatMap( palvelu ->
                    palveluRoolit.get(palvelu).stream().map( rooli -> ROLE_PREFIX + palvelu + "_" + rooli)).collect(Collectors.toSet());
    }

    @Override
    public boolean hasRoleForOrganisations(@NotNull List<Object> organisaatioHenkiloDtoList,
                                           List<String> allowedRolesWithoutPrefix) {
        List<String> orgOidList;
        if (organisaatioHenkiloDtoList == null || organisaatioHenkiloDtoList.isEmpty()) {
            LOG.warn(this.getCurrentUserOid() + " called permission checker with empty input");
            return true;
        }
        else if (organisaatioHenkiloDtoList.get(0) instanceof OrganisaatioHenkiloCreateDto) {
            orgOidList = organisaatioHenkiloDtoList.stream().map(OrganisaatioHenkiloCreateDto.class::cast)
                    .map(OrganisaatioHenkiloCreateDto::getOrganisaatioOid).collect(Collectors.toList());
        }
        else if (organisaatioHenkiloDtoList.get(0) instanceof OrganisaatioHenkiloUpdateDto) {
            orgOidList = organisaatioHenkiloDtoList.stream().map(OrganisaatioHenkiloUpdateDto.class::cast)
                    .map(OrganisaatioHenkiloUpdateDto::getOrganisaatioOid).collect(Collectors.toList());
        }
        else if (organisaatioHenkiloDtoList.get(0) instanceof HaettuKayttooikeusryhmaDto) {
            orgOidList = organisaatioHenkiloDtoList.stream().map(HaettuKayttooikeusryhmaDto.class::cast)
                    .map(HaettuKayttooikeusryhmaDto::getAnomus).map(AnomusDto::getOrganisaatioOid).collect(Collectors.toList());
        }
        else {
            throw new NotImplementedException("Unsupported input type.");
        }
        return checkRoleForOrganisation(orgOidList, allowedRolesWithoutPrefix);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkRoleForOrganisation(@NotNull List<String> orgOidList, List<String> allowedRolesWithoutPrefix) {
        for(String oid : orgOidList) {
            if (!this.hasRoleForOrganisation(oid, allowedRolesWithoutPrefix)) {
                return false;
            }
        }
        return true;
    }

    private static PermissionCheckResponseDto checkPermissionFromExternalService(String serviceUrl,
                                                                                 Set<String> personOidsForSamePerson,
                                                                                 Set<String> organisationOids,
                                                                                 Set<String> loggedInUserRoles) {
        PermissionCheckRequestDto requestDTO = new PermissionCheckRequestDto();
        requestDTO.setPersonOidsForSamePerson(Lists.newArrayList(personOidsForSamePerson));
        requestDTO.setOrganisationOids(Lists.newArrayList(organisationOids));
        requestDTO.setLoggedInUserRoles(loggedInUserRoles);

        try {
            HttpResponse httpResponse = restClient.post(
                    serviceUrl, "application/json; charset=UTF-8", objectMapper.writeValueAsString(requestDTO)
            );
            return objectMapper.readValue(httpResponse.getEntity().getContent(), PermissionCheckResponseDto.class);
        }
        catch (Exception e) {
            LOG.error("External permission check failed: " + e.toString());
            throw new RuntimeException("Failed: " + e);
        }
    }

    private static Set<String> getOidsRecursive(OrganisaatioPerustieto org) {
        Preconditions.checkArgument(!StringUtils.isBlank(org.getOid()), "Organisation oid cannot be blank!");

        Set<String> oids = Sets.newHashSet(org.getOid());

        for (OrganisaatioPerustieto child : org.getChildren()) {
            oids.addAll(getOidsRecursive(child));
        }

        return oids;
    }

    private static Set<String> getPrefixedRoles(final String prefix, final List<String> rolesWithoutPrefix) {
        return rolesWithoutPrefix.stream().map(prefix::concat).collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganisaatioPerustieto> listOrganisaatiosByHenkiloOid(String oid) {
        List<OrganisaatioPerustieto> organisaatios = new ArrayList<>();
        this.henkiloDataRepository.findByOidHenkilo(oid).ifPresent(henkilo -> {
            Set<OrganisaatioHenkilo> orgHenkilos = henkilo.getOrganisaatioHenkilos();
            List<String> organisaatioOids = orgHenkilos.stream().map(OrganisaatioHenkilo::getOrganisaatioOid).collect(Collectors.toList());
            organisaatios.addAll(organisaatioClient.listActiveOrganisaatioPerustiedotByOidRestrictionList(organisaatioOids));
        });
        return organisaatios;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRoleForOrganisation(String orgOid, final List<String> allowedRolesWithoutPrefix) {
        if (this.isCurrentUserAdmin()) {
            return true;
        }

        final Set<String> allowedRoles = getPrefixedRoles(ROLE_ANOMUSTENHALLINTA_PREFIX, allowedRolesWithoutPrefix);

        List<String> orgAndParentOids = this.organisaatioClient.getActiveParentOids(orgOid);
        if (orgAndParentOids.isEmpty()) {
            LOG.warn("Organization " + orgOid + " not found!");
            return false;
        }

        Set<Set<String>> candidateRolesByOrg = orgAndParentOids.stream()
                .map(orgOrParentOid -> allowedRoles.stream()
                        .map(role -> role.concat("_" + orgOrParentOid))
                        .collect(Collectors.toCollection(HashSet::new)))
                .collect(Collectors.toCollection(HashSet::new));

        Set<String> flattenedCandidateRolesByOrg = Sets.newHashSet(Iterables.concat(candidateRolesByOrg));

        return CollectionUtils.containsAny(flattenedCandidateRolesByOrg, this.getCasRoles());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getCurrentUserOrgnisationsWithPalveluRole(String palvelu, String role) {
        return this.getCasRoles().stream()
                .filter(casRole -> casRole.contains(palvelu + "_" + role))
                .flatMap(casRole -> {
                    int index = casRole.indexOf(this.commonProperties.getOrganisaatioPrefix());
                    if (index != -1) {
                        return Stream.of(casRole.substring(index));
                    }
                    return Stream.empty();
                })
                .collect(Collectors.toSet());
    }

    @Override
    public boolean notOwnData(String dataOwnderOid) {
        return !Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new NullPointerException("No user name available from SecurityContext!")).equals(dataOwnderOid);
    }

    @Override
    public String getCurrentUserOid() {
        String oid = SecurityContextHolder.getContext().getAuthentication().getName();
        if (oid == null) {
            throw new NullPointerException("No user name available from SecurityContext!");
        }
        return oid;
    }

    @Override
    public Set<String> getCasRoles(){
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    // Rekisterinpitäjä
    @Override
    public boolean isCurrentUserAdmin() {
        return this.isUserAdmin(this.getCasRoles());
    }

    // Rekisterinpitäjä
    @Override
    public boolean isUserAdmin(Set<String> userRoles) {
        return this.isUserMiniAdmin(userRoles, PALVELU_HENKILONHALLINTA, ROLE_ADMIN);
    }

    // OPH virkailija
    @Override
    public boolean isCurrentUserMiniAdmin() {
        return this.isUserMiniAdmin(this.getCasRoles());
    }

    // OPH virkailija
    @Override
    public boolean isUserMiniAdmin(Set<String> userRoles) {
        return userRoles.stream().anyMatch(role -> role.contains(this.commonProperties.getRootOrganizationOid()));
    }

    // OPH virkailija
    @Override
    public boolean isCurrentUserMiniAdmin(String palvelu, String rooli) {
        return this.isUserMiniAdmin(this.getCasRoles(), palvelu, rooli);
    }

    // OPH virkailija
    @Override
    public boolean isUserMiniAdmin(Set<String> userRoles, String palvelu, String rooli) {
        return userRoles.stream().anyMatch(role -> role.contains(palvelu + "_" + rooli + "_" + this.commonProperties.getOrganisaatioPrefix())
                && role.contains(this.commonProperties.getRootOrganizationOid()));
    }

    @Override
    public boolean hasOrganisaatioInHierarchy(String requiredOrganiaatioOid) {
        return this.hasOrganisaatioInHierarchy(Sets.newHashSet(requiredOrganiaatioOid)).isEmpty();
    }

    @Override
    public Set<String> hasOrganisaatioInHierarchy(Collection<String> requiredOrganiaatioOids) {
        List<String> currentUserOrgnisaatios = this.organisaatioHenkiloRepository
                .findDistinctOrganisaatiosForHenkiloOid(this.getCurrentUserOid());
        return requiredOrganiaatioOids.stream().filter(requiredOrganiaatioOid -> currentUserOrgnisaatios.stream()
                .anyMatch(organisaatioOid -> this.organisaatioClient.getActiveChildOids(organisaatioOid).stream()
                        .anyMatch(requiredOrganiaatioOid::equals)))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> hasOrganisaatioInHierarchy(Collection<String> requiredOrganiaatioOids, String palvelu, String rooli) {
        Set<String> casRoles = this.getCasRoles();
        return requiredOrganiaatioOids.stream().filter(requiredOrganiaatioOid -> casRoles.stream()
                .anyMatch(casRole -> casRole.contains(palvelu + "_" + rooli)
                        && this.organisaatioClient.getActiveParentOids(requiredOrganiaatioOid).stream()
                        .anyMatch(casRole::contains)))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean organisaatioViiteLimitationsAreValid(Long kayttooikeusryhmaId) {
        Set<OrganisaatioViite> organisaatioViite = this.kayttooikeusryhmaDataRepository.findById(kayttooikeusryhmaId)
                .orElseThrow(() -> new NotFoundException("Could not find kayttooikeusryhma with id " + kayttooikeusryhmaId.toString()))
                .getOrganisaatioViite();
        List<String> currentUserOrganisaatioOids = this.organisaatioHenkiloRepository
                .findByHenkiloOidHenkilo(UserDetailsUtil.getCurrentUserOid()).stream()
                .filter(((Predicate<OrganisaatioHenkilo>) OrganisaatioHenkilo::isPassivoitu).negate())
                .map(OrganisaatioHenkilo::getOrganisaatioOid)
                .collect(Collectors.toList());

        // When granting to root organisation it has no organisaatioviite
        return !(!org.springframework.util.CollectionUtils.isEmpty(organisaatioViite)
                // Root organisation users do not need to pass organisaatioviite (admin & mini-admin)
                && !this.isCurrentUserMiniAdmin()
                // Organisaatiohenkilo limitations are valid
                && currentUserOrganisaatioOids.stream()
                .noneMatch((orgOid) -> this.organisaatioLimitationCheck(orgOid, organisaatioViite)));
    }


    // Check that current user MKRT can grant wanted KOR
    @Override
    public boolean kayttooikeusMyontoviiteLimitationCheck(Long kayttooikeusryhmaId) {
        List<Long> masterIdList = this.myonnettyKayttoOikeusRyhmaTapahtumaRepository
                .findValidMyonnettyKayttooikeus(this.getCurrentUserOid()).stream()
                .map(MyonnettyKayttoOikeusRyhmaTapahtuma::getKayttoOikeusRyhma)
                .map(KayttoOikeusRyhma::getId)
                .collect(Collectors.toList());
        List<Long> slaveIds = this.kayttoOikeusRyhmaMyontoViiteRepository.getSlaveIdsByMasterIds(masterIdList);
        return this.isCurrentUserAdmin() || (!slaveIds.isEmpty() && slaveIds.contains(kayttooikeusryhmaId));
    }

    // Check that wanted KOR can be added to the wanted organisation
    @Override
    public boolean organisaatioLimitationCheck(String organisaatioOid, Set<OrganisaatioViite> viiteSet) {
        // Group organizations have to match only as a general set since they're not separated by type or by individual groups
        if (organisaatioOid.startsWith(this.commonProperties.getOrganisaatioRyhmaPrefix())) {
            return viiteSet.stream().map(OrganisaatioViite::getOrganisaatioTyyppi).collect(Collectors.toList())
                    .contains(this.commonProperties.getOrganisaatioRyhmaPrefix());
        }
        OrganisaatioPerustieto organisaatioPerustieto = this.organisaatioClient.getOrganisaatioPerustiedotCached(organisaatioOid)
                .orElseThrow(() -> new NotFoundException("Organisation not found with oid " + organisaatioOid));
        // Organization must have child items in it, so that the institution type can be fetched and verified
        if (!org.springframework.util.CollectionUtils.isEmpty(organisaatioPerustieto.getChildren())) {
            return organisaatioPerustieto.getChildren().stream()
                    .filter(childOrganisation -> OrganisaatioStatus.AKTIIVINEN.equals(childOrganisation.getStatus()))
                    .anyMatch(childOrganisation ->
                            viiteSet.stream().anyMatch(organisaatioViite ->
                                    organisaatioViite.getOrganisaatioTyyppi()
                                            .equals(!org.springframework.util.StringUtils.isEmpty(childOrganisation.getOppilaitostyyppi())
                                                    // Format: getOppilaitostyyppi() = "oppilaitostyyppi_11#1"
                                                    ? childOrganisation.getOppilaitostyyppi().substring(17, 19)
                                                    : null)
                                            || organisaatioViite.getOrganisaatioTyyppi().equals(organisaatioOid)));
        }
        // if the organization doesn't have child items, then it must be a top level organization or some other type
        // organization in which case the target organization OID must match the allowedPalveluRooli-to-organization OID
        return viiteSet.stream().map(OrganisaatioViite::getOrganisaatioTyyppi).collect(Collectors.toList())
                .contains(organisaatioOid);
    }
}
