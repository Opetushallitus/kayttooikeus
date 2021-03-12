package fi.vm.sade.kayttooikeus.config.security.casoppija;

import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;

import static fi.vm.sade.kayttooikeus.config.security.casoppija.SuomiFiAuthenticationProcessingFilter.SUOMI_FI_DETAILS_ATTR_KEY;

public class SuomiFiAuthenticationDetailsSource implements
        AuthenticationDetailsSource<HttpServletRequest, SuomiFiAuthenticationDetails> {

    private static final Collection<SimpleGrantedAuthority> AUTHORITIES = Collections.singleton(
            new SimpleGrantedAuthority("ROLE_TUNNISTUS"));

    @Override
    public SuomiFiAuthenticationDetails buildDetails(HttpServletRequest httpServletRequest) {
        SuomiFiUserDetails details = (SuomiFiUserDetails) httpServletRequest.getAttribute(SUOMI_FI_DETAILS_ATTR_KEY);
        return new SuomiFiAuthenticationDetails(httpServletRequest, AUTHORITIES, details);
    }
}
