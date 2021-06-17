package fi.vm.sade.kayttooikeus.config.properties;

import fi.vm.sade.properties.OphProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration
public class UrlConfiguration extends OphProperties {

    private static final String DEFAULT_PROTOCOL = "https";

    @Autowired
    public UrlConfiguration(Environment environment) {
        addFiles("/kayttooikeus-service-oph.properties");
        String defaultProtocol = environment.getProperty("host.protocol-default", DEFAULT_PROTOCOL);
        addOverride("host-cas", environment.getRequiredProperty("host.host-cas"));
        addOverride("protocol-cas", environment.getProperty("host.protocol-cas", defaultProtocol));
        addOverride("host-oppija", environment.getRequiredProperty("host.host-oppija"));
        addOverride("protocol-oppija", environment.getProperty("host.protocol-oppija", defaultProtocol));
        addOverride("host-virkailija", environment.getRequiredProperty("host.host-virkailija"));
        addOverride("protocol-virkailija", environment.getProperty("host.protocol-virkailija", defaultProtocol));
        addOverride("host-varda", environment.getRequiredProperty("host.host-varda"));
        addOverride("protocol-varda", environment.getProperty("host.protocol-varda", defaultProtocol));
        if (!StringUtils.isEmpty(environment.getProperty("front.lokalisointi.baseUrl"))) {
            frontProperties.put("lokalisointi.baseUrl", environment.getProperty("front.lokalisointi.baseUrl"));
        }
        if (!StringUtils.isEmpty(environment.getProperty("front.organisaatio.baseUrl"))) {
            frontProperties.put("organisaatio-service.baseUrl", environment.getProperty("front.organisaatio.baseUrl"));
        }
        if (!StringUtils.isEmpty(environment.getProperty("front.koodisto.baseUrl"))) {
            frontProperties.put("koodisto-service.baseUrl", environment.getProperty("front.koodisto.baseUrl"));
        }
    }
}
