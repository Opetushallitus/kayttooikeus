package fi.vm.sade.kayttooikeus.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "service-users")
public class ServiceUsersProperties {
    private ServiceUserAccount viestinta;
    private ServiceUserAccount organisaatio;
    private ServiceUserAccount oppijanumerorekisteri;
    
    @Getter
    @Setter
    public static class ServiceUserAccount {
        private String username;
        private String password;
    }
}
