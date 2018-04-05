package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.dto.KayttajaTyyppi;
import fi.vm.sade.kayttooikeus.dto.VirkailijaCreateDto;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.service.CryptoService;
import fi.vm.sade.kayttooikeus.service.KayttajatiedotService;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService;
import fi.vm.sade.kayttooikeus.service.VirkailijaService;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloCreateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class VirkailijaServiceImpl implements VirkailijaService {

    private final KayttajatiedotService kayttajatiedotService;
    private final CryptoService cryptoService;
    private final LdapSynchronizationService ldapSynchronizationService;
    private final OppijanumerorekisteriClient oppijanumerorekisteriClient;
    private final HenkiloDataRepository henkiloRepository;
    private final OrikaBeanMapper mapper;

    @Override
    public String create(VirkailijaCreateDto createDto) {
        String kayttajatunnus = createDto.getKayttajatunnus();
        String salasana = createDto.getSalasana();

        // validointi (suoritettava ennen kuin oid luodaan oppijanumerorekisteriin)
        kayttajatiedotService.throwIfUsernameIsNotValid(kayttajatunnus);
        kayttajatiedotService.throwIfUsernameExists(kayttajatunnus);
        cryptoService.throwIfNotStrongPassword(salasana);

        // luodaan oid oppijanumerorekisteriin
        HenkiloCreateDto henkiloCreateDto = mapper.map(createDto, HenkiloCreateDto.class);
        String oid = oppijanumerorekisteriClient.createHenkilo(henkiloCreateDto);

        // tallennetaan virkailijaksi käyttöoikeuspalveluun
        Henkilo entity = henkiloRepository.findByOidHenkilo(oid).orElseGet(() -> new Henkilo(oid));
        mapper.map(createDto, entity);
        entity.setKayttajaTyyppi(KayttajaTyyppi.VIRKAILIJA);
        Kayttajatiedot kayttajatiedot = new Kayttajatiedot();
        kayttajatiedot.setUsername(kayttajatunnus);
        String salt = cryptoService.generateSalt();
        String hash = cryptoService.getSaltedHash(salasana, salt);
        kayttajatiedot.setSalt(salt);
        kayttajatiedot.setPassword(hash);
        kayttajatiedot.setHenkilo(entity);
        entity.setKayttajatiedot(kayttajatiedot);
        henkiloRepository.save(entity);
        ldapSynchronizationService.updateHenkiloAsap(oid);

        return oid;
    }

}
