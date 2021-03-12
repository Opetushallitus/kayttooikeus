package fi.vm.sade.kayttooikeus.config.security;

import fi.vm.sade.kayttooikeus.NameContainer;
import fi.vm.sade.kayttooikeus.service.VahvaTunnistusService;
import fi.vm.sade.properties.OphProperties;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.jasig.cas.client.validation.TicketValidationException;
import org.jasig.cas.client.validation.TicketValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationFilter;
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
import org.springframework.util.StringUtils;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;


import static java.util.Collections.singletonList;

@Profile("!dev")
@Configuration
@Order(1)
public class TunnistusSecurityConfig extends WebSecurityConfigurerAdapter {


    private static final String KUTSUTTU_ROLE = "APP_KUTSUTTU";

    private static final String KUTSU_CLOB = "/kutsuttu/validate";
    public static final String OPPIJA_TICKET_VALIDATOR_QUALIFIER = "oppijaTicketValidator";

    private final OphProperties ophProperties;
    private final VahvaTunnistusService vahvaTunnistusService;


    public TunnistusSecurityConfig(OphProperties ophProperties, VahvaTunnistusService vahvaTunnistusService) {
        this.ophProperties = ophProperties;
        this.vahvaTunnistusService = vahvaTunnistusService;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(hakijaAuthenticationProvider());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .antMatcher(KUTSU_CLOB)
                .headers().disable()
                .csrf().disable()
                .authorizeRequests()
                .anyRequest()
                .permitAll()
                .and()
                .addFilterBefore(oppijaAuthenticationProcessingFilter(), CasAuthenticationFilter.class);
                //.exceptionHandling()
                //.authenticationEntryPoint(oppijaAuthenticationEntryPoint());
    }

    @Bean(OPPIJA_TICKET_VALIDATOR_QUALIFIER)
    public TicketValidator oppijaTicketValidator() {
        Cas20ProxyTicketValidator ticketValidator = new Cas20ProxyTicketValidator(ophProperties.url("cas.oppija.url"));
        // ticketValidator.setAcceptAnyProxy(true);
        return ticketValidator;
    }

    @Bean
    public Filter oppijaAuthenticationProcessingFilter() throws Exception {
        SimpleUrlAuthenticationSuccessHandler successHandler = oppijaSuccesUrlHandler();
        KutsuttuAuthenticationFilter filter = new KutsuttuAuthenticationFilter("/kutsuttu/validate", oppijaTicketValidator(), ophProperties, successHandler, vahvaTunnistusService);
        filter.setAuthenticationManager(authenticationManager());
        filter.setAuthenticationSuccessHandler(successHandler);
        return filter;
    }

    @Bean
    public SimpleUrlAuthenticationSuccessHandler oppijaSuccesUrlHandler() throws Exception {
        String authenticationSuccessUrl = ophProperties.url("kayttooikeus-service.cas.success");
        return new SimpleUrlAuthenticationSuccessHandler(authenticationSuccessUrl);
    }

    @Bean
    public AuthenticationEntryPoint oppijaAuthenticationEntryPoint() {
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
            String locale = request.getParameter("locale");
            String kutsuToken = request.getParameter("kutsuToken");
            String loginToken = request.getParameter("loginToken");
            String loppuOsa = "";

            if (StringUtils.hasLength(kutsuToken)) {
                loppuOsa = "&kutsuToken=" + kutsuToken;
            } else if (StringUtils.hasLength(loginToken)) {
                loppuOsa = "&loginToken=" + loginToken;
            }
            return properties.url("cas.oppija.login.service", (loginCallbackUrl + "?locale=" + locale + loppuOsa));
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
        private final SimpleUrlAuthenticationSuccessHandler successHandler;
        private final VahvaTunnistusService vahvaTunnistusService;



        public KutsuttuAuthenticationFilter(String defaultFilterProcessesUrl, TicketValidator oppijaticketValidator, OphProperties properties, SimpleUrlAuthenticationSuccessHandler successHandler, VahvaTunnistusService vahvaTunnistusService) {
            super(defaultFilterProcessesUrl);
            this.successHandler = successHandler;
            this.properties = properties;
            this.oppijaticketValidator = oppijaticketValidator;
            this.vahvaTunnistusService = vahvaTunnistusService;
        }

        private String getVahvaTunnistusRedirectUrl(String loginToken, String kielisyys, String hetu) {
            try {
                return vahvaTunnistusService.kirjaaVahvaTunnistus(loginToken, kielisyys, hetu);
            } catch (Exception e) {
                return properties.url("henkilo-ui.vahvatunnistus.virhe", kielisyys, loginToken);
            }
        }

        @Override
        public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException {
            String locale = request.getParameter("locale");
            String kutsuToken = request.getParameter("kutsuToken");
            String loginToken = request.getParameter("loginToken");
            String ticket = Optional.ofNullable(request.getParameter("ticket"))
                    .orElseThrow(() -> new PreAuthenticatedCredentialsNotFoundException("Unable to authenticate because required param doesn't exist"));
            Map<String, Object> casPrincipalAttributes = null;
            try { //Validate uses Cas:s serviceValidate endpoint and parses the result xml into custom Attributes.
                String kayttooikeusTunnistusBaseUrl = properties.url("kayttooikeus-service.cas.tunnistus");
                String loppuOsa = "";
                if (StringUtils.hasLength(kutsuToken)) {
                    loppuOsa = "&kutsuToken=" + kutsuToken;
                } else if (StringUtils.hasLength(loginToken)) {
                    loppuOsa = "&loginToken=" + loginToken;
                }
                casPrincipalAttributes = oppijaticketValidator.validate(ticket, (kayttooikeusTunnistusBaseUrl + "?locale=" + locale + loppuOsa)).getPrincipal().getAttributes();
            } catch (TicketValidationException e) {
                throw new AuthenticationCredentialsNotFoundException("Unable to authenticate because required param doesn't exist");
            }
            String nationalIdentificationNumber = Optional.ofNullable((String)casPrincipalAttributes.get("nationalIdentificationNumber"))
                    .orElseThrow(() -> new PreAuthenticatedCredentialsNotFoundException("Unable to authenticate because required param doesn't exist"));
            String surname = Optional.ofNullable((String)casPrincipalAttributes.get("sn"))
                    .orElse("");
            String firstName = Optional.ofNullable((String)casPrincipalAttributes.get("firstName"))
                    .orElse("");

            if (StringUtils.hasLength(kutsuToken)) {
                successHandler.setDefaultTargetUrl(vahvaTunnistusService.kasitteleKutsunTunnistus(
                        kutsuToken, locale, nationalIdentificationNumber,
                        firstName, surname));
            } else if (StringUtils.hasLength(loginToken)) {
                // Kirjataan henkilön vahva tunnistautuminen järjestelmään, vaihe 1
                // Joko päästetään suoraan sisään tai käytetään lisätietojen keräyssivun kautta
                successHandler.setDefaultTargetUrl(getVahvaTunnistusRedirectUrl(loginToken, locale, nationalIdentificationNumber));
            } else {
                successHandler.setDefaultTargetUrl(vahvaTunnistusService.kirjaaKayttajaVahvallaTunnistuksella(nationalIdentificationNumber, locale));
            }

            PreAuthenticatedAuthenticationToken authRequest = new PreAuthenticatedAuthenticationToken(nationalIdentificationNumber, "N/A");
            List<? extends GrantedAuthority> authorities = singletonList(new SimpleGrantedAuthority(String.format("ROLE_%s", KUTSUTTU_ROLE))); // TODO ???
            authRequest.setDetails(new CasOppijaAuthenticationDetails(request, authorities, firstName, surname, nationalIdentificationNumber));
            return getAuthenticationManager().authenticate(authRequest);
        }

    }
    private static class CasOppijaAuthenticationDetails extends PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails implements NameContainer {

        private final String firstName;
        private final String surname;
        private final String hetu;

        public CasOppijaAuthenticationDetails(HttpServletRequest request, Collection<? extends GrantedAuthority> authorities, String firstName, String surname, String hetu) {
            super(request, authorities);
            this.firstName = firstName;
            this.surname = surname;
            this.hetu = hetu;
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
