package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.model.HaettuKayttoOikeusRyhma;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface HaettuKayttooikeusRyhmaRepository extends CrudRepository<HaettuKayttoOikeusRyhma, Long>, HaettuKayttooikeusRyhmaRepositoryCustom {

    Optional<HaettuKayttoOikeusRyhma> findById(Long id);

    Set<HaettuKayttoOikeusRyhma> findByIdIn(Set<Long> ids);

}
