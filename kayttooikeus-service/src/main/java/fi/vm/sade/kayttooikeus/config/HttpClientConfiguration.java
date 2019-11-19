package fi.vm.sade.kayttooikeus.config;

import fi.vm.sade.javautils.http.OphHttpClient;
import fi.vm.sade.javautils.http.auth.CasAuthenticator;
import fi.vm.sade.kayttooikeus.config.properties.ServiceUsersProperties;
import fi.vm.sade.kayttooikeus.config.properties.UrlConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class HttpClientConfiguration {

    private static final String CALLER_ID = "1.2.246.562.10.00000000001.kayttooikeus-service.backend";
    public static final String HTTP_CLIENT_OPPIJANUMEROREKISTERI = "httpClientOppijanumerorekisteri";
    public static final String HTTP_CLIENT_ORGANISAATIO = "httpClientOrganisaatio";
    public static final String HTTP_CLIENT_VIESTINTA = "httpClientViestinta";

    @Bean
    @Primary
    public OphHttpClient httpClient() {
        return new OphHttpClient.Builder(CALLER_ID).build();
    }

    @Bean(HTTP_CLIENT_OPPIJANUMEROREKISTERI)
    public OphHttpClient httpClientOppijanumerorekisteri(UrlConfiguration properties, ServiceUsersProperties serviceUsersProperties) {
        CasAuthenticator authenticator = new CasAuthenticator.Builder()
                .username(serviceUsersProperties.getOppijanumerorekisteri().getUsername())
                .password(serviceUsersProperties.getOppijanumerorekisteri().getPassword())
                .webCasUrl(properties.url("cas.url"))
                .casServiceUrl(properties.url("oppijanumerorekisteri-service.security-check"))
                .build();
        return new OphHttpClient.Builder(CALLER_ID).authenticator(authenticator).build();
    }

    @Bean(HTTP_CLIENT_ORGANISAATIO)
    public OphHttpClient httpClientOrganisaatio(UrlConfiguration properties, ServiceUsersProperties serviceUsersProperties) {
        CasAuthenticator authenticator = new CasAuthenticator.Builder()
                .username(serviceUsersProperties.getOrganisaatio().getUsername())
                .password(serviceUsersProperties.getOrganisaatio().getPassword())
                .webCasUrl(properties.url("cas.url"))
                .casServiceUrl(properties.url("organisaatio-service.security-check"))
                .build();
        return new OphHttpClient.Builder(CALLER_ID).authenticator(authenticator).build();
    }

    @Bean(HTTP_CLIENT_VIESTINTA)
    public OphHttpClient httpClientViestinta(UrlConfiguration properties, ServiceUsersProperties serviceUsersProperties) {
        CasAuthenticator authenticator = new CasAuthenticator.Builder()
                .username(serviceUsersProperties.getViestinta().getUsername())
                .password(serviceUsersProperties.getViestinta().getPassword())
                .webCasUrl(properties.url("cas.url"))
                .casServiceUrl(properties.url("ryhmasahkoposti-service.security-check"))
                .build();
        return new OphHttpClient.Builder(CALLER_ID).authenticator(authenticator).build();
    }

}
