package fi.vm.sade.kayttooikeus.service.external;


import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkilonYhteystiedotViewDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoRyhmaKuvaus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Optional;

import static fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoRyhmaKuvaus.KOTIOSOITE;
import static fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoRyhmaKuvaus.PRIORITY_ORDER;
import static fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoRyhmaKuvaus.TYOOSOITE;
import static java.util.Collections.singletonList;
import static net.jadler.Jadler.onRequest;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
public class OppijanumerorekisteriClientTest extends AbstractClientTest {
    @Autowired
    private OppijanumerorekisteriClient client;
    
    @Test
    public void getHenkilonPerustiedotTest() throws Exception {
        casAuthenticated("1.2.3.4.5");
        onRequest().havingMethod(is("POST"))
                .havingPath(is("/oppijanumerorekisteri-service/henkilo/henkiloPerustietosByHenkiloOidList"))
                .havingBodyEqualTo("[\"1.2.3.4.5\"]")
                .respond().withStatus(OK).withContentType(MediaType.APPLICATION_JSON_UTF8.getType())
                .withBody(jsonResource("classpath:henkilo/henkilonPerustiedot.json"));
        List<HenkiloPerustietoDto> results = this.client.getHenkilonPerustiedot(singletonList("1.2.3.4.5"));
        assertEquals(1, results.size());
        assertEquals("1.2.3.4.5", results.get(0).getOidHenkilo());
        assertEquals("EN", results.get(0).getAsiointiKieli().getKieliKoodi());

        Optional<HenkiloPerustietoDto> result = this.client.getHenkilonPerustiedot("1.2.3.4.5");
        assertTrue(result.isPresent());
        assertEquals("1.2.3.4.5", result.get().getOidHenkilo());
    }

    @Test
    public void getHenkilonYhteystiedotTest() {
        casAuthenticated("1.2.3.4.5");
        onRequest().havingMethod(is("GET"))
                .havingPath(is("/oppijanumerorekisteri-service/henkilo/1.2.3.4.5/yhteystiedot"))
                .respond().withStatus(OK).withContentType(MediaType.APPLICATION_JSON_UTF8.getType())
                .withBody(jsonResource("classpath:henkilo/yhteystiedot.json"));
        HenkilonYhteystiedotViewDto yhteystiedot = this.client.getHenkilonYhteystiedot("1.2.3.4.5");
        assertNotNull(yhteystiedot);
        assertNotNull(yhteystiedot.get(TYOOSOITE));
        assertNotNull(yhteystiedot.get(KOTIOSOITE));
        assertEquals("tyo@osoite.fi", yhteystiedot.get(TYOOSOITE).getSahkoposti());
        assertEquals("+358451234567", yhteystiedot.get(KOTIOSOITE).getMatkapuhelinnumero());
        assertEquals("tyo@osoite.fi", yhteystiedot.getExclusively(PRIORITY_ORDER).getSahkoposti());
    }
}
