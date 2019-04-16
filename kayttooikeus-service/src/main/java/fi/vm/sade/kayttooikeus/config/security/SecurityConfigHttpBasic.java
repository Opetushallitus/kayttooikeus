package fi.vm.sade.kayttooikeus.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableWebSecurity
@Order(1)
@ConditionalOnProperty("kayttooikeus.httpbasic.enabled")
@RequiredArgsConstructor
public class SecurityConfigHttpBasic extends WebSecurityConfigurerAdapter {

    private final Environment environment;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.headers().disable()
                .csrf().disable()
                .antMatcher("/cas/tunnistus")
                .authorizeRequests()
                .anyRequest().hasRole("KAYTTOOIKEUS_TUNNISTUS")
                .and()
                .httpBasic();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser(environment.getRequiredProperty("kayttooikeus.httpbasic.username"))
                .password(String.format("{noop}%s", environment.getRequiredProperty("kayttooikeus.httpbasic.password")))
                .roles("KAYTTOOIKEUS_TUNNISTUS");
    }

}
