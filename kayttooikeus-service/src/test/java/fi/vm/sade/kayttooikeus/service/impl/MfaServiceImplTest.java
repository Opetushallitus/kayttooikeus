package fi.vm.sade.kayttooikeus.service.impl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import fi.vm.sade.kayttooikeus.aspects.HenkiloHelper;
import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.dto.GoogleAuthSetupDto;
import fi.vm.sade.kayttooikeus.dto.MfaProvider;
import fi.vm.sade.kayttooikeus.model.GoogleAuthToken;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;
import fi.vm.sade.kayttooikeus.repositories.GoogleAuthTokenRepository;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.repositories.KayttajatiedotRepository;
import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import fi.vm.sade.kayttooikeus.service.exception.ValidationException;
import fi.vm.sade.kayttooikeus.util.Crypto;

import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MfaServiceImplTest {
    private MfaServiceImpl mfaServiceImpl;

    private SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private QrDataFactory qrDataFactory = new QrDataFactory(HashingAlgorithm.SHA1, 6, 30);
    private QrGenerator qrGenerator = new ZxingPngQrGenerator();

    @Mock
    private CodeVerifier codeVerifier;

    @Mock
    private PermissionCheckerService permissionCheckerService;
    @Mock
    private HenkiloDataRepository henkiloDataRepository;
    @Mock
    private KayttajatiedotRepository kayttajatiedotRepository;
    @Mock
    private GoogleAuthTokenRepository googleAuthTokenRepository;
    @Mock
    private CommonProperties commonProperties;
    @Mock
    private HenkiloHelper henkiloHelper;

    private String secretKey = secretGenerator.generate();
    private Kayttajatiedot kayttajatiedot = Kayttajatiedot.builder().build();
    private Henkilo henkilo = Henkilo.builder().kayttajatiedot(kayttajatiedot).build();
    private String salt = Crypto.getSalt();
    private byte[] iv = Crypto.getIv();
    private String secretKeyCipher;
    private GoogleAuthToken token;

    @Before
    public void setup() throws Exception {
        secretKeyCipher = Crypto.encrypt("password", salt, secretKey, iv);
        token = new GoogleAuthToken(1, henkilo, secretKeyCipher, salt, Base64.getEncoder().encodeToString(iv), null);
        mfaServiceImpl = new MfaServiceImpl(
                secretGenerator,
                qrDataFactory,
                qrGenerator,
                codeVerifier,
                permissionCheckerService,
                henkiloDataRepository,
                kayttajatiedotRepository,
                googleAuthTokenRepository,
                commonProperties,
                henkiloHelper);
    }

    @Test
    public void setupGoogleAuthUsesExistingGauthToken() {
        when(commonProperties.getCryptoPassword()).thenReturn("password");
        when(permissionCheckerService.getCurrentUserOid()).thenReturn("1.2.3.4.5");
        when(kayttajatiedotRepository.findGoogleAuthToken(any())).thenReturn(Optional.of(token));
        when(henkiloDataRepository.findByOidHenkilo(any())).thenReturn(Optional.of(henkilo));

        GoogleAuthSetupDto dto = mfaServiceImpl.setupGoogleAuth();

        verify(googleAuthTokenRepository, times(0)).save(any());
        assertThat(dto.getSecretKey(), equalTo(secretKey));
        assertThat(dto.getQrCodeDataUri(), startsWith("data:image/png;base64,"));
    }

    @Test
    public void setupGoogleAuthCreatesNewGauthToken() {
        when(commonProperties.getCryptoPassword()).thenReturn("password");
        when(permissionCheckerService.getCurrentUserOid()).thenReturn("1.2.3.4.5");
        when(kayttajatiedotRepository.findGoogleAuthToken(any())).thenReturn(Optional.empty());
        when(henkiloDataRepository.findByOidHenkilo(any())).thenReturn(Optional.of(henkilo));

        GoogleAuthSetupDto dto = mfaServiceImpl.setupGoogleAuth();

        verify(googleAuthTokenRepository, times(1)).save(any());
        assertThat(dto.getSecretKey(), matchesPattern("^[A-Z0-9]{32}$"));
        assertThat(dto.getQrCodeDataUri(), startsWith("data:image/png;base64,"));
    }

    @Test
    public void enableGoogleAuthEnablesGoogleAuth() throws Exception {
        when(commonProperties.getCryptoPassword()).thenReturn("password");
        when(permissionCheckerService.getCurrentUserOid()).thenReturn("1.2.3.4.5");
        when(kayttajatiedotRepository.findGoogleAuthToken(any())).thenReturn(Optional.of(token));
        when(henkiloDataRepository.findByOidHenkilo(any())).thenReturn(Optional.of(henkilo));
        when(codeVerifier.isValidCode(any(), any())).thenReturn(true);

        boolean result = mfaServiceImpl.enableGoogleAuth("12345");

        assertEquals(result, true);
        verify(googleAuthTokenRepository, times(1)).save(any());
        verify(kayttajatiedotRepository, times(1)).save(any());
        verify(henkiloHelper, times(1)).logEnableGauthMfa("1.2.3.4.5");
    }

    @Test
    public void enableGoogleAuthThrowsIfInvalidCode() {
        when(commonProperties.getCryptoPassword()).thenReturn("password");
        when(permissionCheckerService.getCurrentUserOid()).thenReturn("1.2.3.4.5");
        when(kayttajatiedotRepository.findGoogleAuthToken(any())).thenReturn(Optional.of(token));
        when(henkiloDataRepository.findByOidHenkilo(any())).thenReturn(Optional.of(henkilo));
        when(codeVerifier.isValidCode(any(), any())).thenReturn(false);

        assertThrows(ValidationException.class, () -> mfaServiceImpl.enableGoogleAuth("123456"));
        verify(googleAuthTokenRepository, times(0)).save(any());
        verify(kayttajatiedotRepository, times(0)).save(any());
    }

    @Test
    public void enableGoogleAuthThrowsIfNoTokenFound() {
        when(permissionCheckerService.getCurrentUserOid()).thenReturn("1.2.3.4.5");
        when(kayttajatiedotRepository.findGoogleAuthToken(any())).thenReturn(Optional.empty());
        when(henkiloDataRepository.findByOidHenkilo(any())).thenReturn(Optional.of(henkilo));

        assertThrows(NoSuchElementException.class, () -> mfaServiceImpl.enableGoogleAuth("123456"));
        verify(googleAuthTokenRepository, times(0)).save(any());
        verify(kayttajatiedotRepository, times(0)).save(any());
    }

    @Test
    public void enableGoogleAuthDoesNothingIfUserHasMfaProvider() {
        Kayttajatiedot kayttajatiedotWithMfaProvider = Kayttajatiedot.builder().mfaProvider(MfaProvider.GAUTH).build();
        Henkilo henkiloWithMfaProvider = Henkilo.builder().kayttajatiedot(kayttajatiedotWithMfaProvider).build();
        when(permissionCheckerService.getCurrentUserOid()).thenReturn("1.2.3.4.5");
        when(kayttajatiedotRepository.findGoogleAuthToken(any())).thenReturn(Optional.of(token));
        when(henkiloDataRepository.findByOidHenkilo(any())).thenReturn(Optional.of(henkiloWithMfaProvider));

        boolean result = mfaServiceImpl.enableGoogleAuth("123456");

        assertEquals(result, false);
        verify(googleAuthTokenRepository, times(0)).save(any());
        verify(kayttajatiedotRepository, times(0)).save(any());
    }

    @Test
    public void enableGoogleAuthDoesNothingIfUserIsRegisteredAlready() {
        GoogleAuthToken tokenWithRegistrationDate = new GoogleAuthToken(1, henkilo, secretKeyCipher, salt,
                new String(iv, StandardCharsets.UTF_8), LocalDateTime.now());
        when(permissionCheckerService.getCurrentUserOid()).thenReturn("1.2.3.4.5");
        when(kayttajatiedotRepository.findGoogleAuthToken(any())).thenReturn(Optional.of(tokenWithRegistrationDate));
        when(henkiloDataRepository.findByOidHenkilo(any())).thenReturn(Optional.of(henkilo));

        boolean result = mfaServiceImpl.enableGoogleAuth("123456");

        assertEquals(result, false);
        verify(googleAuthTokenRepository, times(0)).save(any());
        verify(kayttajatiedotRepository, times(0)).save(any());
    }
}
