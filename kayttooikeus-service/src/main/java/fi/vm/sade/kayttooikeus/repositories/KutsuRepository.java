package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.dto.KutsunTila;
import fi.vm.sade.kayttooikeus.model.Kutsu;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface KutsuRepository extends CrudRepository<Kutsu, Long>, KutsuRepositoryCustom {
    Optional<Kutsu> findById(Long id);

    Optional<Kutsu> findByTemporaryTokenAndTilaAndTemporaryTokenCreatedGreaterThan(String temporaryToken, KutsunTila kutsunTila, LocalDateTime created);

    default Optional<Kutsu> findByTemporaryTokenIsValidIsActive(String temporaryToken) {
        return findByTemporaryTokenAndTilaAndTemporaryTokenCreatedGreaterThan(
                temporaryToken, KutsunTila.AVOIN, LocalDateTime.now().minusHours(1));
    }

    Optional<Kutsu> findBySalaisuusAndAikaleimaGreaterThanAndTila(String salaisuus, LocalDateTime created, KutsunTila kutsunTila);

    default Optional<Kutsu> findBySalaisuusIsValid(String salaisuus) {
        return findBySalaisuusAndAikaleimaGreaterThanAndTila(salaisuus, LocalDateTime.now().minusMonths(1), KutsunTila.AVOIN);
    }
}
