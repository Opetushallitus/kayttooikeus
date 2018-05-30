package fi.vm.sade.kayttooikeus.service.external;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static java.util.Collections.singletonList;
import static net.jadler.Jadler.onRequest;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
public class OrganisaatioClientTest extends AbstractClientTest {
    @Autowired
    private OrganisaatioClient client;
    
    @Test
    public void listOganisaatioPerustiedotTest() throws Exception {
        onRequest().havingMethod(is("GET"))
                .havingPath(is("/organisaatio-service/rest/organisaatio/v2/hierarkia/hae"))
                .respond().withStatus(OK).withContentType(MediaType.APPLICATION_JSON_UTF8.getType())
                .withBody(jsonResource("classpath:organisaatio/organisaatioServiceHaeResponse.json"));
        onRequest().havingMethod(is("GET"))
                .havingPath(is("/organisaatio-service/rest/organisaatio/1.2.246.562.10.00000000001"))
                .respond().withStatus(OK).withContentType(MediaType.APPLICATION_JSON_UTF8.getType())
                .withBody(jsonResource("classpath:organisaatio/organisaatioServiceRootOrganisation.json"));
        onRequest().havingMethod(is("GET"))
                .havingPath(is("/organisaatio-service/rest/organisaatio/v2/ryhmat"))
                .respond().withStatus(OK).withContentType(MediaType.APPLICATION_JSON_UTF8.getType())
                .withBody("[]");
        this.client.refreshCache();
        List<OrganisaatioPerustieto> results = this.client.listActiveOrganisaatioPerustiedotByOidRestrictionList(singletonList("1.2.246.562.10.14175756379"));
        assertEquals(1, results.size());
        assertEquals("1.2.246.562.10.14175756379", results.get(0).getOid());
    }

    @Test
    public void getLakkautetutOidsTest() {
        onRequest().havingMethod(is("GET"))
                .havingPath(is("/organisaatio-service/rest/organisaatio/v2/hierarkia/hae"))
                .respond().withStatus(OK).withContentType(MediaType.APPLICATION_JSON_UTF8.getType())
                .withBody(jsonResource("classpath:organisaatio/organisaatioServiceHaeResponse.json"));
        onRequest().havingMethod(is("GET"))
                .havingPath(is("/organisaatio-service/rest/organisaatio/1.2.246.562.10.00000000001"))
                .respond().withStatus(OK).withContentType(MediaType.APPLICATION_JSON_UTF8.getType())
                .withBody(jsonResource("classpath:organisaatio/organisaatioServiceRootOrganisation.json"));
        onRequest().havingMethod(is("GET"))
                .havingPath(is("/organisaatio-service/rest/organisaatio/v2/ryhmat"))
                .respond().withStatus(OK).withContentType(MediaType.APPLICATION_JSON_UTF8.getType())
                .withBody(jsonResource("classpath:organisaatio/ryhmat.json"));
        this.client.refreshCache();
        List<String> lakkautetutOids = this.client.getLakkautetutOids();

        // lakkautetut organisaatiot sisältää sekä ryhmien että organisaatioiden passivoidut
        assertTrue(lakkautetutOids.contains("1.2.246.562.28.32497911273"));
        assertTrue(lakkautetutOids.contains("1.2.246.562.10.123456789"));

        assertFalse(lakkautetutOids.contains("1.2.246.562.10.234567890"));
        assertFalse(lakkautetutOids.contains("1.2.246.562.28.36046890756"));
        assertFalse(lakkautetutOids.contains(("1.2.246.562.10.14175756379")));
        assertTrue(lakkautetutOids.size() == 2);
    }
}
