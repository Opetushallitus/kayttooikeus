package fi.vm.sade.kayttooikeus.config.security;

import fi.vm.sade.properties.OphProperties;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.jasig.cas.client.validation.TicketValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Profile("!dev")
@Configuration
@Order(1)
public class TunnistusSecurityConfig extends WebSecurityConfigurerAdapter {

    public static final String OPPIJA_TICKET_VALIDATOR_QUALIFIER = "oppijaTicketValidator";
    public static final String OPPIJA_CAS_TUNNISTUS_PATH = "/cas/tunnistus";

    private final OphProperties ophProperties;

    public TunnistusSecurityConfig(OphProperties ophProperties) {
        this.ophProperties = ophProperties;
    }

    @Bean(OPPIJA_TICKET_VALIDATOR_QUALIFIER)
    public TicketValidator oppijaTicketValidator() {
        Cas20ProxyTicketValidator ticketValidator = new Cas20ProxyTicketValidator(ophProperties.url("cas.oppija.url"));
        //ticketValidator.setAcceptAnyProxy(true);
        return ticketValidator;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .antMatcher(OPPIJA_CAS_TUNNISTUS_PATH)
                .headers().disable()
                .csrf().disable()
                .authorizeRequests()
                .anyRequest()
                .permitAll();
    }
}
