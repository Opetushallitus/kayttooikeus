package fi.vm.sade.kayttooikeus.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "common")
public class CommonProperties {
    private String rootOrganizationOid = "1.2.246.562.10.00000000001";
    private String groupOrganizationId;
    private InvitationEmail invitationEmail;
    private String organisaatioRyhmaPrefix = "1.2.246.562.28";
    
    @Getter @Setter
    public static class InvitationEmail {
        private String template;
        private String from;
        private String sender;
    }
}
