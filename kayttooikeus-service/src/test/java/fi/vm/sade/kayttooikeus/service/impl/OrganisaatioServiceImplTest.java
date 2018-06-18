package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import fi.vm.sade.organisaatio.api.model.types.OrganisaatioStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OrganisaatioServiceImplTest {

    private OrganisaatioServiceImpl organisaatioServiceImpl;

    @Mock
    private OrganisaatioClient organisaatioClientMock;

    @Before
    public void setup() {
        CommonProperties commonProperties = new CommonProperties();
        commonProperties.setRootOrganizationOid("1.2.246.562.10.00000000001");
        organisaatioServiceImpl = new OrganisaatioServiceImpl(organisaatioClientMock);
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
    }

}
