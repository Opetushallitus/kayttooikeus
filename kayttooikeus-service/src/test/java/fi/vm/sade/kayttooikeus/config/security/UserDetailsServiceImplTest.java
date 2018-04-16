package fi.vm.sade.kayttooikeus.config.security;


import fi.vm.sade.kayttooikeus.dto.KayttajatiedotCreateDto;
import fi.vm.sade.kayttooikeus.repositories.KayttoOikeusRepository;
import fi.vm.sade.kayttooikeus.repositories.PalveluRepository;
import fi.vm.sade.kayttooikeus.service.KayttajatiedotService;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService;
import fi.vm.sade.kayttooikeus.service.it.AbstractServiceIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@RunWith(SpringRunner.class)
public class UserDetailsServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private KayttajatiedotService kayttajatiedotService;
    @Autowired
    private PalveluRepository palveluRepository;
    @Autowired
    private KayttoOikeusRepository kayttoOikeusRepository;

    @Test
    public void usernameNotFoundException() {
        String kayttajatunnus = "kayttajatunnus123";

        Throwable throwable = catchThrowable(() -> userDetailsService.loadUserByUsername(kayttajatunnus));
        assertThat(throwable).isInstanceOf(UsernameNotFoundException.class);

        kayttajatiedotService.create("oid123", new KayttajatiedotCreateDto(kayttajatunnus),
                LdapSynchronizationService.LdapSynchronizationType.NORMAL);
        UserDetails userDetails = userDetailsService.loadUserByUsername(kayttajatunnus);
        assertThat(userDetails).extracting(UserDetails::getUsername).containsExactly("oid123");
        assertThat(userDetails.getAuthorities()).isEmpty();
    }

}
