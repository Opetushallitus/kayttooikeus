package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloDto;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloWithOrganisaatioDto;
import fi.vm.sade.kayttooikeus.dto.PalveluRooliGroup;
import fi.vm.sade.kayttooikeus.repositories.criteria.OrganisaatioHenkiloCriteria;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OrganisaatioHenkiloCustomRepository {

    List<String> findDistinctOrganisaatiosForHenkiloOid(String henkiloOid);

    List<String> findUsersOrganisaatioHenkilosByPalveluRoolis(String henkiloOid, PalveluRooliGroup requiredRoles);

    List<OrganisaatioHenkiloWithOrganisaatioDto> findActiveOrganisaatioHenkiloListDtos(String henkiloOoid);

    List<OrganisaatioHenkiloWithOrganisaatioDto> findActiveOrganisaatioHenkiloListDtos(String henkiloOoid, PalveluRooliGroup requiredRoles);

    Optional<OrganisaatioHenkiloDto> findByHenkiloOidAndOrganisaatioOid(String henkiloOid, String organisaatioOid);

    List<OrganisaatioHenkiloDto> findOrganisaatioHenkilosForHenkilo(String henkiloOid);

    Collection<String> findOrganisaatioOidBy(OrganisaatioHenkiloCriteria criteria);

    /**
     * Palauttaa tiedon kuuluuko henkilö annettuun organisaatioon.
     *
     * @param henkiloOid henkilö oid
     * @param organisaatioOid organisaatio oid
     * @param passivoitu onko henkilö-organisaatio -liitos voimassa
     * @return henkilö kuuluu organisaatioon
     */
    boolean isHenkiloInOrganisaatio(String henkiloOid, String organisaatioOid, boolean passivoitu);

    Set<String> findValidByKayttooikeus(String oidHenkilo, String palveluName, String rooli);
}
