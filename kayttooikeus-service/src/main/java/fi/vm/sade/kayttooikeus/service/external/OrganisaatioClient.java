package fi.vm.sade.kayttooikeus.service.external;

import fi.vm.sade.kayttooikeus.dto.enumeration.OrganisaatioStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public interface OrganisaatioClient {
    List<String> getChildOids(String organisaatioOid);

    boolean activeExists(String organisaatioOid);

    boolean existsByOidAndStatus(String organisaatioOid, Set<OrganisaatioStatus> statuses);

    List<OrganisaatioPerustieto> listActiveOganisaatioPerustiedotRecursiveCached(String organisaatioOid);

    long refreshCache();

    Long getCacheOrganisationCount();

    Optional<OrganisaatioPerustieto> getOrganisaatioPerustiedotCached(String organisaatioOid);

    List<OrganisaatioPerustieto> listActiveOrganisaatioPerustiedotByOidRestrictionList(Collection<String> organisaatioOids);

    /**
     * @param organisaatioOid Haettava organisaatio
     * @return Haetun organisaation ja tämän yläorganisaatioiden oidit
     */
    List<String> getParentOids(String organisaatioOid);

    /**
     * @param organisaatioOid Haettava organisaatio
     * @return Haetun organisaation ja tämän yläorganisaatioiden aktiiviset oidit
     */
    List<String> getActiveParentOids(String organisaatioOid);

    /**
     * @param organisaatioOid Haettava organisaatio
     * @return Haetun organisaation ja tämän aliorganisaatioiden aktiiviset oidit
     */
    List<String> getActiveChildOids(String organisaatioOid);

    /**
     * @param organisaatioOid Haettava organisaatio
     * @param filter Suodatin jonka mukaiset organisaatiot palautetaan
     * @return Haetun organisaation ja tämän yläorganisaatioiden oidit
     */
    Set<String> listWithChildOids(String organisaatioOid, Predicate<OrganisaatioPerustieto> filter);

    /**
     * @return kaikkien passiviisten organisaatioiden oidit
     */
    Set<String> getLakkautetutOids();
}
