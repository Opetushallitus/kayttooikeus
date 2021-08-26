package fi.vm.sade.kayttooikeus.controller;


import fi.vm.sade.kayttooikeus.config.security.OphSessionMappingStorage;
import fi.vm.sade.kayttooikeus.dto.IdentifiedHenkiloTypeDto;
import fi.vm.sade.kayttooikeus.dto.KayttajaTyyppi;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotReadDto;
import fi.vm.sade.kayttooikeus.service.IdentificationService;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.TicketValidator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static fi.vm.sade.kayttooikeus.config.security.TunnistusSecurityConfig.OPPIJA_TICKET_VALIDATOR_QUALIFIER;
import static fi.vm.sade.kayttooikeus.config.security.casoppija.SuomiFiAuthenticationProcessingFilter.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class CasControllerTest extends AbstractControllerTest {

    @MockBean
    private IdentificationService identificationService;

    @MockBean
    @Qualifier(OPPIJA_TICKET_VALIDATOR_QUALIFIER)
    private TicketValidator oppijaTicketValidator;

    @MockBean
    private OphSessionMappingStorage sessionStorage;

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA")
    public void generateAuthTokenForHenkiloTest() throws Exception {
        given(this.identificationService.generateAuthTokenForHenkilo("1.2.3.4.9", "somekey", "someidentifier"))
                .willReturn("myrandomtoken");

        this.mvc.perform(get("/cas/auth/oid/1.2.3.4.9")
                        .param("idpid", "someidentifier"))
                .andExpect(status().is5xxServerError());

        this.mvc.perform(get("/cas/auth/oid/1.2.3.4.9")
                        .param("idpkey", "somekey")
                        .param("idpid", "someidentifier"))
                .andExpect(status().isOk())
                .andExpect(content().string("\"myrandomtoken\""));
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA")
    public void getHenkiloOidByIdPAndIdentifierTest() throws Exception {
        given(identificationService.getHenkiloOidByIdpAndIdentifier("somekey", "someidentifier"))
                .willReturn("token");
        this.mvc.perform(get("/cas/auth/idp/somekey").param("idpid", "someidentifier")
                        .param("idpid", "someidentifier"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = "ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA")
    public void getIdentityByAuthTokenTest() throws Exception {
        given(identificationService.findByTokenAndInvalidateToken("mytoken"))
                .willReturn(IdentifiedHenkiloTypeDto.builder()
                        .oidHenkilo("1.2.3.4.5")
                        .henkiloTyyppi(KayttajaTyyppi.VIRKAILIJA)
                        .kayttajatiedot(new KayttajatiedotReadDto("teemuuser"))
                        .build());

        this.mvc.perform(get("/cas/auth/token/mytoken"))
                .andExpect(status().isOk())
                .andExpect(content().json(jsonResource("classpath:cas/identification.json")));
    }

    @Test
    public void invitationHandlingNoSession() throws Exception {
        Assertion assertion = getOppijaAssertionMock();
        when(this.oppijaTicketValidator.validate(eq("oppija-ticket"), anyString())).thenReturn(assertion);

        this.mvc.perform(get("/cas/tunnistus?kutsuToken=kutsuToken&ticket=oppija-ticket"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5", authorities = {"ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA", "ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA_1.2.246.562.10.00000000001"})
    public void invitationHandlingExistingSession() throws Exception {
        Assertion assertion = getOppijaAssertionMock();
        when(this.oppijaTicketValidator.validate(eq("oppija-ticket"), anyString())).thenReturn(assertion);

        this.mvc.perform(get("/cas/tunnistus?kutsuToken=kutsuToken&ticket=oppija-ticket"))
                .andExpect(status().is3xxRedirection());
    }

    private Assertion getOppijaAssertionMock() {
        Assertion assertion = mock(Assertion.class);
        AttributePrincipal principal = mock(AttributePrincipal.class);
        when(assertion.isValid()).thenReturn(true);
        when(assertion.getPrincipal()).thenReturn(principal);
        when(principal.getAttributes()).thenReturn(Map.of(
                HETU_ATTRIBUTE, "123456-7890",
                SUKUNIMI_ATTRIBUTE, "Testi",
                ETUNIMET_ATTRIBUTE, "Testi-Petteri Einari"
        ));
        return assertion;
    }
}
