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

    private static final String SUBSYSTEM_CODE = "kayttooikeus";
    public static final String HTTP_CLIENT_OPPIJANUMEROREKISTERI = "httpClientOppijanumerorekisteri";
    public static final String HTTP_CLIENT_VIESTINTA = "httpClientViestinta";

    @Bean
    @Primary
    public OphHttpClient httpClient() {
        return new OphHttpClient.Builder(SUBSYSTEM_CODE).build();
    }

    @Bean(HTTP_CLIENT_OPPIJANUMEROREKISTERI)
    public OphHttpClient httpClientOppijanumerorekisteri(UrlConfiguration properties, ServiceUsersProperties serviceUsersProperties) {
        CasAuthenticator authenticator = new CasAuthenticator.Builder()
                .username(serviceUsersProperties.getOppijanumerorekisteri().getUsername())
                .password(serviceUsersProperties.getOppijanumerorekisteri().getPassword())
                .webCasUrl(properties.url("cas.url"))
                .casServiceUrl(properties.url("oppijanumerorekisteri-service.security-check"))
                .build();
        return new OphHttpClient.Builder(SUBSYSTEM_CODE).authenticator(authenticator).build();
    }

    @Bean(HTTP_CLIENT_VIESTINTA)
    public OphHttpClient httpClientViestinta(UrlConfiguration properties, ServiceUsersProperties serviceUsersProperties) {
        CasAuthenticator authenticator = new CasAuthenticator.Builder()
                .username(serviceUsersProperties.getViestinta().getUsername())
                .password(serviceUsersProperties.getViestinta().getPassword())
                .webCasUrl(properties.url("cas.url"))
                .casServiceUrl(properties.url("ryhmasahkoposti-service.security-check"))
                .build();
        return new OphHttpClient.Builder(SUBSYSTEM_CODE).authenticator(authenticator).build();
    }

}