package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.dto.enumeration.OrganisaatioStatus;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
        when(organisaatioClientMock.refreshCache()).thenReturn(3L);

        organisaatioServiceImpl.updateOrganisaatioCache();

        verify(organisaatioClientMock).refreshCache();
    }

}
