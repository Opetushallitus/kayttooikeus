package fi.vm.sade.kayttooikeus.config.security;

import fi.vm.sade.kayttooikeus.config.properties.CasProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;

@Configuration
@Conditional(value = LdapUserDetailsConfig.UseCondition.class)
public class LdapUserDetailsConfig {
    private static final Logger logger = LoggerFactory.getLogger(LdapUserDetailsConfig.class);

    private CasProperties casProperties;

    @Autowired
    public LdapUserDetailsConfig(CasProperties casProperties) {
        this.casProperties = casProperties;
    }


    //
    // LDAP
    //

    @Bean
    public LdapContextSource ldapContextSource() {
        LdapContextSource ldapContextSource = new DefaultSpringSecurityContextSource(casProperties.getLdap().getUrl());
        ldapContextSource.setUserDn(casProperties.getLdap().getManagedDn());
        ldapContextSource.setPassword(casProperties.getLdap().getPassword());
        return ldapContextSource;
    }

    public static class UseCondition implements Condition {
        @Override
        public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
            String mockCas = conditionContext.getEnvironment().getProperty("mock.ldap");
            return !"true".equals(mockCas);
        }
    }

}
