package fi.vm.sade.kayttooikeus.controller;


import fi.vm.sade.kayttooikeus.service.HenkiloService;
import fi.vm.sade.kayttooikeus.service.OrganisaatioHenkiloService;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(HenkiloController.class)
public class HenkiloControllerTest {
    @MockBean
    private HenkiloService service;

    @MockBean
    OrganisaatioHenkiloService organisaatioHenkiloService;

    @Autowired
    private MockMvc mvc;

    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void findUsernameByHenkiloOid() throws Exception {
        given(this.service.getUsernameByOidHenkilo("1.2.3.4.5")).willReturn("username");
        this.mvc.perform(get("/henkilo/1.2.3.4.5/username").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(content().string("\"username\""));
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void findUsernameByHenkiloOidNotFound() throws Exception {
        given(this.service.getUsernameByOidHenkilo("1.2.3.4.5")).willThrow(new NotFoundException("not found"));
        this.mvc.perform(get("/henkilo/1.2.3.4.5/username").accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());
    }


}
