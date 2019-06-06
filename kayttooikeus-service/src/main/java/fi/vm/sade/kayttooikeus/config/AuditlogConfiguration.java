package fi.vm.sade.kayttooikeus.config;

import fi.vm.sade.auditlog.ApplicationType;
import fi.vm.sade.auditlog.Audit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditlogConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditlogConfiguration.class);

    private final String SERVICE_NAME = "kayttooikeus-service";

    @Bean
    public Audit audit() {
        return new Audit(LOGGER::info, SERVICE_NAME, ApplicationType.VIRKAILIJA);
    }

}
