package fi.vm.sade.kayttooikeus.config.security;

import fi.vm.sade.kayttooikeus.dto.OrganisaatioPalveluRooliDto;
import fi.vm.sade.kayttooikeus.repositories.KayttajatiedotRepository;
import fi.vm.sade.kayttooikeus.repositories.MyonnettyKayttoOikeusRyhmaTapahtumaRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

/**
 * {@link UserDetailsService}-toteutus joka muodostaa käyttäjän roolit tietokannasta.
 *
 * Huom! Muodostettavat roolit tulee olla samat kuin mitä LDAPiin tallennetaan
 * ({@link fi.vm.sade.kayttooikeus.service.impl.ldap.LdapRoolitBuilder}).
 */
@Component
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final KayttajatiedotRepository kayttajatiedotRepository;
    private final MyonnettyKayttoOikeusRyhmaTapahtumaRepository myonnettyKayttoOikeusRyhmaTapahtumaRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String oid = kayttajatiedotRepository.findOidByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(String.format("Käyttäjää ei löytynyt käyttäjätunnuksella %s", username)));
        Set<SimpleGrantedAuthority> roolit = myonnettyKayttoOikeusRyhmaTapahtumaRepository.findOrganisaatioPalveluRooliByOid(oid)
                .stream()
                .flatMap(UserDetailsServiceImpl::getRoolit)
                .map(String::toUpperCase)
                .map(SimpleGrantedAuthority::new)
                .collect(toSet());
        return new OphUserDetails(oid, roolit);
    }

    private static Stream<String> getRoolit(OrganisaatioPalveluRooliDto dto) {
        Set<String> roolit = new HashSet<>();

        StringBuilder builder = new StringBuilder("ROLE_APP_");
        // ROLE_APP_<palvelu>
        builder.append(dto.getPalvelu());
        roolit.add(builder.toString());

        // ROLE_APP_<palvelu>_<kayttooikeus_rooli>
        builder.append("_");
        builder.append(dto.getRooli());
        roolit.add(builder.toString());

        // ROLE_APP_<palvelu>_<kayttooikeus_rooli>_<organisaatiooid>
        builder.append("_");
        builder.append(dto.getOrganisaatioOid());
        roolit.add(builder.toString());

        return roolit.stream();
    }

}
