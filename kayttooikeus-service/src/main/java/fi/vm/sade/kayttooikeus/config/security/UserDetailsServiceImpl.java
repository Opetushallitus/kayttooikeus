package fi.vm.sade.kayttooikeus.config.security;

import fi.vm.sade.kayttooikeus.dto.KayttooikeusPerustiedotDto;
import fi.vm.sade.kayttooikeus.repositories.KayttajatiedotRepository;
import fi.vm.sade.kayttooikeus.repositories.MyonnettyKayttoOikeusRyhmaTapahtumaRepository;
import fi.vm.sade.kayttooikeus.repositories.criteria.KayttooikeusCriteria;
import fi.vm.sade.kayttooikeus.service.dto.KayttajatiedotDto;
import fi.vm.sade.kayttooikeus.service.exception.DataInconsistencyException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
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
        KayttajatiedotDto kayttajatiedot = kayttajatiedotRepository.findDtoByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(String.format("Käyttäjää ei löytynyt käyttäjätunnuksella %s", username)));

        KayttooikeusCriteria criteria = new KayttooikeusCriteria();
        criteria.setUsername(username);
        List<KayttooikeusPerustiedotDto> kayttooikeudet = myonnettyKayttoOikeusRyhmaTapahtumaRepository.listCurrentKayttooikeusForHenkilo(criteria, null, null);
        if (kayttooikeudet.size() > 1) {
            throw new DataInconsistencyException(String.format("Kysely palautti useamman käyttäjän käyttöoikeuksia (käyttäjätunnus=%s)", username));
        }
        Set<SimpleGrantedAuthority> roolit = kayttooikeudet.stream()
                .flatMap(myonnettyKayttooikeus -> myonnettyKayttooikeus.getOrganisaatiot().stream())
                .flatMap(organisaatio -> organisaatio.getKayttooikeudet().stream()
                        .flatMap(kayttooikeus -> getRoolit(organisaatio.getOrganisaatioOid(), kayttooikeus)))
                .map(String::toUpperCase)
                .map(SimpleGrantedAuthority::new)
                .collect(toSet());

        return new User(kayttajatiedot.getOid(), kayttajatiedot.getSalasana(), roolit);
    }

    private static Stream<String> getRoolit(String organisaatioOid, KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto.KayttooikeusOikeudetDto kayttooikeus) {
        Set<String> roolit = new HashSet<>();

        StringBuilder builder = new StringBuilder("ROLE_APP_");
        // ROLE_APP_<palvelu>
        builder.append(kayttooikeus.getPalvelu());
        roolit.add(builder.toString());

        // ROLE_APP_<palvelu>_<kayttooikeus_rooli>
        builder.append("_");
        builder.append(kayttooikeus.getOikeus());
        roolit.add(builder.toString());

        // ROLE_APP_<palvelu>_<kayttooikeus_rooli>_<organisaatiooid>
        builder.append("_");
        builder.append(organisaatioOid);
        roolit.add(builder.toString());

        return roolit.stream();
    }

}
