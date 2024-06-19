package fi.vm.sade.kayttooikeus.service.it;

import fi.vm.sade.kayttooikeus.dto.CasRedirectParametersResponse;
import fi.vm.sade.kayttooikeus.dto.ChangePasswordRequest;
import fi.vm.sade.kayttooikeus.dto.KayttajaTyyppi;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotCreateDto;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotReadDto;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotUpdateDto;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;
import fi.vm.sade.kayttooikeus.repositories.KayttajatiedotRepository;
import fi.vm.sade.kayttooikeus.service.CryptoService;
import fi.vm.sade.kayttooikeus.service.HenkiloService;
import fi.vm.sade.kayttooikeus.service.KayttajatiedotService;
import fi.vm.sade.kayttooikeus.service.exception.LoginTokenException;
import fi.vm.sade.kayttooikeus.service.exception.LoginTokenNotFoundException;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.exception.PasswordException;
import fi.vm.sade.kayttooikeus.service.exception.UnauthorizedException;
import fi.vm.sade.kayttooikeus.service.exception.ValidationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.Optional;

import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.henkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttajatiedotPopulator.kayttajatiedot;
import static fi.vm.sade.kayttooikeus.repositories.populate.TunnistusTokenPopulator.tunnistusToken;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RunWith(SpringRunner.class)
public class KayttajatiedotServiceTest extends AbstractServiceIntegrationTest {

    private static final String TEST_PASSWORD = "This_is_example_of_strong_password";

    @Autowired
    private KayttajatiedotService kayttajatiedotService;

    @Autowired
    private HenkiloService henkiloService;

    @Autowired
    private KayttajatiedotRepository kayttajatiedotRepository;

    @Autowired
    private CryptoService cryptoService;

    @Test
    @WithMockUser(username = "user1")
    public void createShouldReturn() {
        String oid = "1.2.3.4.5";
        KayttajatiedotCreateDto createDto = new KayttajatiedotCreateDto();
        createDto.setUsername("user1");

        KayttajatiedotReadDto readDto = kayttajatiedotService.create(oid, createDto);

        assertThat(readDto).isNotNull();
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
    public void updateShouldThrowIfHenkiloMissing() {
        String oid = "1.2.3.4.5";
        KayttajatiedotUpdateDto updateDto = new KayttajatiedotUpdateDto();
        updateDto.setUsername("user1");

        Throwable throwable = catchThrowable(() -> kayttajatiedotService.updateKayttajatiedot(oid, updateDto));

        assertThat(throwable).isInstanceOf(NotFoundException.class);
    }

    @Test
    public void updateShouldThrowIfVirkailijaWithoutUsername() {
        String oid = "1.2.3.4.5";
        populate(henkilo(oid).withTyyppi(KayttajaTyyppi.VIRKAILIJA));
        KayttajatiedotUpdateDto updateDto = new KayttajatiedotUpdateDto();
        updateDto.setUsername("user1");

        Throwable throwable = catchThrowable(() -> kayttajatiedotService.updateKayttajatiedot(oid, updateDto));

        assertThat(throwable).isInstanceOf(ValidationException.class);
    }

    @Test
    public void updateShouldReturnIfPalveluWithoutUsername() {
        String oid = "1.2.3.4.5";
        populate(kayttajatiedot(henkilo(oid).withTyyppi(KayttajaTyyppi.PALVELU), "user1"));
        KayttajatiedotUpdateDto updateDto = new KayttajatiedotUpdateDto();
        updateDto.setUsername("user1");

        KayttajatiedotReadDto readDto = kayttajatiedotService.updateKayttajatiedot(oid, updateDto);

        assertThat(readDto.getUsername()).isEqualTo("user1");
    }

    @Test
    @WithMockUser(username = "user1")
    public void testValidateUsernamePassword() {
        final String henkiloOid = "1.2.246.562.24.27470134096";
        String username = "eetu.esimerkki@geemail.fi";
        populate(henkilo(henkiloOid));
        populate(kayttajatiedot(henkilo(henkiloOid), username, null));
        kayttajatiedotService.changePasswordAsAdmin(henkiloOid, TEST_PASSWORD);
        Optional<Kayttajatiedot> kayttajatiedot = this.kayttajatiedotRepository.findByUsername(username);
        assertThat(kayttajatiedot)
                .isNotEmpty()
                .hasValueSatisfying(kayttajatiedot1 -> assertThat(kayttajatiedot1.getPassword()).isNotEmpty());
    }

    @Test
    @WithMockUser(username = "oid1")
    public void getByUsernameAndPassword() {
        populate(henkilo("oid1"));

        // käyttäjää ei löydy
        assertThatThrownBy(() -> kayttajatiedotService.getByUsernameAndPassword("user2", "pass2"))
                .isInstanceOf(UnauthorizedException.class);

        // käyttäjällä ei ole salasanaa
        KayttajatiedotCreateDto createDto = new KayttajatiedotCreateDto("user2");
        kayttajatiedotService.create("oid2", createDto);
        assertThatThrownBy(() -> kayttajatiedotService.getByUsernameAndPassword("user2", "pass2"))
                .isInstanceOf(UnauthorizedException.class);

        // käyttäjällä on salasana
        kayttajatiedotService.changePasswordAsAdmin("oid2", TEST_PASSWORD);
        assertThatThrownBy(() -> kayttajatiedotService.getByUsernameAndPassword("user2", "pass2"))
                .isInstanceOf(UnauthorizedException.class);
        Kayttajatiedot readDto = kayttajatiedotService.getByUsernameAndPassword("USER2", TEST_PASSWORD);
        assertThat(readDto).extracting(Kayttajatiedot::getUsername).isEqualTo("user2");

        // käyttäjä on passivoitu
        henkiloService.passivoi("oid2", "oid1");
        assertThatThrownBy(() -> kayttajatiedotService.getByUsernameAndPassword("USER2", TEST_PASSWORD))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @WithMockUser(username = "counterTest")
    public void countSuccessfullLogins() {
        populate(henkilo("counterTest"));
        populate(kayttajatiedot(henkilo("counterTest"), "counterTest", null));
        kayttajatiedotService.changePasswordAsAdmin("counterTest", TEST_PASSWORD);

        Kayttajatiedot userDetails = kayttajatiedotRepository.findByUsername("counterTest").orElseThrow();
        assertThat(userDetails.getLoginCounter()).isNull();

        kayttajatiedotService.getByUsernameAndPassword("counterTest", TEST_PASSWORD);
        kayttajatiedotService.getByUsernameAndPassword("counterTest", TEST_PASSWORD);

        userDetails = kayttajatiedotRepository.findByUsername("counterTest").orElseThrow();
        assertThat(userDetails.getLoginCounter()).isNotNull();
        assertThat(userDetails.getLoginCounter().getCount()).isEqualTo(2);
    }

    @Test
    @WithMockUser(username = "changePasswordWeak")
    public void changePasswordFailsOnWeakPassword() {
        populate(henkilo("changePasswordWeak"));
        populate(kayttajatiedot(henkilo("changePasswordWeak"), "changePasswordWeak", null));
        kayttajatiedotService.changePasswordAsAdmin("changePasswordWeak", TEST_PASSWORD);

        assertThrows(PasswordException.class, () -> kayttajatiedotService.changePassword(
            new ChangePasswordRequest("token", TEST_PASSWORD, "newPassword")));
    }

    @Test
    @WithMockUser(username = "changePasswordInvalid")
    public void changePasswordFailsOnInvalidLoginToken() {
        populate(henkilo("changePasswordInvalid"));
        populate(kayttajatiedot(henkilo("changePasswordInvalid"), "changePasswordInvalid", null));
        kayttajatiedotService.changePasswordAsAdmin("changePasswordInvalid", TEST_PASSWORD);

        assertThrows(LoginTokenNotFoundException.class, () -> kayttajatiedotService.changePassword(
            new ChangePasswordRequest("token", TEST_PASSWORD, TEST_PASSWORD + "123!")));
    }

    @Test
    @WithMockUser(username = "changePasswordUsed")
    public void changePasswordFailsOnUsedLoginToken() {
        populate(henkilo("changePasswordUsed"));
        populate(kayttajatiedot(henkilo("changePasswordUsed"), "changePasswordUsed", null));
        populate(tunnistusToken(henkilo("changePasswordUsed"))
                .loginToken("loginToken123")
                .aikaleima(LocalDateTime.now())
                .kaytetty(LocalDateTime.now()));
        kayttajatiedotService.changePasswordAsAdmin("changePasswordUsed", TEST_PASSWORD);

        assertThrows(LoginTokenNotFoundException.class, () -> kayttajatiedotService.changePassword(
            new ChangePasswordRequest("loginToken123", TEST_PASSWORD, TEST_PASSWORD + "123!")));
    }

    @Test
    @WithMockUser(username = "changePasswordMatch")
    public void changePasswordFailsWhenCurrentPasswordDoesntMatch() {
        populate(henkilo("changePasswordMatch"));
        populate(kayttajatiedot(henkilo("changePasswordMatch"), "changePasswordMatch", null));
        populate(tunnistusToken(henkilo("changePasswordMatch"))
                .loginToken("loginToken123")
                .aikaleima(LocalDateTime.now()));
        kayttajatiedotService.changePasswordAsAdmin("changePasswordMatch", TEST_PASSWORD);

        assertThrows(LoginTokenException.class, () -> kayttajatiedotService.changePassword(
            new ChangePasswordRequest("loginToken123", TEST_PASSWORD + "123!", TEST_PASSWORD + "1")));
    }

    @Test
    @WithMockUser(username = "changePasswordSuccess")
    public void changePasswordHappyPath() {
        populate(henkilo("changePasswordSuccess"));
        populate(kayttajatiedot(henkilo("changePasswordSuccess"), "changePasswordSuccess", null));
        populate(tunnistusToken(henkilo("changePasswordSuccess"))
                .loginToken("loginToken123")
                .aikaleima(LocalDateTime.now()));
        kayttajatiedotService.changePasswordAsAdmin("changePasswordSuccess", TEST_PASSWORD);

        String newPassword = TEST_PASSWORD + "123!";
        CasRedirectParametersResponse response = kayttajatiedotService.changePassword(
            new ChangePasswordRequest("loginToken123", TEST_PASSWORD, newPassword));
        assertThat(response.getAuthToken()).isNotNull();
        assertThat(response.getService()).endsWith("virkailijan-tyopoyta");

        Kayttajatiedot kayttajatiedot = kayttajatiedotRepository.findByUsername("changePasswordSuccess").orElseThrow();
        assertThat(cryptoService.check(newPassword, kayttajatiedot.getPassword(), kayttajatiedot.getSalt())).isTrue();
        assertThat(kayttajatiedot.getPasswordChange()).isAfter(LocalDateTime.now().minusMinutes(1));
    }
}
