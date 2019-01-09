package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.KayttajatiedotCreateDto;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotReadDto;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotUpdateDto;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;

import java.util.Optional;

public interface KayttajatiedotService {

    KayttajatiedotReadDto create(String henkiloOid, KayttajatiedotCreateDto kayttajatiedot);

    /**
     * Luo tai pävittää ei-tyhjän käyttäjänimen
     * @param oidHenkilo päivitettävän henkilön oid
     * @param username Ei-tyhjä luotava tai pävivitettävä käyttäjänimi
     */
    void createOrUpdateUsername(String oidHenkilo, String username);

    Optional<Kayttajatiedot> getKayttajatiedotByOidHenkilo(String oidHenkilo);

    KayttajatiedotReadDto getByHenkiloOid(String henkiloOid);

    KayttajatiedotReadDto updateKayttajatiedot(String henkiloOid, KayttajatiedotUpdateDto kayttajatiedot);

    void changePasswordAsAdmin(String oid, String newPassword);

    void throwIfUsernameExists(String username);

    void throwIfUsernameIsNotValid(String username);

    KayttajatiedotReadDto getByUsernameAndPassword(String username, String password);
}
