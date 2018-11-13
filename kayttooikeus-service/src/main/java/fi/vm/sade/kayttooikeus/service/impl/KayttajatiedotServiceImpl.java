package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.dto.Constants;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotCreateDto;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotReadDto;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotUpdateDto;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.repositories.KayttajatiedotRepository;
import fi.vm.sade.kayttooikeus.service.CryptoService;
import fi.vm.sade.kayttooikeus.service.KayttajatiedotService;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService.LdapSynchronizationType;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.exception.UnauthorizedException;
import fi.vm.sade.kayttooikeus.service.exception.UsernameAlreadyExistsException;
import fi.vm.sade.kayttooikeus.service.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KayttajatiedotServiceImpl implements KayttajatiedotService {

    private final KayttajatiedotRepository kayttajatiedotRepository;
    private final HenkiloDataRepository henkiloDataRepository;
    private final OrikaBeanMapper mapper;
    private final CryptoService cryptoService;
    private final LdapSynchronizationService ldapSynchronizationService;

    @Override
    @Transactional
    public KayttajatiedotReadDto create(String henkiloOid, KayttajatiedotCreateDto createDto, LdapSynchronizationType ldapSynchronization) {
        KayttajatiedotReadDto readDto = henkiloDataRepository.findByOidHenkilo(henkiloOid)
                .map(henkilo -> create(henkilo, createDto))
                .orElseGet(() -> create(new Henkilo(henkiloOid), createDto));
        ldapSynchronization.getAction().accept(ldapSynchronizationService, henkiloOid);
        return readDto;
    }

    public KayttajatiedotReadDto create(Henkilo henkilo, KayttajatiedotCreateDto createDto) {
        if (henkilo.getKayttajatiedot() != null) {
            throw new IllegalArgumentException("Käyttäjätiedot on jo luotu henkilölle");
        }

        Kayttajatiedot kayttajatiedot = mapper.map(createDto, Kayttajatiedot.class);
        validateUsernameUnique(kayttajatiedot.getUsername(), henkilo.getOidHenkilo());
        return saveKayttajatiedot(henkilo, kayttajatiedot);
    }

    private void validateUsernameUnique(String username, String henkiloOid) {
        henkiloDataRepository.findByKayttajatiedotUsername(username)
                .ifPresent(henkiloByUsername -> {
                    if (henkiloOid == null || !henkiloOid.equals(henkiloByUsername.getOidHenkilo())) {
                        throw new IllegalArgumentException("Käyttäjänimi on jo käytössä");
                    }
                });
    }

    @Override
    @Transactional
    public void createOrUpdateUsername(String oidHenkilo, String username, LdapSynchronizationType ldapSynchronization) {
        if (StringUtils.hasLength(username)) {
            Optional<Kayttajatiedot> kayttajatiedot = this.kayttajatiedotRepository.findByHenkiloOidHenkilo(oidHenkilo);
            if (kayttajatiedot.isPresent()) {
                this.kayttajatiedotRepository.findByUsername(username)
                        .ifPresent((Kayttajatiedot t) -> {
                            throw new IllegalArgumentException("Käyttäjänimi on jo käytössä");
                        });
                kayttajatiedot.get().setUsername(username);
                ldapSynchronization.getAction().accept(this.ldapSynchronizationService, oidHenkilo);
            }
            else {
                this.create(oidHenkilo, new KayttajatiedotCreateDto(username), ldapSynchronization);
            }
        }
        else {
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
    public KayttajatiedotReadDto updateKayttajatiedot(String henkiloOid, KayttajatiedotUpdateDto kayttajatiedotUpdateDto) {
        kayttajatiedotRepository.findByUsername(kayttajatiedotUpdateDto.getUsername()).ifPresent(kayttajatiedot -> {
            if(!kayttajatiedot.getHenkilo().getOidHenkilo().equals(henkiloOid)) {
                throw new IllegalArgumentException("Käyttäjänimi on jo käytössä");
            }
        });

        KayttajatiedotReadDto kayttajatiedotReadDto = henkiloDataRepository.findByOidHenkilo(henkiloOid)
                .map(henkilo -> updateKayttajatiedot(henkilo, kayttajatiedotUpdateDto))
                .orElseGet(() -> updateKayttajatiedot(new Henkilo(henkiloOid), kayttajatiedotUpdateDto));
        this.ldapSynchronizationService.updateHenkiloAsap(henkiloOid);
        return kayttajatiedotReadDto;
    }

    private KayttajatiedotReadDto updateKayttajatiedot(Henkilo henkilo, KayttajatiedotUpdateDto kayttajatiedotUpdateDto) {
        Kayttajatiedot kayttajatiedot = Optional.ofNullable(henkilo.getKayttajatiedot())
                .orElseGet(Kayttajatiedot::new);
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
        changePasswordAsAdmin(oid, newPassword, LdapSynchronizationType.ASAP);
    }

    @Override
    @Transactional
    public void changePasswordAsAdmin(String oid, String newPassword, LdapSynchronizationType ldapSynchronizationType) {
        this.cryptoService.throwIfNotStrongPassword(newPassword);
        this.changePassword(oid, newPassword, ldapSynchronizationType);
    }

    @Override
    @Transactional(readOnly = true)
    public void throwIfUsernameExists(String username) {
        this.kayttajatiedotRepository.findByUsername(username)
                .ifPresent(foundUsername -> {
                    throw new UsernameAlreadyExistsException(String.format("Username %s already exists", foundUsername));
                });
    }

    @Override
    public void throwIfUsernameIsNotValid(String username) {
        if(!username.matches(Constants.USERNAME_REGEXP)) {
            throw new IllegalArgumentException("Username is not valid with pattern " + Constants.USERNAME_REGEXP);
        }
    }

    @Override
    public KayttajatiedotReadDto getByUsernameAndPassword(String username, String password) {
        return kayttajatiedotRepository.findByUsername(username)
                .filter(entity -> cryptoService.check(password, entity.getPassword(), entity.getSalt()))
                .map(entity -> mapper.map(entity, KayttajatiedotReadDto.class))
                .orElseThrow(UnauthorizedException::new);
    }

    private void changePassword(String oid, String newPassword, LdapSynchronizationType ldapSynchronizationType) {
        setPasswordForHenkilo(oid, newPassword);
        // Trigger ASAP priority update to LDAP
        ldapSynchronizationType.getAction().accept(ldapSynchronizationService, oid);
    }

    private void setPasswordForHenkilo(String oidHenkilo, String password) {
        Kayttajatiedot kayttajatiedot = this.kayttajatiedotRepository.findByHenkiloOidHenkilo(oidHenkilo)
                .orElseThrow(() -> new ValidationException("Käyttäjätunnus on asetettava ennen salasanaa"));
        String salt = this.cryptoService.generateSalt();
        String hash = this.cryptoService.getSaltedHash(password, salt);
        kayttajatiedot.setSalt(salt);
        kayttajatiedot.setPassword(hash);
    }

}
