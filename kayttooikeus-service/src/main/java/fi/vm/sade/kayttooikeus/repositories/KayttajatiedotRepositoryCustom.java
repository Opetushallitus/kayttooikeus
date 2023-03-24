package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.dto.KayttajatiedotReadDto;
import fi.vm.sade.kayttooikeus.model.GoogleAuthToken;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface KayttajatiedotRepositoryCustom {

    Optional<KayttajatiedotReadDto> findByHenkiloOid(String henkiloOid);

    Optional<Kayttajatiedot> findByUsername(String username);

    Optional<String> findOidByUsername(String username);

    Collection<Henkilo> findPassiveServiceUsers(LocalDateTime passiveSince);

    Optional<GoogleAuthToken> findGoogleAuthToken(String username);
}
