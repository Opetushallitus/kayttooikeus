package fi.vm.sade.kayttooikeus.controller;


import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.service.IdentificationService;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

import static java.util.Collections.singletonList;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class CasControllerTest extends AbstractControllerTest {

    @MockBean
    private IdentificationService identificationService;

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
    public void getIdentityByAuthTokenTest() throws Exception {
        given(identificationService.findByTokenAndInvalidateToken("mytoken"))
                .willReturn(IdentifiedHenkiloTypeDto.builder()
                        .id(44L)
                        .oidHenkilo("1.2.3.4.5")
                        .idpEntityId("idpent")
                        .version(1)
                        .identifier("identif")
                        .henkiloTyyppi(KayttajaTyyppi.VIRKAILIJA)
                        .passivoitu(false)
                        .asiointiKieli(AsiointikieliDto.builder()
                                .kieliKoodi("fi")
                                .kieliTyyppi("suomi")
                                .build())
                        .email("myemail@mail.com")
                        .etunimet("Teemu")
                        .kutsumanimi("Teemu")
                        .sukunimi("Testi")
                        .sukupuoli("MIES")
                        .hetu("11111-1111")
                        .kayttajatiedot(new KayttajatiedotReadDto("teemuuser"))
                        .authorizationData(AuthorizationDataDto.builder()
                                .accessrights(AccessRightListTypeDto.builder()
                                        .accessRight(singletonList(AccessRightTypeDto.builder()
                                                .organisaatioOid("4.5.6.7.8")
                                                .palvelu("KOODISTO")
                                                .rooli("CRUD")
                                                .build()))
                                        .build())
                                .groups(GroupListTypeDto.builder()
                                        .group(singletonList(GroupTypeDto.builder()
                                                .organisaatioOid("4.5.6.7.8")
                                                .nimi("Groupname")
                                                .build()))
                                        .build())
                                .build())
                        .build());

        this.mvc.perform(get("/cas/auth/token/mytoken"))
                .andExpect(status().isOk())
                .andExpect(content().json(jsonResource("classpath:cas/identification.json")));
    }

    @Test
    public void updateIdentificationAndGenerateTokenForHenkiloByHetuTest() throws Exception {
        given(identificationService.updateIdentificationAndGenerateTokenForHenkiloByHetu("11111-madeup"))
                .willThrow(new NotFoundException("henkilo not found"));
        this.mvc.perform(get("/cas/henkilo/11111-madeup"))
                .andExpect(status().is4xxClientError());

        given(identificationService.updateIdentificationAndGenerateTokenForHenkiloByHetu("11111-1111"))
                .willReturn("sometoken");
        this.mvc.perform(get("/cas/henkilo/11111-1111"))
                .andExpect(status().isOk())
                .andExpect(content().string("\"sometoken\""));
    }


}
