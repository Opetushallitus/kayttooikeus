package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.model.TunnistusToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Transactional(propagation = Propagation.MANDATORY)
@Repository
public interface TunnistusTokenDataRepository extends CrudRepository<TunnistusToken, Long> {
    Optional<TunnistusToken> findByAikaleimaGreaterThanAndLoginTokenAndKaytettyIsNull(LocalDateTime aikaleima, String loginToken);

    default Optional<TunnistusToken> findByValidLoginToken(String loginToken) {
        return findByAikaleimaGreaterThanAndLoginTokenAndKaytettyIsNull(LocalDateTime.now().minusMinutes(10), loginToken);
    }

    Optional<TunnistusToken> findByLoginToken(String loginToken);
}
