package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.model.OrganisaatioCache;
import fi.vm.sade.kayttooikeus.repositories.OrganisaatioCacheRepository;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import java.util.Arrays;
import java.util.Collection;
import static java.util.Collections.emptyList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import fi.vm.sade.kayttooikeus.dto.enumeration.OrganisaatioStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import static org.mockito.Matchers.anyInt;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OrganisaatioServiceImplTest {

    private OrganisaatioServiceImpl organisaatioServiceImpl;

    @Mock
    private OrganisaatioClient organisaatioClientMock;
    @Mock
    private OrganisaatioCacheRepository organisaatioCacheRepositoryMock;

    @Captor
    private ArgumentCaptor<Collection<OrganisaatioCache>> organisaatioCachesCaptor;

    @Before
    public void setup() {
        CommonProperties commonProperties = new CommonProperties();
        commonProperties.setRootOrganizationOid("1.2.246.562.10.00000000001");
        organisaatioServiceImpl = new OrganisaatioServiceImpl(
                organisaatioClientMock,
                organisaatioCacheRepositoryMock,
                commonProperties);
    }

    private OrganisaatioPerustieto organisaatio(String oid) {
        return organisaatio(oid, emptyList());
    }

    private OrganisaatioPerustieto organisaatio(String oid, List<OrganisaatioPerustieto> children) {
        OrganisaatioPerustieto organisaatioPerustieto = new OrganisaatioPerustieto();
        organisaatioPerustieto.setOid(oid);
        organisaatioPerustieto.setChildren(children);
        organisaatioPerustieto.setStatus(OrganisaatioStatus.AKTIIVINEN);
        return organisaatioPerustieto;
    }

    @Test
    public void updateOrganisaatioCache() {
        when(organisaatioClientMock.refreshCache()).thenReturn(
                Arrays.asList(organisaatio("1.2.246.562.10.48349941889",
                        Arrays.asList(organisaatio("1.2.246.562.10.23893528846"), organisaatio("1.2.246.562.10.11762519889")))
                ));

        organisaatioServiceImpl.updateOrganisaatioCache();

        verify(organisaatioClientMock).refreshCache();
        verify(organisaatioCacheRepositoryMock).deleteAllInBatch();
        verify(organisaatioCacheRepositoryMock).persistInBatch(organisaatioCachesCaptor.capture(), anyInt());
        Collection<OrganisaatioCache> organisaatiot = organisaatioCachesCaptor.getValue();
        assertThat(organisaatiot).extracting("organisaatioOid", "organisaatioOidPath").containsExactly(
                tuple("1.2.246.562.10.00000000001", "1.2.246.562.10.00000000001"),
                tuple("1.2.246.562.10.48349941889", "1.2.246.562.10.48349941889/1.2.246.562.10.00000000001"),
                tuple("1.2.246.562.10.23893528846", "1.2.246.562.10.23893528846/1.2.246.562.10.48349941889/1.2.246.562.10.00000000001"),
                tuple("1.2.246.562.10.11762519889", "1.2.246.562.10.11762519889/1.2.246.562.10.48349941889/1.2.246.562.10.00000000001")
        );
    }

}
