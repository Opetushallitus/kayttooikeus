package fi.vm.sade.kayttooikeus.service.external;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrganisaatioClient {
    List<String> getChildOids(String organisaatioOid);

    boolean activeExists(String organisaatioOid);

    List<OrganisaatioPerustieto> listActiveOganisaatioPerustiedotRecursiveCached(String organisaatioOid);

    List<OrganisaatioPerustieto> refreshCache();

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
     * @return kaikkien passiviisten organisaatioiden oidit
     */
    List<String> getLakkautetutOids();
}
