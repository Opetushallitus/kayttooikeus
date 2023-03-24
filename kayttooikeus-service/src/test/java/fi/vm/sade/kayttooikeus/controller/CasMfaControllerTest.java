package fi.vm.sade.kayttooikeus.controller;

import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.model.GoogleAuthToken;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;
import fi.vm.sade.kayttooikeus.service.KayttajatiedotService;
import fi.vm.sade.kayttooikeus.util.Crypto;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Base64;
import java.util.Optional;

@RunWith(SpringRunner.class)
public class CasMfaControllerTest extends AbstractControllerTest {
    @MockBean
    private KayttajatiedotService kayttajatiedotService;
    @MockBean
    private CommonProperties commonProperties;

    private String username = "cas";
    private String password = "mfa";

    private String secretKey = "SU4XHOHKQERMGQTLPDZOYXD7TGLPQLMD";
    private Kayttajatiedot kayttajatiedot = Kayttajatiedot.builder().build();
    private Henkilo henkilo = Henkilo.builder().kayttajatiedot(kayttajatiedot).build();
    private String salt = Crypto.getSalt();
    private byte[] iv = Crypto.getIv();
    private String secretKeyCipher;
    private GoogleAuthToken token;

    @Before
    public void setup() throws Exception {
        secretKeyCipher = Crypto.encrypt("password", salt, secretKey, iv);
        token = new GoogleAuthToken(1, henkilo, secretKeyCipher, salt, Base64.getEncoder().encodeToString(iv),
                LocalDateTime.of(1979, Month.APRIL, 2, 3, 4, 0, 0));
    }

    @Test
    public void getMfaTriggerRequiresBasicAuth() throws Exception {
        when(kayttajatiedotService.getMfaProviderAndConsumeBypass(any())).thenReturn("mfa-gauth");
        mvc.perform(post("/mfa/trigger")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"principalId\":\"username\",\"serviceId\":\"service\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void getMfaTriggerReturnsMfaProvider() throws Exception {
        when(kayttajatiedotService.getMfaProviderAndConsumeBypass(any())).thenReturn("mfa-gauth");
        mvc.perform(post("/mfa/trigger")
            .with(httpBasic(username, password))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"principalId\":\"username\",\"serviceId\":\"service\"}"))
            .andExpect(status().isOk())
            .andExpect(content().string("mfa-gauth"));
    }

    @Test
    public void getMfaTriggerReturnsEmptyStringIfNoMfaProvider() throws Exception {
        when(kayttajatiedotService.getMfaProviderAndConsumeBypass(any())).thenReturn("");
        mvc.perform(post("/mfa/trigger")
            .with(httpBasic(username, password))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"principalId\":\"username\",\"serviceId\":\"service\"}"))
            .andExpect(status().isOk())
            .andExpect(content().string(""));
    }

    @Test
    public void getGoogleAuthTokenRequiresBasicAuth() throws Exception {
        when(kayttajatiedotService.getGoogleAuthToken(any())).thenReturn(Optional.empty());
        mvc.perform(get("/mfa/token")
            .header("username", "username")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void getGoogleAuthTokenReturns404WhenTokenNotFound() throws Exception {
        when(kayttajatiedotService.getGoogleAuthToken(any())).thenReturn(Optional.empty());
        mvc.perform(get("/mfa/token")
            .with(httpBasic(username, password))
            .header("username", "username")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void getGoogleAuthTokenReturnsToken() throws Exception {
        when(commonProperties.getCryptoPassword()).thenReturn("password");
        when(kayttajatiedotService.getGoogleAuthToken(any())).thenReturn(Optional.of(token));
        mvc.perform(get("/mfa/token")
            .with(httpBasic(username, password))
            .header("username", "username")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpectAll(
                status().isOk(),
                jsonPath("$.[0]").value("java.util.ArrayList"),
                jsonPath("$.[1].[0].id").value(1),
                jsonPath("$.[1].[0].name").value("device"),
                jsonPath("$.[1].[0].scratchCodes.[0]").value("java.util.ArrayList"),
                jsonPath("$.[1].[0].scratchCodes.[1]").value(empty()),
                jsonPath("$.[1].[0].secretKey").value(matchesPattern("^[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]+$")),
                jsonPath("$.[1].[0].validationCode").value(0),
                jsonPath("$.[1].[0].username").value("username"),
                jsonPath("$.[1].[0].@class").value("org.apereo.cas.gauth.credential.GoogleAuthenticatorAccount"),
                jsonPath("$.[1].[0].registrationDate").value("1979-04-02T03:04Z")
            );
    }
}
