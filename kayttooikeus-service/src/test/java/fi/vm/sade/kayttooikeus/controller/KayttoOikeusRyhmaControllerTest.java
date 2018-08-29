package fi.vm.sade.kayttooikeus.controller;


import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.service.KayttoOikeusService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class KayttoOikeusRyhmaControllerTest extends AbstractControllerTest {
    @MockBean
    private KayttoOikeusService kayttoOikeusService;

    @Test
    public void listKayttoOikeusRyhmaDeniedTest() throws Exception {
        this.mvc.perform(get("/kayttooikeusryhma").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_HENKILONHALLINTA_READ")
    public void listKayttoOikeusRyhmaTest() throws Exception {
        given(this.kayttoOikeusService.listAllKayttoOikeusRyhmas())
            .willReturn(singletonList(KayttoOikeusRyhmaDto.builder()
                    .organisaatioViite(singletonList(OrganisaatioViiteDto.builder()
                            .organisaatioTyyppi("organisaatiotyyppi")
                            .id(44L).build()))
                    .rooliRajoite("roolirajoite")
                    .id(14L)
                    .tunniste("Nimi")
                    .nimi(new TextGroupListDto(1L).put("FI", "Test"))
                    .build()));

        this.mvc.perform(get("/kayttooikeusryhma").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().json(jsonResource("classpath:kayttooikeusryhma/kayttoOikeusRyhmaList.json")));
    }

    @Test
    public void listKayttoOikeusRyhmasByOrdOidDeniedTest() throws Exception {
        this.mvc.perform(get("/kayttooikeusryhma/organisaatio/123").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_HENKILONHALLINTA_READ")
    public void listKayttoOikeusRyhmasByOrdOidTest() throws Exception {
        given(this.kayttoOikeusService.listPossibleRyhmasByOrganization("123"))
                .willReturn(singletonList(KayttoOikeusRyhmaDto.builder()
                        .id(33L)
                        .tunniste("Käyttöoikeusryhmä")
                        .nimi(new TextGroupListDto(1L).put("FI", "Kuvaus"))
                        .organisaatioViite(singletonList(OrganisaatioViiteDto.builder()
                                .organisaatioTyyppi("organisaatiotyyppi")
                                .id(44L).build()))
                        .rooliRajoite("Roolirajoite test")
                        .build()));

        this.mvc.perform(get("/kayttooikeusryhma/organisaatio/123").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().json(jsonResource("classpath:kayttooikeusryhma/kayttoOikeusRyhmaByOrganisaatio.json")));
    }

    @Test
    public void listKayttoOikeusRyhmasIncludingHenkilosDeniedTest() throws Exception {
        this.mvc.perform(get("/kayttooikeusryhma/234/123").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = {
        "ROLE_APP_HENKILONHALLINTA_OPHREKISTERI",
        "ROLE_APP_HENKILONHALLINTA_OPHREKISTERI_1.2.246.562.10.00000000001",
    })
    public void listKayttoOikeusRyhmasIncludingHenkilosTest() throws Exception {
        given(this.kayttoOikeusService.listMyonnettyKayttoOikeusRyhmasMergedWithHenkilos("234", "123", "1.2.3.4.5"))
                .willReturn(singletonList(MyonnettyKayttoOikeusDto.builder()
                        .ryhmaId(234L)
                        .alkuPvm(LocalDate.of(2015, 1, 2))
                        .kasitelty(LocalDateTime.of(2015, 1, 1, 0, 0 , 0, 0))
                        .kasittelijaNimi("Käsittelijä nimi")
                        .kasittelijaOid("123456.234")
                        .muutosSyy("syy muutokselle")
                        .removed(false)
                        .organisaatioOid("234.2435.234.12343.23")
                        .ryhmaNames(new TextGroupListDto(1L).put("FI", "Ryhmänimi"))
                        .tila(KayttoOikeudenTila.MYONNETTY)
                        .tyyppi("joku tyyppi")
                        .myonnettyTapahtumaId(43L)
                        .selected(true)
                        .tehtavanimike("joku nimike")
                        .voimassaPvm(LocalDate.of(2015, 1, 22))
                        .build()));

        this.mvc.perform(get("/kayttooikeusryhma/234/123").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().json(jsonResource("classpath:kayttooikeusryhma/kayttooikeusRyhmas.json")));
    }

    @Test
    public void listKayttoOikeusRyhmaByHenkiloDeniedTest() throws Exception {
        this.mvc.perform(get("/kayttooikeusryhma/henkilo/1.2.3.4.5").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_HENKILONHALLINTA_READ")
    public void listKayttoOikeusRyhmaByHenkiloTest() throws Exception {
        given(this.kayttoOikeusService.listMyonnettyKayttoOikeusRyhmasByHenkiloAndOrganisaatio("1.2.3.4.5", null))
                .willReturn(singletonList(buildKayttoOikeusForHenkilo()));

        this.mvc.perform(get("/kayttooikeusryhma/henkilo/1.2.3.4.5").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().json(jsonResource("classpath:kayttooikeusryhma/kayttooikeusRyhma.json")));
    }

    @Test
    public void listKayttoOikeusRyhmaByCurrentHenkiloDeniedTest() throws Exception {
        this.mvc.perform(get("/kayttooikeusryhma/henkilo/current").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_HENKILONHALLINTA_READ")
    public void listKayttoOikeusRyhmaByCurrentHenkiloTest() throws Exception {
        given(this.kayttoOikeusService.listMyonnettyKayttoOikeusRyhmasByHenkiloAndOrganisaatio("1.2.3.4.5", null))
                .willReturn(singletonList(buildKayttoOikeusForHenkilo()));

        this.mvc.perform(get("/kayttooikeusryhma/henkilo/current").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().json(jsonResource("classpath:kayttooikeusryhma/kayttooikeusRyhma.json")));
    }


    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_HENKILONHALLINTA_READ")
    public void getKayttoOikeusRyhmaDeniedTest() throws Exception {
        this.mvc.perform(get("/kayttooikeusryhma/44").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_KAYTTOOIKEUS_KAYTTOOIKEUSRYHMIEN_LUKU")
    public void getKayttoOikeusRyhmaTest() throws Exception {
        given(this.kayttoOikeusService.findKayttoOikeusRyhma(44L))
                .willReturn(buildKayttoOikeusRyhma());

        this.mvc.perform(get("/kayttooikeusryhma/44").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().json(jsonResource("classpath:kayttooikeusryhma/kayttooikeusRyhma2.json")));
    }

    @Test
    public void getSubRyhmasByKayttoOikeusRyhmaDeniedTest() throws Exception {
        this.mvc.perform(get("/kayttooikeusryhma/44/sallitut").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_KAYTTOOIKEUS_KAYTTOOIKEUSRYHMIEN_LUKU")
    public void getSubRyhmasByKayttoOikeusRyhmaTest() throws Exception {
        given(this.kayttoOikeusService.findSubRyhmasByMasterRyhma(44L))
                .willReturn(singletonList(buildKayttoOikeusRyhma()));

        this.mvc.perform(get("/kayttooikeusryhma/44/sallitut").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().json(jsonResource("classpath:kayttooikeusryhma/kayttooikeusRyhma2list.json")));
    }

    private KayttoOikeusRyhmaDto buildKayttoOikeusRyhma() {
        return KayttoOikeusRyhmaDto.builder()
                .id(44L)
                .rooliRajoite("roolirajoite")
                .nimi(new TextGroupListDto(1L).put("FI", "Kuvaus").put("EN", "kuvaus en"))
                .tunniste("ryhmänimi")
                .organisaatioViite(singletonList(OrganisaatioViiteDto.builder()
                        .organisaatioTyyppi("organisaatiotyyppi")
                        .id(3423L).build()))
                .build();
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_HENKILONHALLINTA_READ")
    public void getKayttoOikeusByKayttoOikeusRyhmaTestDenied() throws Exception {
        this.mvc.perform(get("/kayttooikeusryhma/46/kayttooikeus").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_KAYTTOOIKEUS_KAYTTOOIKEUSRYHMIEN_LUKU")
    public void getKayttoOikeusByKayttoOikeusRyhmaTest() throws Exception {
        given(this.kayttoOikeusService.findPalveluRoolisByKayttoOikeusRyhma(46L))
                .willReturn(singletonList(PalveluRooliDto.builder()
                        .palveluName("palvelunimi")
                        .palveluTexts(new TextGroupListDto().put("FI", "palvelu kuvaus"))
                        .rooli("joku rooli")
                        .rooliTexts(new TextGroupListDto().put("FI", "rooli kuvaus"))
                    .build()));

        this.mvc.perform(get("/kayttooikeusryhma/46/kayttooikeus").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().json(jsonResource("classpath:kayttooikeusryhma/palveluRooli.json")));
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_KOOSTEROOLIENHALLINTA_READ")
    public void createKayttoOikeusRyhmaDeniedTest() throws Exception {
        this.mvc.perform(post("/kayttooikeusryhma").contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(jsonResource("classpath:kayttooikeusryhma/createKayttoOikeusRyhma.json"))
                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_KOOSTEROOLIENHALLINTA_CRUD")
    public void createKayttoOikeusRyhmaTest() throws Exception {
        given(this.kayttoOikeusService.createKayttoOikeusRyhma(any(KayttoOikeusRyhmaModifyDto.class)))
                .willReturn(234L);

        this.mvc.perform(post("/kayttooikeusryhma").contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(jsonResource("classpath:kayttooikeusryhma/createKayttoOikeusRyhma.json"))
                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().string("234"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_HENKILONHALLINTA_READ")
    public void createNewKayttoOikeusDeniedTest() throws Exception {
        this.mvc.perform(post("/kayttooikeusryhma/kayttooikeus").contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(jsonResource("classpath:kayttooikeusryhma/createKayttoOikeus.json"))
                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_KOOSTEROOLIENHALLINTA_CRUD")
    public void createNewKayttoOikeusTest() throws Exception {
        given(this.kayttoOikeusService.createKayttoOikeus(any(KayttoOikeusCreateDto.class)))
                .willReturn(1L);

        given(this.kayttoOikeusService.findKayttoOikeusById(1L))
                .willReturn(KayttoOikeusDto.builder()
                        .id(3423L)
                        .rooli("joku rooli")
                        .textGroup(new TextGroupDto(2L).put("FI", "kuvaus")
                                .put("EN", "ryhmän kuvaus en")
                                .put("SV", "kuvaus sv"))
                        .kayttoOikeusRyhmas(singleton(KayttoOikeusRyhmaDto.builder()
                                .id(22L)
                                .tunniste("kayttooikeusryhmä")
                                .rooliRajoite("roolirajoite")
                                .nimi(new TextGroupDto(3L).put("FI", "ryhmän kuvaus")
                                        .put("SV", "ryhmän kuvaus sv")
                                        .put("EN", "ryhmän kuvaus en"))
                                .organisaatioViite(singletonList(OrganisaatioViiteDto.builder()
                                        .id(44L)
                                        .organisaatioTyyppi("viitteen tyyppi")
                                        .build()))
                                .build()))
                        .palvelu(PalveluDto.builder()
                                .id(7L)
                                .name("palvelun nimi")
                                .description(new TextGroupDto(4L).put("FI", "palvelun kuvaus")
                                        .put("EN", "palvelun kuvaus en")
                                        .put("SV", "palvelun kuvaus sv"))
                                .palveluTyyppi(PalveluTyyppi.KOKOELMA)
                                .kokoelma(PalveluDto.builder()
                                        .id(8L)
                                        .palveluTyyppi(PalveluTyyppi.YKSITTAINEN)
                                        .description(new TextGroupDto(4L).put("FI", "kokoelman kuvaus"))
                                        .name("kuvaus")
                                        .build())
                                .build())
                        .build());

        this.mvc.perform(post("/kayttooikeusryhma/kayttooikeus").contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(jsonResource("classpath:kayttooikeusryhma/createKayttoOikeus.json"))
                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().json(jsonResource("classpath:kayttooikeusryhma/newKayttoOikeus.json")));
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_HENKILONHALLINTA_READ")
    public void updateKayttoOikeusRyhmaTestDenied() throws Exception {
        this.mvc.perform(put("/kayttooikeusryhma/345").contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(jsonResource("classpath:kayttooikeusryhma/updateKayttoOikeusRyhma.json"))
                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_KOOSTEROOLIENHALLINTA_CRUD")
    public void updateKayttoOikeusRyhmaTest() throws Exception {
        given(kayttoOikeusService.findKayttoOikeusRyhma(eq(345L)))
                .willReturn(KayttoOikeusRyhmaDto.builder()
                        .id(345L)
                        .tunniste("kayttooikeusryhmä")
                        .rooliRajoite("roolirajoite")
                        .nimi(new TextGroupDto(3L).put("FI", "ryhmän kuvaus"))
                        .organisaatioViite(singletonList(OrganisaatioViiteDto.builder()
                                .id(44L)
                                .organisaatioTyyppi("viitteen tyyppi")
                                .build()))
                        .build());

        this.mvc.perform(put("/kayttooikeusryhma/345").contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(jsonResource("classpath:kayttooikeusryhma/updateKayttoOikeusRyhma.json"))
                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().json(jsonResource("classpath:kayttooikeusryhma/updatedKayttoOikeusRyhma.json")));
    }

    private MyonnettyKayttoOikeusDto buildKayttoOikeusForHenkilo() {
        return MyonnettyKayttoOikeusDto.builder()
                .ryhmaId(234243L)
                .ryhmaNames(new TextGroupListDto(1L).put("FI", "Ryhmänimi test"))
                .tyyppi("joku tyyppi")
                .voimassaPvm(LocalDate.of(2016, 2, 22))
                .tehtavanimike("joku tehtävä")
                .selected(true)
                .myonnettyTapahtumaId(234234L)
                .alkuPvm(LocalDate.of(2016, 1, 1))
                .kasitelty(LocalDate.of(2016, 1, 1).atStartOfDay())
                .kasittelijaNimi("joku käsittelijä")
                .kasittelijaOid("234.2434.546.234")
                .muutosSyy("testaillaan")
                .organisaatioOid("84384.23832732.234443")
                .removed(false)
                .tila(KayttoOikeudenTila.MYONNETTY)
                .build();
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_KAYTTOOIKEUS_KAYTTOOIKEUSRYHMIEN_LUKU")
    public void getKayttoOikeusRyhmasByKayttoOikeusTest() throws Exception {
        given(kayttoOikeusService.findKayttoOikeusRyhmasByKayttoOikeusList(anyMap()))
                .willReturn(singletonList(KayttoOikeusRyhmaDto.builder()
                        .organisaatioViite(singletonList(OrganisaatioViiteDto.builder()
                                .organisaatioTyyppi("organisaatiotyyppi")
                                .id(44L).build()))
                        .rooliRajoite("roolirajoite")
                        .id(14L)
                        .tunniste("Nimi")
                        .nimi(new TextGroupListDto(1L).put("FI", "Test"))
                        .build()));

        this.mvc.perform(post("/kayttooikeusryhma/ryhmasByKayttooikeus")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content("{\"KOODISTO\": \"CRUD\"}")
                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().json(jsonResource("classpath:kayttooikeusryhma/kayttoOikeusRyhmaList.json")));
    }

    @Test
    public void getKayttoOikeusRyhmasByKayttoOikeusTestDenied() throws Exception {
        this.mvc.perform(post("/kayttooikeusryhma/ryhmasByKayttooikeus")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content("[345]")
                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is3xxRedirection());
    }
}
