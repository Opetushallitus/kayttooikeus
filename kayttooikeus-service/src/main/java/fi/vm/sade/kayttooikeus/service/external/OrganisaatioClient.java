package fi.vm.sade.kayttooikeus.service.external;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public interface OrganisaatioClient {
    List<String> getChildOids(String organisaatioOid);

    /**
     * @param organisaatioOid Haettava organisaatio
     * @param filter  Suodatin jonka mukaiset organisaatiot palautetaan
     * @return Haetun organisaation ja tämän ylä- ja alaorganisaatioiden oidit
     */
    List<OrganisaatioPerustieto> listWithParentsAndChildren(String organisaatioOid,
                                                            Predicate<OrganisaatioPerustieto> filter);

    long refreshCache();

    Long getCacheOrganisationCount();

    Optional<OrganisaatioPerustieto> getOrganisaatioPerustiedotCached(String organisaatioOid);

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
     * @param filter Suodatin jonka mukaiset organisaatiot palautetaan
     * @return Haetun organisaation ja tämän aliorganisaatioiden oidit
     */
    Set<String> listWithChildOids(String organisaatioOid, Predicate<OrganisaatioPerustieto> filter);

    /**
     * @return kaikkien passiviisten organisaatioiden oidit
     */
    Set<String> getLakkautetutOids();
}
