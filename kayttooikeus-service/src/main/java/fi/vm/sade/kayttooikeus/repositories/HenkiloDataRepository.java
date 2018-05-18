package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.model.Henkilo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional(propagation = Propagation.MANDATORY)
@Repository
public interface HenkiloDataRepository extends JpaRepository<Henkilo, Long>, HenkiloDataRepositoryCustom {
    Optional<Henkilo> findByOidHenkilo(String oidHenkilo);

    Long countByEtunimetCachedNotNull();

    List<Henkilo> findByOidHenkiloIn(List<String> oidHenkilo);

    @EntityGraph("henkilohaku")
    List<Henkilo> readByOidHenkiloIn(List<String> oidHenkilo);

    List<Henkilo> findByAnomusilmoitusIsTrue();

}
