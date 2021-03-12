package fi.vm.sade.kayttooikeus.config.security.casoppija;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

public class SuomiFiAuthenticationDetails extends PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails {

    public final String hetu;
    public final String sukunimi;
    public final String etunimet;

    public SuomiFiAuthenticationDetails(HttpServletRequest request,
                                        Collection<? extends GrantedAuthority> authorities,
                                        SuomiFiUserDetails details) {
        super(request, authorities);
        this.hetu = details.hetu;
        this.sukunimi = details.sukunimi;
        this.etunimet = details.etunimet;
    }
}
