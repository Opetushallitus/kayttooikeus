package fi.vm.sade.kayttooikeus.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;


@EnableWebSecurity
@Profile("dev")
@EnableGlobalMethodSecurity(prePostEnabled = true,
        proxyTargetClass = true, jsr250Enabled = true)
public class SecurityConfigDev extends WebSecurityConfigurerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfigDev.class);

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .headers()
                .disable()
                .csrf()
                .disable()
                .authorizeRequests()
                .antMatchers("/buildversion.txt").permitAll()
                .antMatchers("/swagger-ui.html").permitAll()
                .antMatchers("/cas/auth/token/*").permitAll()
                .antMatchers("/cas/henkilo/*").permitAll()
                .antMatchers("/salasana/unohtunut/*").permitAll()
                .antMatchers("/salasana/resetointi/*").permitAll()
                .antMatchers("/kutsu/token/*").permitAll()
                .antMatchers("/cas/tunnistus").permitAll()
                .antMatchers("/cas/uudelleenrekisterointi").permitAll()
                .antMatchers("/userDetails/*").permitAll()
                .antMatchers("/swagger-resources/**").permitAll()
                .antMatchers("/webjars/springfox-swagger-ui/**").permitAll()
                .antMatchers("/v2/api-docs").permitAll()
                .anyRequest().authenticated()
                .and()
                .httpBasic();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(inMemoryUserDetailsManager());
    }

    private InMemoryUserDetailsManager inMemoryUserDetailsManager() {
        final Properties users = new Properties();
        try {
            String path = Paths.get(System.getProperties().getProperty("user.home"), "/oph-configuration/security-context.properties").toString();
            FileInputStream input = new FileInputStream(path);
            users.load(input);
        } catch (IOException e) {
            logger.error("security-context.properties not found");
        }

        return new InMemoryUserDetailsManager(users);
    }

}
