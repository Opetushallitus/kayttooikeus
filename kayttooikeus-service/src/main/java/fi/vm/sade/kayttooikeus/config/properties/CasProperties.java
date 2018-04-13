package fi.vm.sade.kayttooikeus.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cas")
public class CasProperties {
    @Getter
    @Setter
    public static class Ldap {
        private String url;
        private String managedDn;
        private String password;
    }
    private Ldap ldap;

    private String service;
    private Boolean sendRenew;
    private String key;
}