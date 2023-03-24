package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.model.GoogleAuthToken;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.repositories.KayttajatiedotRepository;
import fi.vm.sade.kayttooikeus.service.CryptoService;
import fi.vm.sade.kayttooikeus.service.KayttajatiedotService;
import fi.vm.sade.kayttooikeus.service.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KayttajatiedotServiceImpl implements KayttajatiedotService {

    private final KayttajatiedotRepository kayttajatiedotRepository;
    private final HenkiloDataRepository henkiloDataRepository;
    private final OrikaBeanMapper mapper;
    private final CryptoService cryptoService;

    private final int MFA_BYPASS_MAX_AGE_SECONDS = 30;

    @Override
    @Transactional
    public KayttajatiedotReadDto create(String henkiloOid, KayttajatiedotCreateDto createDto) {
        return henkiloDataRepository.findByOidHenkilo(henkiloOid)
                .map(henkilo -> create(henkilo, createDto))
                .orElseGet(() -> create(new Henkilo(henkiloOid), createDto));
    }

    public KayttajatiedotReadDto create(Henkilo henkilo, KayttajatiedotCreateDto createDto) {
        if (henkilo.getKayttajatiedot() != null) {
            throw new IllegalArgumentException("Käyttäjätiedot on jo luotu henkilölle");
        }

        Kayttajatiedot kayttajatiedot = mapper.map(createDto, Kayttajatiedot.class);
        return saveKayttajatiedot(henkilo, kayttajatiedot);
    }

    private boolean isUsernameUnique(String username, Optional<String> henkiloOid) {
        return henkiloDataRepository.findByKayttajatiedotUsername(username)
                .map(henkiloByUsername -> henkiloOid.map(henkiloByUsername.getOidHenkilo()::equals)
                        .orElse(false))
                .orElse(true);
    }

    @Override
    @Transactional
    public void createOrUpdateUsername(String oidHenkilo, String username) {
        if (StringUtils.hasLength(username)) {
            Optional<Kayttajatiedot> kayttajatiedot = this.kayttajatiedotRepository.findByHenkiloOidHenkilo(oidHenkilo);
            if (kayttajatiedot.isPresent()) {
                kayttajatiedot.get().setUsername(username);
            } else {
                this.create(oidHenkilo, new KayttajatiedotCreateDto(username));
            }
        } else {
            log.warn("Tried to create or update empty username.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Kayttajatiedot> getKayttajatiedotByOidHenkilo(String oidHenkilo) {
        return this.kayttajatiedotRepository.findByHenkiloOidHenkilo(oidHenkilo);
    }

    @Override
    @Transactional(readOnly = true)
    public KayttajatiedotReadDto getByHenkiloOid(String henkiloOid) {
        return kayttajatiedotRepository.findByHenkiloOid(henkiloOid)
                .orElseThrow(() -> new NotFoundException("Käyttäjätietoja ei löytynyt OID:lla " + henkiloOid));
    }

    @Override
    @Transactional
    public KayttajatiedotReadDto updateKayttajatiedot(String henkiloOid,
            KayttajatiedotUpdateDto kayttajatiedotUpdateDto) {
        return henkiloDataRepository.findByOidHenkilo(henkiloOid)
                .map(henkilo -> updateKayttajatiedot(henkilo, kayttajatiedotUpdateDto))
                .orElseThrow(
                        () -> new NotFoundException(String.format("Käyttäjää ei löytynyt OID:lla %s", henkiloOid)));
    }

    private KayttajatiedotReadDto updateKayttajatiedot(Henkilo henkilo,
            KayttajatiedotUpdateDto kayttajatiedotUpdateDto) {
        Kayttajatiedot kayttajatiedot = Optional.ofNullable(henkilo.getKayttajatiedot()).orElseGet(() -> {
            if (!KayttajaTyyppi.PALVELU.equals(henkilo.getKayttajaTyyppi())) {
                throw new ValidationException("Vain palvelukäyttäjälle voi lisätä käyttäjätunnuksen");
            }
            return new Kayttajatiedot();
        });
        mapper.map(kayttajatiedotUpdateDto, kayttajatiedot);
        return saveKayttajatiedot(henkilo, kayttajatiedot);
    }

    private KayttajatiedotReadDto saveKayttajatiedot(Henkilo henkilo, Kayttajatiedot kayttajatiedot) {
        kayttajatiedot.setHenkilo(henkilo);
        henkilo.setKayttajatiedot(kayttajatiedot);
        henkilo = henkiloDataRepository.save(henkilo);
        return mapper.map(henkilo.getKayttajatiedot(), KayttajatiedotReadDto.class);
    }

    @Override
    @Transactional
    public void changePasswordAsAdmin(String oid, String newPassword) {
        this.cryptoService.throwIfNotStrongPassword(newPassword);
        this.changePassword(oid, newPassword);
    }

    @Override
    @Transactional(readOnly = true)
    public void throwIfUsernameExists(String username) {
        throwIfUsernameExists(username, Optional.empty());
    }

    @Override
    public void throwIfUsernameExists(String username, Optional<String> henkiloOid) {
        if (!isUsernameUnique(username, henkiloOid)) {
            throw new UsernameAlreadyExistsException(String.format("Username %s already exists", username));
        }
    }

    @Override
    public void throwIfUsernameIsNotValid(String username) {
        if (!username.matches(Constants.USERNAME_REGEXP)) {
            throw new IllegalArgumentException("Username is not valid with pattern " + Constants.USERNAME_REGEXP);
        }
    }

    @Override
    public void throwIfOldPassword(String oidHenkilo, String newPassword) {
        Kayttajatiedot kayttajatiedot = this.kayttajatiedotRepository.findByHenkiloOidHenkilo(oidHenkilo)
                .orElseThrow(() -> new ValidationException("Käyttäjätunnus on asetettava ennen salasanaa"));

        if (this.cryptoService.check(newPassword, kayttajatiedot.getPassword(), kayttajatiedot.getSalt())) {
            throw new PasswordException("Salasana on jo käytössä");
        }
    }

    @Override
    @Transactional
    public KayttajatiedotReadDto getByUsernameAndPassword(String username, String password) {
        return kayttajatiedotRepository.findByUsername(username)
                .filter(entity -> cryptoService.check(password, entity.getPassword(), entity.getSalt()))
                .map(userDetails -> {
                    userDetails.incrementLoginCount();
                    return mapper.map(userDetails, KayttajatiedotReadDto.class);
                })
                .orElseThrow(UnauthorizedException::new);
    }

    private void changePassword(String oid, String newPassword) {
        setPasswordForHenkilo(oid, newPassword);
    }

    private void setPasswordForHenkilo(String oidHenkilo, String password) {
        Kayttajatiedot kayttajatiedot = this.kayttajatiedotRepository.findByHenkiloOidHenkilo(oidHenkilo)
                .orElseThrow(() -> new ValidationException("Käyttäjätunnus on asetettava ennen salasanaa"));
        String salt = this.cryptoService.generateSalt();
        String hash = this.cryptoService.getSaltedHash(password, salt);
        kayttajatiedot.setSalt(salt);
        kayttajatiedot.setPassword(hash);
    }

    @Override
    @Transactional
    public String getMfaProviderAndConsumeBypass(String username) {
        Kayttajatiedot kayttajatiedot = kayttajatiedotRepository.findByUsername(username).orElseThrow();
        String mfaProvider = "";
        boolean isBypass = kayttajatiedot.getMfaBypass() != null
                && kayttajatiedot.getMfaBypass().isAfter(LocalDateTime.now().minusSeconds(MFA_BYPASS_MAX_AGE_SECONDS));
        if (kayttajatiedot.getMfaBypass() != null) {
            log.info("db: " + kayttajatiedot.getMfaBypass().toString());
            log.info("now minus seconds: " + LocalDateTime.now().minusSeconds(MFA_BYPASS_MAX_AGE_SECONDS).toString());
            log.info("is after: " + kayttajatiedot.getMfaBypass().isAfter(LocalDateTime.now().minusSeconds(MFA_BYPASS_MAX_AGE_SECONDS)));
            log.info("is bypass: " + isBypass);
        }
        if (kayttajatiedot.getMfaProvider() != null && !isBypass) {
            log.info("mfaprovider: " + kayttajatiedot.getMfaProvider().getMfaProvider());
            mfaProvider = kayttajatiedot.getMfaProvider().getMfaProvider();
        }
        kayttajatiedot.setMfaBypass(null);
        kayttajatiedotRepository.save(kayttajatiedot);
        return mfaProvider;
    }

    @Override
    public Optional<GoogleAuthToken> getGoogleAuthToken(String username) {
        return kayttajatiedotRepository.findGoogleAuthToken(username);
    }
}
