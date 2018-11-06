package fi.vm.sade.kayttooikeus.service.it;

import fi.vm.sade.kayttooikeus.dto.KayttajatiedotCreateDto;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotReadDto;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotUpdateDto;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;
import fi.vm.sade.kayttooikeus.repositories.KayttajatiedotRepository;
import fi.vm.sade.kayttooikeus.service.HenkiloService;
import fi.vm.sade.kayttooikeus.service.KayttajatiedotService;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService.LdapSynchronizationType;
import fi.vm.sade.kayttooikeus.service.exception.UnauthorizedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.henkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttajatiedotPopulator.kayttajatiedot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

@RunWith(SpringRunner.class)
public class KayttajatiedotServiceTest extends AbstractServiceIntegrationTest {

    @Autowired
    private KayttajatiedotService kayttajatiedotService;

    @Autowired
    private HenkiloService henkiloService;

    @Autowired
    private KayttajatiedotRepository kayttajatiedotRepository;

    @Test
    @WithMockUser(username = "user1")
    public void createShouldReturn() {
        String oid = "1.2.3.4.5";
        KayttajatiedotCreateDto createDto = new KayttajatiedotCreateDto();
        createDto.setUsername("user1");

        KayttajatiedotReadDto readDto = kayttajatiedotService.create(oid, createDto, LdapSynchronizationType.ASAP);

        assertThat(readDto).isNotNull();
    }

    @Test
    public void createShouldThrowOnDuplicateUsername() {
        String oid = "1.2.3.4.5";
        populate(kayttajatiedot(henkilo("toinen"), "user1"));
        KayttajatiedotCreateDto createDto = new KayttajatiedotCreateDto();
        createDto.setUsername("USER1");

        Throwable throwable = catchThrowable(() -> kayttajatiedotService.create(oid, createDto, LdapSynchronizationType.ASAP));

        assertThat(throwable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("on jo käytössä");
    }

    @Test
    public void updateShouldThrowOnDuplicateUsername() {
        String oid = "1.2.3.4.5";
        populate(henkilo(oid));
        populate(kayttajatiedot(henkilo("toinen"), "user1"));
        KayttajatiedotUpdateDto updateDto = new KayttajatiedotUpdateDto();
        updateDto.setUsername("USER1");

        Throwable throwable = catchThrowable(() -> kayttajatiedotService.updateKayttajatiedot(oid, updateDto));

        assertThat(throwable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Käyttäjänimi on jo käytössä");
    }

    @Test
    public void updateShouldNotThrowIfUsernameNotChanged() {
        String oid = "1.2.3.4.5";
        populate(kayttajatiedot(henkilo(oid), "user1"));
        KayttajatiedotUpdateDto updateDto = new KayttajatiedotUpdateDto();
        updateDto.setUsername("user1");

        KayttajatiedotReadDto readDto = kayttajatiedotService.updateKayttajatiedot(oid, updateDto);

        assertThat(readDto.getUsername()).isEqualTo("user1");
    }

    @Test
    public void updateShouldCreateHenkiloIfMissing() {
        String oid = "1.2.3.4.5";
        KayttajatiedotUpdateDto updateDto = new KayttajatiedotUpdateDto();
        updateDto.setUsername("user1");

        KayttajatiedotReadDto readDto = kayttajatiedotService.updateKayttajatiedot(oid, updateDto);

        assertThat(readDto.getUsername()).isEqualTo("user1");
    }

    @Test
    @WithMockUser(username = "user1")
    public void testValidateUsernamePassword() throws Exception {
        final String henkiloOid = "1.2.246.562.24.27470134096";
        String username = "eetu.esimerkki@geemail.fi";
        String password = "paSsword&23";
        populate(henkilo(henkiloOid));
        populate(kayttajatiedot(henkilo(henkiloOid), username));
        kayttajatiedotService.changePasswordAsAdmin(henkiloOid, password);
        Optional<Kayttajatiedot> kayttajatiedot = this.kayttajatiedotRepository.findByUsername(username);
        assertThat(kayttajatiedot)
                .isNotEmpty()
                .hasValueSatisfying(kayttajatiedot1 -> assertThat(kayttajatiedot1.getPassword()).isNotEmpty());
    }

    @Test
    @WithMockUser(username = "oid1")
    public void getByUsernameAndPassword() {
        // käyttäjää ei löydy
        assertThatThrownBy(() -> kayttajatiedotService.getByUsernameAndPassword("user2", "pass2"))
                .isInstanceOf(UnauthorizedException.class);

        // käyttäjällä ei ole salasanaa
        KayttajatiedotCreateDto createDto = new KayttajatiedotCreateDto("user2");
        kayttajatiedotService.create("oid2", createDto, LdapSynchronizationType.ASAP);
        assertThatThrownBy(() -> kayttajatiedotService.getByUsernameAndPassword("user2", "pass2"))
                .isInstanceOf(UnauthorizedException.class);

        // käyttäjällä on salasana
        kayttajatiedotService.changePasswordAsAdmin("oid2", "IFuRzDC5+aYLSSqE");
        assertThatThrownBy(() -> kayttajatiedotService.getByUsernameAndPassword("user2", "pass2"))
                .isInstanceOf(UnauthorizedException.class);
        KayttajatiedotReadDto readDto = kayttajatiedotService.getByUsernameAndPassword("USER2", "IFuRzDC5+aYLSSqE");
        assertThat(readDto).extracting(KayttajatiedotReadDto::getUsername).containsExactly("user2");

        // käyttäjä on passivoitu
        henkiloService.passivoi("oid2", "oid1");
        assertThatThrownBy(() -> kayttajatiedotService.getByUsernameAndPassword("USER2", "IFuRzDC5+aYLSSqE"))
                .isInstanceOf(UnauthorizedException.class);
    }
}
