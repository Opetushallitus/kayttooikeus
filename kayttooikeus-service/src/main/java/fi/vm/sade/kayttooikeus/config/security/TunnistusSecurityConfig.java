package fi.vm.sade.kayttooikeus.config.security;

import fi.vm.sade.kayttooikeus.NameContainer;
import fi.vm.sade.kayttooikeus.service.dto.OppijaCasTunnistusDto;
import fi.vm.sade.properties.OphProperties;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import fi.vm.sade.kayttooikeus.service.OppijaCasTicketService;
import org.jasig.cas.client.validation.TicketValidationException;
import org.jasig.cas.client.validation.TicketValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.preauth.*;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;


import static java.util.Collections.singletonList;

@Profile("!dev")
@Configuration
@Order(2)
public class TunnistusSecurityConfig extends WebSecurityConfigurerAdapter {


    private static final String KUTSUTTU_ROLE = "APP_KAYTTOOIKEUS_KUTSUTTU";

    private static final String CAS_LOGIN_CLOB= "/cas/login";
    private static final String CAS_TUNNISTUS_CLOB = "/cas/tunnistus";
    public static final String OPPIJA_TICKET_VALIDATOR_QUALIFIER = "oppijaTicketValidator";

    private final OphProperties ophProperties;

    public TunnistusSecurityConfig(OphProperties ophProperties) {
        this.ophProperties = ophProperties;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(hakijaAuthenticationProvider());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.headers().disable().csrf().disable();
        http.antMatcher(CAS_LOGIN_CLOB).antMatcher(CAS_TUNNISTUS_CLOB).authorizeRequests()
                .anyRequest().hasRole(KUTSUTTU_ROLE)
                .and()
                .addFilterBefore(hakijaAuthenticationProcessingFilter(), BasicAuthenticationFilter.class)
                .exceptionHandling()
                .authenticationEntryPoint(hakijaAuthenticationEntryPoint());
    }

    @Bean(OPPIJA_TICKET_VALIDATOR_QUALIFIER)
    public TicketValidator oppijaTicketValidator() {
        Cas20ProxyTicketValidator ticketValidator = new Cas20ProxyTicketValidator(ophProperties.url("cas.oppija.url"));
        // ticketValidator.setAcceptAnyProxy(true);
        return ticketValidator;
    }

    @Bean
    public Filter hakijaAuthenticationProcessingFilter() throws Exception {
        KutsuttuAuthenticationFilter filter = new KutsuttuAuthenticationFilter("/cas/login", oppijaTicketValidator(), ophProperties);
        filter.setAuthenticationManager(authenticationManager());
        String authenticationSuccessUrl = ophProperties.url("kayttooikeus-service.cas.tunnistus");
        filter.setAuthenticationSuccessHandler(new SimpleUrlAuthenticationSuccessHandler(authenticationSuccessUrl));
        return filter;
    }

    @Bean
    public AuthenticationEntryPoint hakijaAuthenticationEntryPoint() {
        String loginCallbackUrl = ophProperties.url("kayttooikeus-service.cas.tunnistus");
        String defaultLoginUrl = ophProperties.url("cas.oppija.login.service", loginCallbackUrl);
        return new AuthenticationEntryPointImpl(defaultLoginUrl, ophProperties, loginCallbackUrl);
    }

    private static class AuthenticationEntryPointImpl extends LoginUrlAuthenticationEntryPoint {

        private final OphProperties properties;
        private final String loginCallbackUrl;

        public AuthenticationEntryPointImpl(String loginFormUrl, OphProperties properties, String loginCallbackUrl) {
            super(loginFormUrl);
            this.properties = properties;
            this.loginCallbackUrl = loginCallbackUrl;
        }

        @Override
        protected String determineUrlToUseForThisRequest(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
            //Locale locale = findSessionAttribute(request, SESSION_ATTRIBUTE_NAME_LOCALE, Locale.class)
            //        .orElse(DEFAULT_LOCALE);
            //String language = locale.getLanguage();

            //TODO Kieli ja kutsutoken ym requestista?
            return properties.url("cas.oppija.login.service", loginCallbackUrl);
        }
    }

    @Bean
    public AuthenticationProvider hakijaAuthenticationProvider() {
        PreAuthenticatedAuthenticationProvider authenticationProvider = new PreAuthenticatedAuthenticationProvider();
        authenticationProvider.setPreAuthenticatedUserDetailsService(new PreAuthenticatedGrantedAuthoritiesUserDetailsService());
        return authenticationProvider;
    }

    private static class KutsuttuAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

        private final TicketValidator oppijaticketValidator;
        private final OphProperties properties;


        public KutsuttuAuthenticationFilter(String defaultFilterProcessesUrl, TicketValidator oppijaticketValidator, OphProperties properties) {
            super(defaultFilterProcessesUrl);
            this.properties = properties;
            this.oppijaticketValidator = oppijaticketValidator;
        }

        @Override
        public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException {
            String ticket = Optional.ofNullable(request.getParameter("ticket"))
                    .orElseThrow(() -> new PreAuthenticatedCredentialsNotFoundException("Unable to authenticate because required param doesn't exist"));
            Map<String, Object> casPrincipalAttributes = null;
            try { //Validate uses Cas:s serviceValidate endpoint and parses the result xml into custom Attributes.
                String kayttooikeusTunnistusUrl = properties.url("kayttooikeus-service.cas.tunnistus");
                casPrincipalAttributes = oppijaticketValidator.validate(ticket, kayttooikeusTunnistusUrl).getPrincipal().getAttributes();
            } catch (TicketValidationException e) {
                throw new AuthenticationCredentialsNotFoundException("Unable to authenticate because required param doesn't exist");
            }
            String nationalIdentificationNumber = Optional.ofNullable((String)casPrincipalAttributes.get("nationalIdentificationNumber"))
                    .orElseThrow(() -> new PreAuthenticatedCredentialsNotFoundException("Unable to authenticate because required param doesn't exist"));
            String surname = Optional.ofNullable((String)casPrincipalAttributes.get("sn"))
                    .orElse("");
            String firstName = Optional.ofNullable((String)casPrincipalAttributes.get("firstName"))
                    .orElse("");

            PreAuthenticatedAuthenticationToken authRequest = new PreAuthenticatedAuthenticationToken(nationalIdentificationNumber, "N/A");
            List<? extends GrantedAuthority> authorities = singletonList(new SimpleGrantedAuthority(String.format("ROLE_%s", "KUTSUTTU_ROOLI"))); // TODO ???
            authRequest.setDetails(new CasOppijaAuthenticationDetails(request, authorities, firstName, surname));
            return getAuthenticationManager().authenticate(authRequest);
        }

    }
    private static class CasOppijaAuthenticationDetails extends PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails implements NameContainer {

        private final String firstName;
        private final String surname;

        public CasOppijaAuthenticationDetails(HttpServletRequest request, Collection<? extends GrantedAuthority> authorities, String firstName, String surname) {
            super(request, authorities);
            this.firstName = firstName;
            this.surname = surname;
        }

        @Override
        public String getFirstName() {
            return firstName;
        }

        @Override
        public String getSurname() {
            return surname;
        }

    }
}
