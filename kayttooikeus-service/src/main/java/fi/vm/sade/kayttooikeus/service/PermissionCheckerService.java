package fi.vm.sade.kayttooikeus.service;


import fi.vm.sade.kayttooikeus.dto.permissioncheck.ExternalPermissionService;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.PermissionCheckDto;
import fi.vm.sade.kayttooikeus.model.OrganisaatioViite;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PermissionCheckerService {

    @Deprecated
    boolean isAllowedToAccessPerson(String personOid, List<String> allowedRoles, ExternalPermissionService permissionService);

    boolean isAllowedToAccessPerson(String personOid, Map<String, List<String>> allowedRoles, ExternalPermissionService permissionService);

    @Deprecated
    boolean isAllowedToAccessPersonOrSelf(String personOid, List<String> allowedRoles, ExternalPermissionService permissionService);

    boolean isAllowedToAccessPersonOrSelf(String personOid, Map<String, List<String>> allowedRoles, ExternalPermissionService permissionService);

    boolean isAllowedToAccessPerson(PermissionCheckDto permissionCheckDto);

    @Deprecated
    boolean checkRoleForOrganisation(List<String> orgOidList, List<String> allowedRolesWithoutPrefix);

    boolean checkRoleForOrganisation(List<String> orgOidList, Map<String, List<String>> allowedRoles);

    List<OrganisaatioPerustieto> listActiveOrganisaatiosByHenkiloOid(String oid);

    boolean hasInternalAccess(String personOid, List<String> allowedRolesWithoutPrefix, Set<String> callingUserRoles);

    @Deprecated
    boolean hasRoleForOrganisations(List<Object> organisaatioHenkiloDtoList, List<String> allowedRolesWithoutPrefix);

    boolean hasRoleForOrganisations(List<Object> organisaatioHenkiloDtoList, Map<String, List<String>> allowedRoles);

    @Deprecated
    boolean hasRoleForOrganisation(String orgOid, List<String> allowedRolesWithoutPrefix);

    boolean hasRoleForOrganisation(String orgOid, Map<String, List<String>> allowedRoles);

    @Deprecated
    Set<String> getCurrentUserOrgnisationsWithPalveluRole(String palvelu, String role);

    Set<String> getCurrentUserOrgnisationsWithPalveluRole(Map<String, List<String>> palveluRoolit);

    boolean notOwnData(String dataOwnderOid);

    String getCurrentUserOid();

    Set<String> getCasRoles();

    /**
     * Rekisterinpitäjä
     * @return isRekisterinpitäjä
     */
    boolean isCurrentUserAdmin();

    // Rekisterinpitäjä
    boolean isUserAdmin(Set<String> userRoles);

    /**
     * OPH-virkailija
     * @return isOph-virkailija
     */
    boolean isCurrentUserMiniAdmin();

    // OPH virkailija
    boolean isUserMiniAdmin(Set<String> userRoles);

    /**
     * @param palvelu name
     * @param rooli name
     * @return isOph-virkailija with käyttöoikeus
     */
    boolean isCurrentUserMiniAdmin(String palvelu, String rooli);

    // OPH virkailija
    boolean isUserMiniAdmin(Set<String> userRoles, String palvelu, String rooli);

    boolean hasOrganisaatioInHierarchy(String requiredOrganiaatioOid);

    Set<String> hasOrganisaatioInHierarchy(Collection<String> requiredOrganiaatioOid);

    @Deprecated
    Set<String> hasOrganisaatioInHierarchy(Collection<String> requiredOrganiaatioOids, String palvelu, String rooli);

    Set<String> hasOrganisaatioInHierarchy(Collection<String> requiredOrganiaatioOids, Map<String, List<String>> palveluRoolit);

    /**
     * @param kayttooikeusryhmaId käyttöoikeusryhmästä joka halutaan myöntää
     * @return isValid
     */
    boolean organisaatioViiteLimitationsAreValid(Long kayttooikeusryhmaId);

    boolean kayttooikeusMyontoviiteLimitationCheck(Long kayttooikeusryhmaId);

    boolean organisaatioLimitationCheck(String organisaatioOid, Set<OrganisaatioViite> viiteSet);

    boolean organisaatioLimitationCheck(String organisaatioOid, List<OrganisaatioPerustieto> organisaatiot, Set<String> organisaatiorajoitteet);
}
