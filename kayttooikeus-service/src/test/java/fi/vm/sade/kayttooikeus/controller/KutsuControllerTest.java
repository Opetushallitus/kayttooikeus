package fi.vm.sade.kayttooikeus.controller;

import com.google.common.collect.Sets;
import fi.vm.sade.kayttooikeus.dto.KutsuReadDto;
import fi.vm.sade.kayttooikeus.dto.TextGroupMapDto;
import fi.vm.sade.kayttooikeus.enumeration.KutsuOrganisaatioOrder;
import fi.vm.sade.kayttooikeus.service.KutsuService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
public class KutsuControllerTest extends AbstractControllerTest {
    @MockBean
    private KutsuService kutsuService;

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_HENKILONHALLINTA_CRUD")
    public void listAvoinKutsusTest() throws Exception {

        given(this.kutsuService.listKutsus(eq(KutsuOrganisaatioOrder.AIKALEIMA), eq(Sort.Direction.DESC), any(), eq(null), eq(20l)))
                .willReturn(singletonList(KutsuReadDto.builder()
                        .id(1L).aikaleima(LocalDateTime.of(2016,1,1, 0, 0, 0, 0))
                        .sahkoposti("posti@example.com")
                        .organisaatiot(Sets.newHashSet(
                                new KutsuReadDto.KutsuOrganisaatioDto(new TextGroupMapDto(3L).put("FI", "Oikeus"), "OID", null)
                        ))
                        .build()));
        this.mvc.perform(get("/kutsu").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().json(jsonResource("classpath:kutsu/simpleKutsuListaus.json")));
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_HENKILONHALLINTA_CRUD")
    public void createTest() throws Exception {
        given(kutsuService.createKutsu(any())).willReturn(1L);

        mvc.perform(post("/kutsu").contentType(MediaType.APPLICATION_JSON).content(jsonResource("classpath:kutsu/simpleKutsuLuonti.json")))
                .andExpect(status().isCreated())
                .andExpect(header().string("location", endsWith("/kutsu/1")));
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_HENKILONHALLINTA_CRUD")
    public void deleteTest() throws Exception {
        this.mvc.perform(delete("/kutsu/1").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
    }
}
