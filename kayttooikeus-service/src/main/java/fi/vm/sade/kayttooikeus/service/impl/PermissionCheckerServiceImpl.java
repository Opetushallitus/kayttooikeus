package fi.vm.sade.kayttooikeus.service.impl;

import com.google.common.collect.Lists;
import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.ExternalPermissionService;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.PermissionCheckDto;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.PermissionCheckRequestDto;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.PermissionCheckResponseDto;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.KayttoOikeusRyhma;
import fi.vm.sade.kayttooikeus.model.OrganisaatioHenkilo;
import fi.vm.sade.kayttooikeus.model.OrganisaatioViite;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.repositories.KayttoOikeusRyhmaMyontoViiteRepository;
import fi.vm.sade.kayttooikeus.repositories.KayttooikeusryhmaDataRepository;
import fi.vm.sade.kayttooikeus.repositories.OrganisaatioHenkiloRepository;
import fi.vm.sade.kayttooikeus.repositories.criteria.MyontooikeusCriteria;
import fi.vm.sade.kayttooikeus.service.KayttajarooliProvider;
import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import fi.vm.sade.kayttooikeus.service.external.ExternalPermissionClient;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import fi.vm.sade.kayttooikeus.util.OrganisaatioMyontoPredicate;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fi.vm.sade.kayttooikeus.util.FunctionalUtils.appending;
import static fi.vm.sade.kayttooikeus.util.FunctionalUtils.not;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
public class PermissionCheckerServiceImpl implements PermissionCheckerService {
    private static final Logger LOG = LoggerFactory.getLogger(PermissionCheckerService.class);
    public static final String PALVELU_KAYTTOOIKEUS_PREFIX = "ROLE_APP_KAYTTOOIKEUS_";
    public static final String PALVELU_KAYTTOOIKEUS = "KAYTTOOIKEUS";
    public static final String ROLE_REKISTERINPITAJA = "REKISTERINPITAJA";
    public static final String ROLE_CRUD = "CRUD";
    public static final String ROLE_KUTSU_CRUD = "KUTSU_CRUD";
    public static final String ROLE_PREFIX = "ROLE_APP_";
    private static final String ROLE_PALVELUKAYTTAJA_CRUD = "ROLE_APP_KAYTTOOIKEUS_PALVELUKAYTTAJA_CRUD";

    private final HenkiloDataRepository henkiloDataRepository;
    private final KayttoOikeusRyhmaMyontoViiteRepository kayttoOikeusRyhmaMyontoViiteRepository;
    private final OrganisaatioHenkiloRepository organisaatioHenkiloRepository;
    private final KayttooikeusryhmaDataRepository kayttooikeusryhmaDataRepository;

    private final ExternalPermissionClient externalPermissionClient;
    private final OppijanumerorekisteriClient oppijanumerorekisteriClient;
    private final OrganisaatioClient organisaatioClient;

    private final KayttajarooliProvider kayttajarooliProvider;

    private final CommonProperties commonProperties;

    @Override
    @Transactional(readOnly = true)
    public boolean isAllowedToAccessPerson(String personOid, Map<String, List<String>> allowedRoles, ExternalPermissionService permissionService) {
        return isAllowedToAccessPerson(getCurrentUserOid(), personOid, allowedRoles, permissionService, this.getCasRoles());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAllowedToAccessPersonOrSelf(String personOid, Map<String, List<String>> allowedRoles, ExternalPermissionService permissionService) {
        String currentUserOid = getCurrentUserOid();
        return personOid.equals(currentUserOid)
                || isAllowedToAccessPerson(currentUserOid, personOid, allowedRoles, permissionService, this.getCasRoles());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAllowedToAccessPerson(PermissionCheckDto permissionCheckDto) {
        return isAllowedToAccessPerson(permissionCheckDto.getCallingUserOid(),
                permissionCheckDto.getUserOid(),
                permissionCheckDto.getAllowedPalveluRooli(),
                permissionCheckDto.getExternalPermissionService(),
                permissionCheckDto.getCallingUserRoles()
        );
    }

    /*
     * Check internally and externally whether currentuser has any of the palvelu/rooli pair combination given in allowedPalveluRooli
     * that grants access to the given user (personOidToAccess)
     */
    private boolean isAllowedToAccessPerson(String callingUserOid,
                                            String personOidToAccess,
                                            Map<String, List<String>> allowedPalveluRooli,
                                            ExternalPermissionService permissionCheckService,
                                            Set<String> callingUserRoles) {
        if (this.hasInternalAccess(personOidToAccess, allowedPalveluRooli, callingUserRoles)) {
            return true;
        }

        if (StringUtils.isBlank(personOidToAccess) || permissionCheckService == null) {
            LOG.error("isAllowedToAccess() called with empty personOid or permissionCheckService");
            return false;
        }

        // Get orgs for logged in user
        if (callingUserOid == null) {
            LOG.error("isAllowedToAccess(): no logged in user found -> return no permission");
            return false;
        }

        OrganisaatioMyontoPredicate organisaatioMyontoPredicate = new OrganisaatioMyontoPredicate(
                // myönnetään vain koski-palvelulle käyttöoikeudet passiivisiin organisaatioihin
                ExternalPermissionService.KOSKI.equals(permissionCheckService));
        Set<String> flattedOrgs = this.henkiloDataRepository.findByOidHenkilo(callingUserOid).map(henkilo ->
                henkilo.getOrganisaatioHenkilos().stream()
                        .filter(OrganisaatioHenkilo::isAktiivinen)
                        .map(OrganisaatioHenkilo::getOrganisaatioOid)
                        .filter(organisaatioOid -> this.hasRoleForOrganisation(organisaatioOid, allowedPalveluRooli, callingUserRoles))
                        .flatMap(organisaatioOid -> organisaatioClient
                                .listWithChildOids(organisaatioOid, organisaatioMyontoPredicate).stream())
                        .collect(Collectors.toSet()))
                .orElse(emptySet());
        if (flattedOrgs.isEmpty()) {
            LOG.error("No organisations found for logged in user with oid: " + callingUserOid);
            return false;
        }

        Set<String> personOidsForSamePerson = oppijanumerorekisteriClient.getAllOidsForSamePerson(personOidToAccess);

        PermissionCheckRequestDto permissionCheckRequestDto = new PermissionCheckRequestDto();
        permissionCheckRequestDto.setPersonOidsForSamePerson(Lists.newArrayList(personOidsForSamePerson));
        permissionCheckRequestDto.setOrganisationOids(Lists.newArrayList(flattedOrgs));
        permissionCheckRequestDto.setLoggedInUserRoles(callingUserRoles);
        permissionCheckRequestDto.setLoggedInUserOid(callingUserOid);

        PermissionCheckResponseDto response = externalPermissionClient.getPermission(permissionCheckService,  permissionCheckRequestDto);

        if (!response.isAccessAllowed()) {
            LOG.error("Insufficient roles. permission check done from external service: {} " +
                    "Logged in user: {} " +
                    "Accessed personId: {} " +
                    "Loginuser orgs: {} " +
                    "Palveluroles needed: {} " +
                    "User cas roles: {} " +
                    "PersonOidsForSamePerson: {} " +
                    "External service error message: {}",
                    permissionCheckService,
                    callingUserOid,
                    personOidToAccess,
                    String.join(", ", flattedOrgs),
                    String.join(", ", getPrefixedRolesByPalveluRooli(allowedPalveluRooli)),
                    String.join(", ", callingUserRoles),
                    String.join(", ", personOidsForSamePerson),
                    response.getErrorMessage());
        }

        return response.isAccessAllowed();
    }

    /**
     * Checks if the logged in user has roles that grants access to the wanted person (personOid) through
     * privileges and organisation hierarchy
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
            if (henkilo.get().getKayttajaTyyppi() != null && !KayttajaTyyppi.OPPIJA.equals(henkilo.get().getKayttajaTyyppi())
                    && CollectionUtils.containsAny(callingUserRoles, allowedRoles)) {
                return true;
            }
        }

        if (callingUserRoles.contains(ROLE_PALVELUKAYTTAJA_CRUD)
                && allowedRoles.contains(ROLE_PALVELUKAYTTAJA_CRUD)) {
            // käyttöoikeudella saa muokata vain palvelukäyttäjiä
            if (!KayttajaTyyppi.PALVELU.equals(henkilo.get().getKayttajaTyyppi())) {
                allowedRoles.remove(ROLE_PALVELUKAYTTAJA_CRUD);
            }
        }

        Set<String> candidateRoles = new HashSet<>();
        henkilo.get().getOrganisaatioHenkilos().stream().filter(OrganisaatioHenkilo::isAktiivinen).forEach(orgHenkilo -> {
            List<String> orgWithParents = this.organisaatioClient.getActiveParentOids(orgHenkilo.getOrganisaatioOid());
            for (String allowedRole : allowedRoles) {
                candidateRoles.addAll(getPrefixedRoles(allowedRole + "_", Lists.newArrayList(orgWithParents)));
            }
        });

        return CollectionUtils.containsAny(callingUserRoles, candidateRoles);
    }

    public static Set<String> getPrefixedRolesByPalveluRooli(Map<String, List<String>> palveluRoolit) {
        return palveluRoolit.keySet().stream().flatMap( palvelu ->
                    palveluRoolit.get(palvelu).stream().map( rooli -> ROLE_PREFIX + palvelu + "_" + rooli)).collect(Collectors.toSet());
    }

    @Override
    public boolean hasRoleForOrganisations(List<Object> organisaatioHenkiloDtoList,
            Map<String, List<String>> allowedRoles) {
        return hasRoleForOrganisations(organisaatioHenkiloDtoList, orgOidList
                -> checkRoleForOrganisation(orgOidList, allowedRoles));
    }

    private boolean hasRoleForOrganisations(List<Object> organisaatioHenkiloDtoList,
            Function<List<String>, Boolean> checkRoleForOrganisationFunc) {
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
        return checkRoleForOrganisationFunc.apply(orgOidList);
    }

    @Override
    public boolean checkRoleForOrganisation(List<String> orgOidList, Map<String, List<String>> allowedRoles) {
        for(String oid : orgOidList) {
            if (!this.hasRoleForOrganisation(oid, allowedRoles, this.getCasRoles())) {
                return false;
            }
        }
        return true;
    }

    private static Set<String> getPrefixedRoles(final String prefix, final List<String> rolesWithoutPrefix) {
        return rolesWithoutPrefix.stream().map(prefix::concat).collect(Collectors.toSet());
    }

    private boolean hasRoleForOrganisation(String orgOid, Map<String, List<String>> allowedRolesAsMap, Set<String> userRoles) {
        if (this.isCurrentUserAdmin()) {
            return true;
        }

        final Set<String> allowedRoles = getPrefixedRolesByPalveluRooli(allowedRolesAsMap);

        List<String> orgAndParentOids = this.organisaatioClient.getActiveParentOids(orgOid);
        if (orgAndParentOids.isEmpty()) {
            LOG.warn("Organization " + orgOid + " not found!");
            return false;
        }

        Set<String> flattenedCandidateRolesByOrg = orgAndParentOids.stream()
                .flatMap(orgOrParentOid -> allowedRoles.stream()
                        .map(role -> role.concat("_" + orgOrParentOid)))
                .collect(Collectors.toCollection(HashSet::new));

        return CollectionUtils.containsAny(flattenedCandidateRolesByOrg, userRoles);
    }

    @Override
    public Set<String> getCurrentUserOrgnisationsWithPalveluRole(Map<String, List<String>> palveluRoolit) {
        return this.getCasRoles().stream()
                .filter(casRole -> palveluRoolit.entrySet().stream().anyMatch(entry -> entry.getValue().stream().anyMatch(role -> casRole.contains(entry.getKey() + "_" + role))))
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

    private boolean isRekisterinpitaja(String kayttajaOid) {
        if (getCurrentUserOid().equals(kayttajaOid)) {
            return isCurrentUserAdmin();
        }
        return isUserAdmin(kayttajarooliProvider.getByKayttajaOid(kayttajaOid));
    }

    // Rekisterinpitäjä
    @Override
    public boolean isCurrentUserAdmin() {
        return this.isUserAdmin(this.getCasRoles());
    }

    // Rekisterinpitäjä
    @Override
    public boolean isUserAdmin(Set<String> userRoles) {
        return this.isUserMiniAdmin(userRoles, PALVELU_KAYTTOOIKEUS, ROLE_REKISTERINPITAJA);
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
    public boolean isCurrentUserMiniAdmin(String palvelu, String rooli, String... muutRoolit) {
        return this.isUserMiniAdmin(this.getCasRoles(), palvelu, rooli, muutRoolit);
    }

    // OPH virkailija
    @Override
    public boolean isUserMiniAdmin(Set<String> userRoles, String palvelu, String rooli, String... muutRoolit) {
        List<String> kaikkiRoolit = Stream.concat(Stream.of(rooli), Arrays.stream(muutRoolit)).collect(Collectors.toList());
        return userRoles.stream().anyMatch(role -> kaikkiRoolit.stream().anyMatch(haluttuRooli -> role.contains(palvelu + "_" + haluttuRooli + "_" + this.commonProperties.getOrganisaatioPrefix()))
                && role.contains(this.commonProperties.getRootOrganizationOid()));
    }

    @Override
    public Set<String> hasOrganisaatioInHierarchy(Collection<String> requiredOrganiaatioOids, Map<String, List<String>> palveluRoolit) {
        Set<String> casRoles = this.getCasRoles();
        return requiredOrganiaatioOids.stream().filter(requiredOrganiaatioOid -> casRoles.stream()
                .anyMatch(casRole -> palveluRoolit.entrySet().stream().anyMatch(entry -> entry.getValue().stream().anyMatch(rooli -> casRole.contains(entry.getKey() + "_" + rooli)))
                        && this.organisaatioClient.getActiveParentOids(requiredOrganiaatioOid).stream()
                        .anyMatch(casRole::contains)))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean organisaatioViiteLimitationsAreValid(String organisaatioOid, Long kayttooikeusryhmaId) {
        if (isCurrentUserAdmin()) {
            return true;
        }
        return kayttooikeusryhmaDataRepository.findById(kayttooikeusryhmaId)
                .map(KayttoOikeusRyhma::getOrganisaatioViite)
                .filter(not(Collection::isEmpty))
                .map(organisaatioViite -> organisaatioLimitationCheck(organisaatioOid, organisaatioViite))
                .orElse(false);
    }


    // Check that current user MKRT can grant wanted KOR
    @Override
    public boolean kayttooikeusMyontoviiteLimitationCheck(String organisaatioOid, Long kayttooikeusryhmaId) {
        return kayttooikeusMyontoviiteLimitationCheck(getCurrentUserOid(), organisaatioOid, kayttooikeusryhmaId);
    }

    @Override
    public boolean kayttooikeusMyontoviiteLimitationCheck(String kayttajaOid, String organisaatioOid, Long kayttooikeusryhmaId) {
        boolean rekisterinpitaja = isRekisterinpitaja(kayttajaOid);
        if (rekisterinpitaja) {
            return true;
        }
        Map<String, Set<Long>> myontooikeudet = this.kayttoOikeusRyhmaMyontoViiteRepository
                .getSlaveIdsByMasterHenkiloOid(kayttajaOid, MyontooikeusCriteria.oletus());
        lisaaAliorganisaatiot(myontooikeudet, rekisterinpitaja);
        return myontooikeudet.getOrDefault(organisaatioOid, emptySet()).contains(kayttooikeusryhmaId);
    }

    private void lisaaAliorganisaatiot(Map<String, Set<Long>> myontooikeudet, boolean passiiviset) {
        myontooikeudet.entrySet().stream()
                .flatMap(entry -> organisaatioClient.listWithChildOids(entry.getKey(), new OrganisaatioMyontoPredicate(passiiviset))
                        .stream()
                        .map(aliorganisaatioOid -> new AbstractMap.SimpleEntry<>(aliorganisaatioOid, entry.getValue())))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, appending()))
                .forEach((organisaatioOid, kayttooikeusryhmaIds) -> myontooikeudet.merge(organisaatioOid, kayttooikeusryhmaIds, appending()));
    }

    // Check that wanted KOR can be added to the wanted organisation
    @Override
    public boolean organisaatioLimitationCheck(String organisaatioOid, Set<OrganisaatioViite> viiteSet) {
        List<OrganisaatioPerustieto> organisaatiot = this.organisaatioClient.listWithParentsAndChildren(organisaatioOid,
                new OrganisaatioMyontoPredicate(isCurrentUserAdmin()));
        return organisaatioLimitationCheck(organisaatioOid, organisaatiot, viiteSet.stream().map(OrganisaatioViite::getOrganisaatioTyyppi).collect(Collectors.toSet()));
    }

    @Override
    public boolean organisaatioLimitationCheck(String organisaatioOid, List<OrganisaatioPerustieto> organisaatiot, Set<String> viiteSet) {
        // Group organizations have to match only as a general set since they're not separated by type or by individual groups
        if (organisaatioOid.startsWith(this.commonProperties.getOrganisaatioRyhmaPrefix())) {
            return viiteSet.contains(this.commonProperties.getOrganisaatioRyhmaPrefix());
        }
        return viiteSet.stream().anyMatch(organisaatioOid::equals)
                || organisaatiot.stream().anyMatch(organisaatio -> organisaatioLimitationCheck(organisaatioOid, viiteSet, organisaatio));
    }

    private static boolean organisaatioLimitationCheck(String organisaatioOid, Set<String> viiteSet, OrganisaatioPerustieto childOrganisation) {
        return viiteSet.stream().anyMatch(organisaatioViite ->
                                    organisaatioViite
                                            .equals(!org.springframework.util.StringUtils.isEmpty(childOrganisation.getOppilaitostyyppi())
                                                    // Format: getOppilaitostyyppi() = "oppilaitostyyppi_11#1"
                                                    ? childOrganisation.getOppilaitostyyppi().substring(17, 19)
                                                    : null)
                                            || organisaatioOid.equals(childOrganisation.getOid()) && childOrganisation.hasAnyOrganisaatiotyyppi(organisaatioViite));
    }
}
