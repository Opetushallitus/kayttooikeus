package fi.vm.sade.kayttooikeus.config.security;

import fi.vm.sade.kayttooikeus.config.properties.KayttooikeusProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Order(1)
public class UserDetailsSecurityConfig extends WebSecurityConfigurerAdapter {

    public static final String USER_DETAILS_GET_PATH = "/userDetails/*";
    public static final String USER_DETAILS_POST_PATH = "/userDetails";
    public static final String USER_DETAILS_ROLE = "APP_KAYTTOOIKEUS_PALVELUKAYTTAJA_READ";

    @Autowired
    private KayttooikeusProperties kayttooikeusProperties;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .requestMatchers()
                    .antMatchers(USER_DETAILS_GET_PATH, USER_DETAILS_POST_PATH)
                .and()
                    .csrf().disable()
                    .headers().disable()
                    .authorizeRequests()
                    .anyRequest().authenticated()
                .and()
                    .httpBasic()
                .and()
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.NEVER);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser(kayttooikeusProperties.getUserDetailsAuth().getUsername())
                .password(passwordEncoder().encode(kayttooikeusProperties.getUserDetailsAuth().getPassword()))
                .roles(USER_DETAILS_ROLE);
    }

    @Bean(name = "noOpPasswordEncoder")
    protected PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

}
