package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.CasRedirectParametersResponse;
import fi.vm.sade.kayttooikeus.dto.ChangePasswordRequest;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotCreateDto;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotReadDto;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotUpdateDto;
import fi.vm.sade.kayttooikeus.model.GoogleAuthToken;
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

    CasRedirectParametersResponse changePassword(ChangePasswordRequest changePassword);

    void throwIfUsernameExists(String username);

    void throwIfUsernameExists(String username, Optional<String> henkiloOid);

    void throwIfUsernameIsNotValid(String username);

    void throwIfOldPassword(String oidHenkilo, String password);

    Kayttajatiedot getByUsernameAndPassword(String username, String password);

    Optional<String> getMfaProvider(String username);

    Optional<GoogleAuthToken> getGoogleAuthToken(String username);
}
