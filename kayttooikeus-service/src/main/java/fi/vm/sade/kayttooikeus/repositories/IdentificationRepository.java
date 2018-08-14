package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.Identification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface IdentificationRepository extends JpaRepository<Identification, Long> {

    Collection<Identification> findByHenkilo(Henkilo henkilo);

    Optional<Identification> findByidpEntityIdAndIdentifier(String idpKey, String idpIdentifier);

    Collection<Identification> findByidpEntityIdAndIdentifierIn(String idpKey, Collection<String> identifiers);

    Optional<Identification> findByAuthtokenAndAuthTokenCreatedGreaterThan(String token, LocalDateTime created);

    default Optional<Identification> findByAuthtokenIsValid(String token) {
        return findByAuthtokenAndAuthTokenCreatedGreaterThan(token, LocalDateTime.now().minusMinutes(1));
    }

    List<Identification> findByHenkiloOidHenkiloAndIdpEntityId(String oid, String idpKey);
}
