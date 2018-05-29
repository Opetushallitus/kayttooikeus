package fi.vm.sade.kayttooikeus.service.external.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.sade.generic.rest.CachingRestClient;
import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.config.properties.UrlConfiguration;
import fi.vm.sade.kayttooikeus.service.external.ExternalServiceException;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioHakutulos;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import fi.vm.sade.organisaatio.api.model.types.OrganisaatioStatus;
import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;
import lombok.extern.slf4j.Slf4j;

import javax.validation.ValidationException;
import java.util.*;
import java.util.stream.Collectors;

import static com.carrotsearch.sizeof.RamUsageEstimator.humanReadableUnits;
import static com.carrotsearch.sizeof.RamUsageEstimator.sizeOf;
import static fi.vm.sade.kayttooikeus.service.external.ExternalServiceException.mapper;
import static fi.vm.sade.kayttooikeus.util.FunctionalUtils.io;
import static fi.vm.sade.kayttooikeus.util.FunctionalUtils.retrying;
import static java.util.stream.Collectors.toList;

@Slf4j
public class OrganisaatioClientImpl implements OrganisaatioClient {
    private final CachingRestClient restClient = new CachingRestClient()
            .setClientSubSystemCode("kayttooikeus.kayttooikeuspalvelu-service");
    private final UrlConfiguration urlConfiguration;
    private final String rootOrganizationOid;
    private final ObjectMapper objectMapper;
    private final OrikaBeanMapper orikaBeanMapper;

    private OrganisaatioCache cache;
    
    
    public OrganisaatioClientImpl(UrlConfiguration urlConfiguration,
                                  ObjectMapper objectMapper,
                                  CommonProperties commonProperties,
                                  OrikaBeanMapper orikaBeanMapper) {
        this.urlConfiguration = urlConfiguration;
        this.objectMapper = objectMapper;
        this.rootOrganizationOid = commonProperties.getRootOrganizationOid();
        this.orikaBeanMapper = orikaBeanMapper;
    }

    @Override
    public synchronized List<OrganisaatioPerustieto> refreshCache() {
        Map<String, String> queryParamsAktiivisetSuunnitellut = new HashMap<String, String>() {{
            put("aktiiviset", "true");
            put("suunnitellut", "true");
            put("lakkautetut", "true");
        }};
        String haeHierarchyUrl = this.urlConfiguration.url("organisaatio-service.organisaatio.v2.hae", queryParamsAktiivisetSuunnitellut);
        // Add organisations to cache (active, incoming and passive)
        List<OrganisaatioPerustieto> organisaatiosWithoutRootOrg =
                retrying(io(() -> this.restClient.get(haeHierarchyUrl, OrganisaatioHakutulos.class)), 2)
                        .get().orFail(mapper(haeHierarchyUrl)).getOrganisaatiot();
        // Add ryhmas to cache
        String haeRyhmasUrl = this.urlConfiguration.url("organisaatio-service.organisaatio.ryhmat");
        organisaatiosWithoutRootOrg.addAll(Arrays.stream(retrying(io(() ->
                this.restClient.get(haeRyhmasUrl, OrganisaatioPerustieto[].class)), 2)
                .get().<ExternalServiceException>orFail(mapper(haeRyhmasUrl)))
                // Make ryhma parentoidpath format same as on normal organisations.
                .map(ryhma -> {
                    ryhma.setParentOidPath(ryhma.getOid() + "/"
                            + ryhma.getParentOidPath().replaceAll("^\\||\\|$", "").replace("|", "/"));
                    return ryhma;
                }).collect(Collectors.toSet()));
        this.cache = new OrganisaatioCache(this.fetchPerustiedot(this.rootOrganizationOid), organisaatiosWithoutRootOrg);
        log.info("Organisation client cache refreshed. Cache size " + humanReadableUnits(sizeOf(this.cache)));
        return organisaatiosWithoutRootOrg;
    }

    private OrganisaatioPerustieto fetchPerustiedot(String oid) {
        String url = urlConfiguration.url("organisaatio-service.organisaatio.perustiedot", oid);
        return this.orikaBeanMapper.map(retrying(io(() -> (OrganisaatioRDTO) objectMapper.readerFor(OrganisaatioRDTO.class)
                .readValue(restClient.getAsString(url))), 2).get().orFail(mapper(url)), OrganisaatioPerustieto.class);
    }

    @Override
    public Long getCacheOrganisationCount() {
        return this.cache.getCacheCount();
    }
    @Override
    public Optional<OrganisaatioPerustieto> getOrganisaatioPerustiedotCached(String oid) {
        return this.cache.getByOid(oid);
    }

    @Override
    public boolean activeExists(String organisaatioOid) {
        return this.getOrganisaatioPerustiedotCached(organisaatioOid)
                .filter(organisaatioPerustieto -> OrganisaatioStatus.AKTIIVINEN.equals(organisaatioPerustieto.getStatus()))
                .isPresent();
    }

    @Override
    public List<OrganisaatioPerustieto> listActiveOganisaatioPerustiedotRecursiveCached(String organisaatioOid) {
        return this.cache.flatWithParentsAndChildren(organisaatioOid)
                // the resource never returns the root
                .filter(org -> !rootOrganizationOid.equals(org.getOid()))
                .filter(org -> OrganisaatioStatus.AKTIIVINEN.equals(org.getStatus()))
                .collect(toList());
    }

    @Override
    public List<OrganisaatioPerustieto> listActiveOrganisaatioPerustiedotByOidRestrictionList(Collection<String> organisaatioOids) {
        return organisaatioOids.stream()
                .map(this.cache::getByOid)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(organisaatioPerustieto -> OrganisaatioStatus.AKTIIVINEN.equals(organisaatioPerustieto.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getParentOids(String organisaatioOid) {
        return this.cache.flatWithParentsByOid(organisaatioOid).map(OrganisaatioPerustieto::getOid).collect(toList());
    }

    @Override
    public List<String> getActiveParentOids(String organisaatioOid) {
        return this.cache.flatWithParentsByOid(organisaatioOid)
                .filter(organisaatioPerustieto -> OrganisaatioStatus.AKTIIVINEN.equals(organisaatioPerustieto.getStatus()))
                .map(OrganisaatioPerustieto::getOid)
                .collect(toList());
    }

    @Override
    public List<String> getChildOids(String organisaatioOid) {
        return this.cache.flatWithChildrenByOid(organisaatioOid)
                        .map(OrganisaatioPerustieto::getOid)
                        .collect(toList());
    }

    @Override
    public List<String> getActiveChildOids(String organisaatioOid) {
        return this.cache.flatWithChildrenByOid(organisaatioOid)
                .filter(organisaatioPerustieto -> OrganisaatioStatus.AKTIIVINEN.equals(organisaatioPerustieto.getStatus()))
                .map(OrganisaatioPerustieto::getOid)
                .collect(toList());
    }

    @Override
    public List<String> getLakkautetutOids() {
        Set<OrganisaatioPerustieto> organisaatiosWithoutRootOrg = this.cache.flatAllOrganisaatios()
                .filter(organisaatioPerustieto -> OrganisaatioStatus.PASSIIVINEN.equals(organisaatioPerustieto.getStatus()))
                .collect(Collectors.toSet());

        Map<String, String> queryParamsLakkautetutRyhmat = new HashMap<String, String>() {{
            put("aktiiviset", "false");
            put("lakkautetut", "true");
        }};
        String haeRyhmasUrl = this.urlConfiguration.url("organisaatio-service.organisaatio.ryhmat", queryParamsLakkautetutRyhmat);
        Set<OrganisaatioPerustieto> ryhmas = Arrays.stream(retrying(io(() ->
                this.restClient.get(haeRyhmasUrl, OrganisaatioPerustieto[].class)), 2)
                .get().<ExternalServiceException>orFail(mapper(haeRyhmasUrl)))
                .filter(ryhma -> OrganisaatioStatus.PASSIIVINEN.equals(ryhma.getStatus()))
                // Make ryhma parentoidpath format same as on normal organisations.
                .map(ryhma -> {
                    ryhma.setParentOidPath(ryhma.getOid() + "/"
                            + ryhma.getParentOidPath().replaceAll("^\\||\\|$", "").replace("|", "/"));
                    return ryhma;
                }).collect(Collectors.toSet());

        organisaatiosWithoutRootOrg.addAll(ryhmas);

        return organisaatiosWithoutRootOrg.stream().map(OrganisaatioPerustieto::getOid).collect(toList());
    }

}
