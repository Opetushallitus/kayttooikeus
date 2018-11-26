package fi.vm.sade.kayttooikeus.service.external;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;
import java.util.Set;

import static java.util.Collections.singletonList;
import static net.jadler.Jadler.onRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
public class OrganisaatioClientTest extends AbstractClientTest {
    @Autowired
    private OrganisaatioClient client;

    @Test
    public void getLakkautetutOidsTest() {
        onRequest().havingMethod(is("GET"))
                .havingPath(is("/organisaatio-service/rest/organisaatio/v4/hierarkia/hae"))
                .respond().withStatus(OK).withContentType(MediaType.APPLICATION_JSON_UTF8.getType())
                .withBody(jsonResource("classpath:organisaatio/organisaatioServiceHaeResponse.json"));
        onRequest().havingMethod(is("GET"))
                .havingPath(is("/organisaatio-service/rest/organisaatio/v4/1.2.246.562.10.00000000001"))
                .respond().withStatus(OK).withContentType(MediaType.APPLICATION_JSON_UTF8.getType())
                .withBody(jsonResource("classpath:organisaatio/organisaatioServiceRootOrganisation.json"));
        onRequest().havingMethod(is("GET"))
                .havingPath(is("/organisaatio-service/rest/organisaatio/v2/ryhmat"))
                .respond().withStatus(OK).withContentType(MediaType.APPLICATION_JSON_UTF8.getType())
                .withBody(jsonResource("classpath:organisaatio/ryhmat.json"));
        this.client.refreshCache();
        Set<String> lakkautetutOids = this.client.getLakkautetutOids();

        // lakkautetut organisaatiot sisältää sekä ryhmien että organisaatioiden passivoidut
        assertTrue(lakkautetutOids.contains("1.2.246.562.28.32497911273"));
        assertTrue(lakkautetutOids.contains("1.2.246.562.10.123456789"));

        assertFalse(lakkautetutOids.contains("1.2.246.562.10.234567890"));
        assertFalse(lakkautetutOids.contains("1.2.246.562.28.36046890756"));
        assertFalse(lakkautetutOids.contains(("1.2.246.562.10.14175756379")));
        assertTrue(lakkautetutOids.size() == 2);
    }

    @Test
    public void getOrganisaatioPerustiedotCachedRoot() {
        onRequest().havingMethod(is("GET"))
                .havingPath(is("/organisaatio-service/rest/organisaatio/v4/hierarkia/hae"))
                .respond().withStatus(OK).withContentType(MediaType.APPLICATION_JSON_UTF8.getType())
                .withBody(jsonResource("classpath:organisaatio/organisaatioServiceHaeResponse.json"));
        onRequest().havingMethod(is("GET"))
                .havingPath(is("/organisaatio-service/rest/organisaatio/v4/1.2.246.562.10.00000000001"))
                .respond().withStatus(OK).withContentType(MediaType.APPLICATION_JSON_UTF8.getType())
                .withBody(jsonResource("classpath:organisaatio/organisaatioServiceRootOrganisation.json"));
        onRequest().havingMethod(is("GET"))
                .havingPath(is("/organisaatio-service/rest/organisaatio/v2/ryhmat"))
                .respond().withStatus(OK).withContentType(MediaType.APPLICATION_JSON_UTF8.getType())
                .withBody(jsonResource("classpath:organisaatio/ryhmat.json"));
        client.refreshCache();

        Optional<OrganisaatioPerustieto> organisaatio = client.getOrganisaatioPerustiedotCached("1.2.246.562.10.00000000001");

        assertThat(organisaatio).hasValueSatisfying(org -> {
            assertThat(org)
                    .returns(singletonList("MUU_ORGANISAATIO"), OrganisaatioPerustieto::getOrganisaatiotyypit)
                    .returns(singletonList("MUU_ORGANISAATIO"), OrganisaatioPerustieto::getTyypit);
        });
    }

    @Test
    public void getOrganisaatioPerustiedotCachedNotRoot() {
        onRequest().havingMethod(is("GET"))
                .havingPath(is("/organisaatio-service/rest/organisaatio/v4/hierarkia/hae"))
                .respond().withStatus(OK).withContentType(MediaType.APPLICATION_JSON_UTF8.getType())
                .withBody(jsonResource("classpath:organisaatio/organisaatioServiceHaeResponse.json"));
        onRequest().havingMethod(is("GET"))
                .havingPath(is("/organisaatio-service/rest/organisaatio/v4/1.2.246.562.10.00000000001"))
                .respond().withStatus(OK).withContentType(MediaType.APPLICATION_JSON_UTF8.getType())
                .withBody(jsonResource("classpath:organisaatio/organisaatioServiceRootOrganisation.json"));
        onRequest().havingMethod(is("GET"))
                .havingPath(is("/organisaatio-service/rest/organisaatio/v2/ryhmat"))
                .respond().withStatus(OK).withContentType(MediaType.APPLICATION_JSON_UTF8.getType())
                .withBody(jsonResource("classpath:organisaatio/ryhmat.json"));
        client.refreshCache();

        Optional<OrganisaatioPerustieto> organisaatio = client.getOrganisaatioPerustiedotCached("1.2.246.562.10.14175756379");

        assertThat(organisaatio).hasValueSatisfying(org -> {
            assertThat(org)
                    .returns(singletonList("KOULUTUSTOIMIJA"), OrganisaatioPerustieto::getOrganisaatiotyypit)
                    .returns(singletonList("KOULUTUSTOIMIJA"), OrganisaatioPerustieto::getTyypit);
        });
    }

}
