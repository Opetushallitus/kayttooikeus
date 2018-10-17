package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface KayttajatiedotRepository extends CrudRepository<Kayttajatiedot, Long>, KayttajatiedotRepositoryCustom {

    Optional<Kayttajatiedot> findByHenkiloOidHenkilo(String oidHenkilo);

    long deleteByHenkilo(Henkilo henkilo);

}
