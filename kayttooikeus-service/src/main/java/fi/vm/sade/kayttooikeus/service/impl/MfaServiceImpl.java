package fi.vm.sade.kayttooikeus.service.impl;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
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
import fi.vm.sade.kayttooikeus.service.MfaService;
import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import fi.vm.sade.kayttooikeus.service.exception.ValidationException;
import fi.vm.sade.kayttooikeus.util.Crypto;

import java.time.LocalDateTime;
import java.util.Base64;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
@Transactional
@RequiredArgsConstructor
public class MfaServiceImpl implements MfaService {
    private final SecretGenerator secretGenerator;
    private final QrDataFactory qrDataFactory;
    private final QrGenerator qrGenerator;
    private final CodeVerifier verifier;
    private final PermissionCheckerService permissionCheckerService;
    private final HenkiloDataRepository henkiloDataRepository;
    private final KayttajatiedotRepository kayttajatiedotRepository;
    private final GoogleAuthTokenRepository googleAuthTokenRepository;
    private final CommonProperties commonProperties;
    private final HenkiloHelper henkiloHelper;

    private String getNewGoogleAuthSecretKey(Henkilo henkilo) throws Exception {
        String secretKey = secretGenerator.generate();
        byte[] iv = Crypto.getIv();
        String salt = Crypto.getSalt();
        String cipherText = Crypto.encrypt(commonProperties.getCryptoPassword(), salt, secretKey, iv);
        GoogleAuthToken token = new GoogleAuthToken(null, henkilo, cipherText, salt,
                Base64.getEncoder().encodeToString(iv), null);
        googleAuthTokenRepository.save(token);
        return secretKey;
    }

    private Henkilo getCurrentUserOrThrow() {
        String currentUserOid = permissionCheckerService.getCurrentUserOid();
        return henkiloDataRepository.findByOidHenkilo(currentUserOid)
                .orElseThrow(() -> new IllegalStateException(String.format("Käyttäjää %s ei löydy", currentUserOid)));
    }

    @Override
    @Transactional
    public GoogleAuthSetupDto setupGoogleAuth() {
        Henkilo currentUser = getCurrentUserOrThrow();
        String username = currentUser.getKayttajatiedot().getUsername();

        try {
            GoogleAuthToken token = kayttajatiedotRepository.findGoogleAuthToken(username).orElse(null);
            String secretKey;
            if (token != null) {
                secretKey = Crypto.decrypt(commonProperties.getCryptoPassword(), token.getSalt(),
                        token.getSecretKey(), token.getIv());
            } else {
                secretKey = getNewGoogleAuthSecretKey(currentUser);
            }

            QrData data = qrDataFactory.newBuilder()
                    .label("Opintopolku:" + username)
                    .secret(secretKey)
                    .issuer("Opetushallitus")
                    .build();

            String qrCodeDataUri = getDataUriForImage(qrGenerator.generate(data), qrGenerator.getImageMimeType());
            return new GoogleAuthSetupDto(secretKey, qrCodeDataUri);
        } catch (QrGenerationException qre) {
            throw new RuntimeException("Failed to generate QR data", qre);
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup Google Auth", e);
        }
    }

    @Override
    @Transactional
    public boolean enableGoogleAuth(String tokenToVerify) {
        Henkilo currentUser = getCurrentUserOrThrow();
        Kayttajatiedot kayttajatiedot = currentUser.getKayttajatiedot();
        GoogleAuthToken token = kayttajatiedotRepository.findGoogleAuthToken(kayttajatiedot.getUsername())
                .orElseThrow();

        if (kayttajatiedot.getMfaProvider() != null || token.getRegistrationDate() != null) {
            return false;
        }

        String secretKey;
        try {
            secretKey = Crypto.decrypt(commonProperties.getCryptoPassword(), token.getSalt(),
                    token.getSecretKey(), token.getIv());
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt secret key", e);
        }

        if (!verifier.isValidCode(secretKey, tokenToVerify)) {
            throw new ValidationException("Invalid token");
        }

        token.setRegistrationDate(LocalDateTime.now());
        googleAuthTokenRepository.save(token);
        kayttajatiedot.setMfaProvider(MfaProvider.GAUTH);
        kayttajatiedotRepository.save(kayttajatiedot);
        henkiloHelper.logEnableGauthMfa(permissionCheckerService.getCurrentUserOid());

        return true;
    }

    @Override
    @Transactional
    public boolean disableGoogleAuth() {
        Henkilo currentUser = getCurrentUserOrThrow();
        Kayttajatiedot kayttajatiedot = currentUser.getKayttajatiedot();
        GoogleAuthToken token = kayttajatiedotRepository.findGoogleAuthToken(kayttajatiedot.getUsername())
                .orElseThrow();

        kayttajatiedot.setMfaProvider(null);
        kayttajatiedotRepository.save(kayttajatiedot);
        googleAuthTokenRepository.delete(token);
        henkiloHelper.logDisableGauthMfa(permissionCheckerService.getCurrentUserOid());

        return true;
    }
}
