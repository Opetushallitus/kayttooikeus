package fi.vm.sade.kayttooikeus.controller;

import fi.vm.sade.kayttooikeus.service.OrganisaatioHenkiloService;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

import static fi.vm.sade.kayttooikeus.dto.KayttajaTyyppi.VIRKAILIJA;
import static java.util.Collections.singletonList;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class OrganisaatioHenkiloControllerTest extends AbstractControllerTest {
    @MockBean
    private OrganisaatioHenkiloService service;
    @MockBean
    private OrganisaatioClient organisaatioClient;
    
    @Test
    public void listOrganisaatioPerustiedotForCurrentUserIsSecuredTest() throws Exception {
        this.mvc.perform(get("/organisaatiohenkilo/current/organisaatio").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is3xxRedirection()); // redirect to CAS login
    }
    
    @Test
    @WithMockUser(roles = "APP_HENKILONHALLINTA_CRUD")
    public void listPossibleHenkiloTypesByCurrentHenkiloTest() throws Exception {
        given(this.service.listPossibleHenkiloTypesAccessibleForCurrentUser()).willReturn(singletonList(VIRKAILIJA));
        this.mvc.perform(get("/organisaatiohenkilo/current/availablehenkilotype").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(content().json("['VIRKAILIJA']"));
    }

    @Test
    public void listPossibleHenkiloTypesByCurrentHenkiloIsSecuredTest() throws Exception {
        this.mvc.perform(get("/organisaatiohenkilo/current/availablehenkilotype").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is3xxRedirection()); // redirect to CAS login
    }

    @Test
    @WithMockUser(roles = "APP_KAYTTOOIKEUS_CRUD")
    public void findOrCreateOrganisaatioHenkilosTest() throws Exception {
        this.mvc.perform(post("/organisaatiohenkilo/henkiloOid/findOrCreate")
                .content("[]").contentType(MediaType.APPLICATION_JSON_UTF8)
                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "APP_KAYTTOOIKEUS_CRUD")
    public void passivoiHenkiloOrganisationTest() throws Exception {
        this.mvc.perform(delete("/organisaatiohenkilo/henkiloOid/organisaatioOid").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is4xxClientError());
    }
}
