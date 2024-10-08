package fi.vm.sade.kayttooikeus.config;

import ch.qos.logback.access.tomcat.LogbackValve;
import org.apache.catalina.Context;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccessLogConfiguration {

    @Bean
    @ConditionalOnProperty(name = "logback.access")
    public WebServerFactoryCustomizer containerCustomizer() {
        return container -> {
            if (container instanceof TomcatServletWebServerFactory) {
                ((TomcatServletWebServerFactory) container).addContextCustomizers(context -> {
                    LogbackValve logbackValve = new LogbackValve();
                    logbackValve.setFilename("logback-access.xml");
                    context.getPipeline().addValve(logbackValve);
                });
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "feature.accesslog-to-stdout")
    public WebServerFactoryCustomizer containerCustomizerAccessLogToStdout() {
        return container -> {
            if (container instanceof TomcatServletWebServerFactory) {
                ((TomcatServletWebServerFactory) container).addContextCustomizers(context -> {
                    LogbackValve logbackValve = new LogbackValve();
                    logbackValve.setFilename("logback-access-stdout.xml");
                    context.getPipeline().addValve(logbackValve);
                });
            }
        };
    }
}
