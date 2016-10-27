package fi.vm.sade.kayttooikeus.controller;


import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.service.KayttoOikeusService;
import fi.vm.sade.kayttooikeus.service.TaskExecutorService;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class KayttoOikeusControllerTest extends AbstractControllerTest {
    @MockBean
    private KayttoOikeusService kayttoOikeusService;
    @MockBean
    private TaskExecutorService taskExecutorService;
    
    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_HENKILONHALLINTA_READ")
    public void listKayttoOikeusByPalveluTest() throws Exception {
        given(this.kayttoOikeusService.listKayttoOikeusByPalvelu("HENKILOHALLINTA"))
            .willReturn(singletonList(PalveluKayttoOikeusDto.builder()
                    .rooli("ROLE").oikeusLangs(new TextGroupListDto(1L).put("FI", "Nimi")
                        .put("EN", "Name"))
                    .build()));
        this.mvc.perform(get("/kayttooikeus/HENKILOHALLINTA").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().json(jsonResource("classpath:kayttooikeus/palveluOikeusList.json")));
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_HENKILONHALLINTA_READ")
    public void listKayttoOikeusCurrentUserTest() throws Exception {
        given(this.kayttoOikeusService.listMyonnettyKayttoOikeusHistoriaForCurrentUser())
                .willReturn(singletonList(KayttoOikeusHistoriaDto.builder()
                        .aikaleima(new LocalDate(2015, 1, 1).toDateTimeAtStartOfDay())
                        .kasittelija("kasittelija")
                        .kayttoOikeusId(1L)
                        .kuvaus(new TextGroupListDto(2L).put("FI", "nimi"))
                        .organisaatioOid("orgOid")
                        .tehtavanimike("nimike")
                        .tila(KayttoOikeudenTila.HYLATTY)
                        .tyyppi(KayttoOikeusTyyppi.KOOSTEROOLI)
                        .voimassaAlkuPvm(new LocalDate(2015, 1, 1))
                        .voimassaLoppuPvm(new LocalDate(2015, 12, 31))
                .build()));
        this.mvc.perform(get("/kayttooikeus/kayttaja/current").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().json(jsonResource("classpath:kayttooikeus/currentUserHistoria.json")));
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_HENKILONHALLINTA_OPHREKISTERI")
    public void sendExpirationRemindersTest() throws Exception {
        given(this.taskExecutorService.sendExpirationReminders(Matchers.any(Period.class))).willReturn(1);
        this.mvc.perform(post("/kayttooikeus/expirationReminders")
                .param("year", "2015").param("month", "1").param("day", "1"))
            .andExpect(status().isOk()).andExpect(content().string(is("1")));
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_HENKILONHALLINTA_OPHREKISTERI")
    public void getHaettuKayttooikeusRyhmasByOidTest() throws Exception{
        Map<String, List<Integer>> kayttooikeusRyhmasByOrganisaatio
                = Collections.singletonMap("1.0.0.1.0", Collections.singletonList(12000));
        String expectedContent = "{  \"1.0.0.1.0\": [" +
                "    12000" +
                "  ]}";
        given(this.kayttoOikeusService.findKayttooikeusryhmatAndOrganisaatioByHenkiloOid(anyString()))
                .willReturn(kayttooikeusRyhmasByOrganisaatio);
        this.mvc.perform(get("/kayttooikeusryhma/ryhmasByOrganisaatio/1.0.0.1.0").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(content().json(expectedContent));
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_HENKILONHALLINTA_OPHREKISTERI")
    public void getHaettuKayttooikeusRyhmasByOidInvalidDataTest() throws Exception{
        given(this.kayttoOikeusService.findKayttooikeusryhmatAndOrganisaatioByHenkiloOid(anyString()))
                .willThrow(new NullPointerException("null_ryhma_id"));
        this.mvc.perform(get("/kayttooikeusryhma/ryhmasByOrganisaatio/1.0.0.1.0").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isInternalServerError());
    }
}
