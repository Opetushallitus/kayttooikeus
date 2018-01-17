package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.KayttajatiedotCreateDto;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotReadDto;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotUpdateDto;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService.LdapSynchronizationType;

import java.util.Optional;

public interface KayttajatiedotService {

    KayttajatiedotReadDto create(String henkiloOid, KayttajatiedotCreateDto kayttajatiedot, LdapSynchronizationType ldapSynchronization);

    void createOrUpdateUsername(String oidHenkilo, String username, LdapSynchronizationType ldapSynchronization);

    Optional<Kayttajatiedot> getKayttajatiedotByOidHenkilo(String oidHenkilo);

    KayttajatiedotReadDto getByHenkiloOid(String henkiloOid);

    KayttajatiedotReadDto updateKayttajatiedot(String henkiloOid, KayttajatiedotUpdateDto kayttajatiedot);

    void changePasswordAsAdmin(String oid, String newPassword);

    void changePasswordAsAdmin(String oid, String newPassword, LdapSynchronizationType ldapSynchronizationType);

    void throwIfUsernameExists(String username);

    void throwIfUsernameIsNotValid(String username);
}
