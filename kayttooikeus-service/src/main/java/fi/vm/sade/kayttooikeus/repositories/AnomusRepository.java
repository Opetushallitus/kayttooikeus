package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.model.Anomus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface AnomusRepository extends CrudRepository<Anomus, Long>, AnomusRepositoryCustom {

    List<Anomus> findByHenkiloOidHenkilo(String oidHenkilo);
}
