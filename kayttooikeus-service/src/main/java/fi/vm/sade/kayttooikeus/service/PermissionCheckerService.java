package fi.vm.sade.kayttooikeus.service;


import fi.vm.sade.kayttooikeus.dto.permissioncheck.ExternalPermissionService;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.PermissionCheckDto;
import fi.vm.sade.kayttooikeus.model.OrganisaatioViite;
import fi.vm.sade.kayttooikeus.repositories.criteria.MyontooikeusCriteria;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PermissionCheckerService {

    boolean isAllowedToAccessPerson(String personOid, Map<String, List<String>> allowedRoles, ExternalPermissionService permissionService);

    boolean isAllowedToAccessPersonOrSelf(String personOid, Map<String, List<String>> allowedRoles, ExternalPermissionService permissionService);

    boolean isAllowedToAccessPerson(PermissionCheckDto permissionCheckDto);

    boolean checkRoleForOrganisation(List<String> orgOidList, Map<String, List<String>> allowedRoles);

    boolean hasRoleForOrganisations(List<Object> organisaatioHenkiloDtoList, Map<String, List<String>> allowedRoles);

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
    boolean isCurrentUserMiniAdmin(String palvelu, String rooli, String... muutRoolit);

    // OPH virkailija
    boolean isUserMiniAdmin(Set<String> userRoles, String palvelu, String rooli, String... muutRoolit);

    Set<String> hasOrganisaatioInHierarchy(Collection<String> requiredOrganiaatioOids, Map<String, List<String>> palveluRoolit);

    /**
     * @param organisaatioOid organisaatio johon käyttöoikeusryhmä halutaan myöntää
     * @param kayttooikeusryhmaId käyttöoikeusryhmästä joka halutaan myöntää
     * @return isValid
     */
    boolean organisaatioViiteLimitationsAreValid(String organisaatioOid, Long kayttooikeusryhmaId);

    boolean kayttooikeusMyontoviiteLimitationCheck(String organisaatioOid, Long kayttooikeusryhmaId);

    boolean kayttooikeusMyontoviiteLimitationCheck(String kayttajaOid, String organisaatioOid, Long kayttooikeusryhmaId, MyontooikeusCriteria criteria);

    boolean organisaatioLimitationCheck(String organisaatioOid, Set<OrganisaatioViite> viiteSet);

    boolean organisaatioLimitationCheck(String organisaatioOid, List<OrganisaatioPerustieto> organisaatiot, Set<String> organisaatiorajoitteet);
}
