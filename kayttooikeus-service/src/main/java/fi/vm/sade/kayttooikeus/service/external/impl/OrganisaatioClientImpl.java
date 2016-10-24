package fi.vm.sade.kayttooikeus.service.external.impl;

import fi.vm.sade.generic.rest.CachingRestClient;
import fi.vm.sade.kayttooikeus.config.properties.UrlConfiguration;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.organisaatio.api.search.OrganisaatioHakutulos;
import fi.vm.sade.organisaatio.api.search.OrganisaatioPerustieto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static fi.vm.sade.kayttooikeus.service.external.ExternalServiceException.mapper;
import static fi.vm.sade.kayttooikeus.util.FunctionalUtils.io;
import static fi.vm.sade.kayttooikeus.util.FunctionalUtils.retrying;
import static java.util.stream.Collectors.joining;

public class OrganisaatioClientImpl implements OrganisaatioClient {
    private final CachingRestClient restClient = new CachingRestClient()
            .setClientSubSystemCode("kayttooikeus.kayttooikeuspalvelu-service");
    private final UrlConfiguration urlConfiguration;

    public OrganisaatioClientImpl(UrlConfiguration urlConfiguration) {
        this.urlConfiguration = urlConfiguration;
    }

    @Override
    public List<OrganisaatioPerustieto> listOganisaatioPerustiedot(Collection<String> organisaatioOids) {
        if (organisaatioOids == null || organisaatioOids.isEmpty()) {
            return new ArrayList<>();
        }
        String url = urlConfiguration.url("organisaatio-service.organisaatio.hae");
        return retrying(io(() -> restClient.get(url+"?oidRestrictionList="+organisaatioOids.stream()
                        .collect(joining("&oidRestrictionList=")),OrganisaatioHakutulos.class)), 2)
                .get().orFail(mapper(url)).getOrganisaatiot();
    }
}
