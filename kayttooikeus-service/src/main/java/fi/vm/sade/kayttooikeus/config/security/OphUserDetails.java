package fi.vm.sade.kayttooikeus.config.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
@AllArgsConstructor
public class OphUserDetails implements UserDetails {

    private final String oid;
    private final String username;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;

    public OphUserDetails(String username, Collection<? extends GrantedAuthority> authorities) {
        this(username, username, authorities, true, true, true, true);
    }

    @Override
    public String getPassword() {
        // Instanssia käytetään vain roolien lataamiseen (CasAuthenticationProvider) joten salasanaa ei tarvita
        return null;
    }

}

