package fi.vm.sade.kayttooikeus.utiltest;

import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.dto.enumeration.LogInRedirectType;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.util.HenkiloUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

import static fi.vm.sade.kayttooikeus.util.CreateUtil.createHenkilo;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = OrikaBeanMapper.class)
public class HenkiloUtilsTest {

    @Test
    public void loginRedirectTypeVahvastiTunnistautuneelleJaSahkopostitarkistusSuoritettu() {
        Henkilo henkilo = createHenkilo("123");
        boolean isVahvastiTunnistettu = true;
        henkilo.setSahkopostivarmennusAikaleima(LocalDateTime.now());
        LogInRedirectType loginRedirectType = HenkiloUtils.getLoginRedirectType(henkilo, isVahvastiTunnistettu, LocalDateTime.now());
        assertThat(loginRedirectType).isNull();
    }

    @Test
    public void loginRedirectTypeEiVahvastiTunnistautuneelle() {
        Henkilo henkilo = createHenkilo("123");
        boolean isVahvastiTunnistettu = false;
        LogInRedirectType loginRedirectType = HenkiloUtils.getLoginRedirectType(henkilo, isVahvastiTunnistettu, LocalDateTime.now());
        assertThat(loginRedirectType).isEqualTo(LogInRedirectType.STRONG_IDENTIFICATION);
    }

    @Test
    public void loginRedirectTypeKunHenkiloEiOleTehnySahkopostiTarkistusta() {
        Henkilo henkilo = createHenkilo("123");
        boolean isVahvastiTunnistettu = true;
        LogInRedirectType loginRedirectType = HenkiloUtils.getLoginRedirectType(henkilo, isVahvastiTunnistettu, LocalDateTime.now());
        assertThat(loginRedirectType).isEqualTo(LogInRedirectType.EMAIL_VERIFICATION);
    }

    @Test
    public void loginRedirectTypeSahkopostinVarmistusUudestaan() {
        Henkilo henkilo = createHenkilo("123");
        boolean isVahvastiTunnistettu = true;
        henkilo.setSahkopostivarmennusAikaleima(LocalDateTime.now().minusMonths(7));
        LogInRedirectType loginRedirectType = HenkiloUtils.getLoginRedirectType(henkilo, isVahvastiTunnistettu, LocalDateTime.now());
        assertThat(loginRedirectType).isEqualTo(LogInRedirectType.EMAIL_VERIFICATION);
    }

    @Test
    public void loginRedirectTypeSahkopostinVarmistustaEiVielaTehda() {
        Henkilo henkilo = createHenkilo("123");
        boolean isVahvastiTunnistettu = true;
        henkilo.setSahkopostivarmennusAikaleima(LocalDateTime.now().minusMonths(5));
        LogInRedirectType loginRedirectType = HenkiloUtils.getLoginRedirectType(henkilo, isVahvastiTunnistettu, LocalDateTime.now());
        assertThat(loginRedirectType).isNull();
    }


}
