package fi.vm.sade.kayttooikeus.config.security;

import fi.vm.sade.kayttooikeus.config.security.casoppija.SuomiFiAuthenticationDetailsSource;
import fi.vm.sade.kayttooikeus.config.security.casoppija.SuomiFiAuthenticationProcessingFilter;
import fi.vm.sade.properties.OphProperties;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.jasig.cas.client.validation.TicketValidator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesUserDetailsService;

@Profile("!dev")
@Configuration
@Order(2)
public class TunnistusSecurityConfig extends WebSecurityConfigurerAdapter {

    public static final String OPPIJA_TICKET_VALIDATOR_QUALIFIER = "oppijaTicketValidator";
    public static final String OPPIJA_CAS_TUNNISTUS_PATH = "/cas/tunnistus";

    private final OphProperties ophProperties;

    public TunnistusSecurityConfig(OphProperties ophProperties) {
        this.ophProperties = ophProperties;
    }

    @Bean
    public SuomiFiAuthenticationDetailsSource suomiFiAuthenticationDetailsSource() {
        return new SuomiFiAuthenticationDetailsSource();
    }

    @Bean(name = OPPIJA_TICKET_VALIDATOR_QUALIFIER)
    public TicketValidator oppijaTicketValidator() {
        return new Cas20ProxyTicketValidator(ophProperties.url("cas.oppija.url"));
    }

    @Bean
    public SuomiFiAuthenticationProcessingFilter suomiFiAuthenticationProcessingFilter(
            @Qualifier(OPPIJA_TICKET_VALIDATOR_QUALIFIER) TicketValidator ticketValidator) throws Exception{
        SuomiFiAuthenticationProcessingFilter filter =
                new SuomiFiAuthenticationProcessingFilter(ticketValidator);
        filter.setAuthenticationManager(authenticationManager());
        filter.setAuthenticationDetailsSource(suomiFiAuthenticationDetailsSource());
        return filter;
    }

    @Bean
    public PreAuthenticatedAuthenticationProvider authenticationProvider() {
        PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService(new PreAuthenticatedGrantedAuthoritiesUserDetailsService());
        return provider;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .antMatcher(OPPIJA_CAS_TUNNISTUS_PATH)
                .headers().disable()
                .csrf().disable()
                .authorizeRequests()
                .anyRequest()
                .permitAll()
            .and()
                .addFilter(suomiFiAuthenticationProcessingFilter(oppijaTicketValidator()));
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(authenticationProvider());
    }

}
