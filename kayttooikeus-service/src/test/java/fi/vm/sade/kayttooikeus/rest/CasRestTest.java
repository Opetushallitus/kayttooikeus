package fi.vm.sade.kayttooikeus.rest;

import fi.vm.sade.kayttooikeus.DatabaseService;
import fi.vm.sade.kayttooikeus.config.ApplicationTest;
import fi.vm.sade.kayttooikeus.dto.KutsunTila;
import fi.vm.sade.kayttooikeus.service.OppijaCasTicketService;
import fi.vm.sade.kayttooikeus.service.dto.OppijaCasTunnistusDto;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static fi.vm.sade.kayttooikeus.controller.KutsuPopulator.kutsu;
import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.henkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloPopulator.organisaatioHenkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.TunnistusTokenPopulator.tunnistusToken;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@ApplicationTest
@AutoConfigureMockMvc
public class CasRestTest {

    /* Ruma kludge. Tällä kierretään mystinen @MockBean -ongelma. */
    @TestConfiguration
    static class OppijaCasConfiguration {
        @Bean
        @Primary
        public OppijaCasTicketService oppijaCasTicketService() {
            return (casTicket, service) -> new OppijaCasTunnistusDto("123456-7890", "Testi", "Testi-Petteri");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatabaseService databaseService;

    @MockBean
    private OppijanumerorekisteriClient oppijanumerorekisteriClient;

    @After
    public void cleanup() {
        databaseService.truncate();
    }

    @Test
    @WithMockUser(roles = "KAYTTOOIKEUS_TUNNISTUS")
    public void tunnistusKutsuToken() throws Exception {
        databaseService.populate(kutsu("etu", "suku", "sahkoposti@example.com")
                .tila(KutsunTila.AVOIN)
                .salaisuus("kutsuToken123")
                .aikaleima(LocalDateTime.now()));
        mockMvc.perform(get("/cas/tunnistus")
                .param("kutsuToken", "kutsuToken123")
                .param("kielisyys", "kielisyys123")
                .header("nationalidentificationnumber", "hetu123")
                .header("firstname", "etunimi123")
                .header("sn", "sukunimi123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/henkilo-ui/rekisteroidy?temporaryKutsuToken=")));
    }

    @Test
    @WithMockUser(roles = "KAYTTOOIKEUS_TUNNISTUS")
    public void tunnistusKutsuTokenEiLoydy() throws Exception {
        mockMvc.perform(get("/cas/tunnistus")
                .param("kutsuToken", "kutsuToken123")
                .param("kielisyys", "kielisyys123")
                .header("nationalidentificationnumber", "hetu123")
                .header("firstname", "etunimi123")
                .header("sn", "sukunimi123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", endsWith("/henkilo-ui/vahvatunnistusinfo/virhe/kielisyys123/vanhakutsu")));
    }

    @Test
    @WithMockUser(roles = "KAYTTOOIKEUS_TUNNISTUS")
    public void tunnistusLoginToken() throws Exception {
        databaseService.populate(tunnistusToken(henkilo("henkilo123").withUsername("kayttaja123"))
                .loginToken("loginToken123")
                .aikaleima(LocalDateTime.now()));
        when(oppijanumerorekisteriClient.getHenkiloByOid(any())).thenReturn(HenkiloDto.builder()
                .oidHenkilo("henkilo123")
                .hetu("hetu123")
                .build());

        mockMvc.perform(get("/cas/tunnistus")
                .param("loginToken", "loginToken123")
                .param("kielisyys", "kielisyys123")
                .header("nationalidentificationnumber", "hetu123")
                .header("firstname", "etunimi123")
                .header("sn", "sukunimi123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/henkilo-ui/uudelleenrekisterointi/kielisyys123/loginToken123/")));
    }

    @Test
    @WithMockUser(roles = "KAYTTOOIKEUS_TUNNISTUS")
    public void tunnistusLoginTokenEiLoydy() throws Exception {
        mockMvc.perform(get("/cas/tunnistus")
                .param("loginToken", "loginToken123")
                .param("kielisyys", "kielisyys123")
                .header("nationalidentificationnumber", "hetu123")
                .header("firstname", "etunimi123")
                .header("sn", "sukunimi123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", endsWith("/henkilo-ui/vahvatunnistusinfo/virhe/kielisyys123/vanha")));
    }

    @Test
    @WithMockUser(roles = "KAYTTOOIKEUS_TUNNISTUS")
    public void tunnistusLoginTokenOppijanumerorekisteriEiToimi() throws Exception {
        databaseService.populate(tunnistusToken(henkilo("henkilo123"))
                .loginToken("loginToken123")
                .aikaleima(LocalDateTime.now()));
        when(oppijanumerorekisteriClient.getHenkiloByOid(any())).thenThrow(new RuntimeException("oppijanumerorekisteri ei toimi"));

        mockMvc.perform(get("/cas/tunnistus")
                .param("loginToken", "loginToken123")
                .param("kielisyys", "kielisyys123")
                .header("nationalidentificationnumber", "hetu123")
                .header("firstname", "etunimi123")
                .header("sn", "sukunimi123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", endsWith("/henkilo-ui/vahvatunnistusinfo/virhe/kielisyys123/loginToken123")));
    }

    @Test
    @WithMockUser(roles = "KAYTTOOIKEUS_TUNNISTUS")
    public void tunnistusVahvaTunnistautuminen() throws Exception {
        databaseService.populate(organisaatioHenkilo(henkilo("henkilo123")
                .withUsername("kayttaja123"), "organisaatio123"));
        when(oppijanumerorekisteriClient.getHenkiloByHetu(any())).thenReturn(Optional.of(HenkiloDto.builder()
                .oidHenkilo("henkilo123")
                .build()));

        mockMvc.perform(get("/cas/tunnistus")
                .param("kielisyys", "kielisyys123")
                .header("nationalidentificationnumber", "hetu123")
                .header("firstname", "etunimi123")
                .header("sn", "sukunimi123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/henkilo-ui/uudelleenrekisterointi/kielisyys123/")));
    }

    @Test
    @WithMockUser(roles = "KAYTTOOIKEUS_TUNNISTUS")
    public void tunnistusVahvaTunnistautuminenHetuEiLoydy() throws Exception {
        when(oppijanumerorekisteriClient.getHenkiloByHetu(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/cas/tunnistus")
                .param("kielisyys", "kielisyys123")
                .header("nationalidentificationnumber", "hetu123")
                .header("firstname", "etunimi123")
                .header("sn", "sukunimi123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", endsWith("/henkilo-ui/vahvatunnistusinfo/virhe/kielisyys123/eiloydy")));
    }

}
