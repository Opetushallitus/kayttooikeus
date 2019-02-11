package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.model.KayttoOikeusRyhma;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface KayttoOikeusRyhmaRepository extends KayttoOikeusRyhmaRepositoryCustom, CrudRepository<KayttoOikeusRyhma, Long> {
    Collection<KayttoOikeusRyhma> findByIdIn(Collection<Long> ids);
}
