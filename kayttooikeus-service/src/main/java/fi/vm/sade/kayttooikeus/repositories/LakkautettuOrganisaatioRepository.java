package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.model.LakkautettuOrganisaatio;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface LakkautettuOrganisaatioRepository extends CrudRepository<LakkautettuOrganisaatio, String>, LakkautettuOrganisaatioRepositoryCustom {

    Optional<LakkautettuOrganisaatio> findByOid(String oid);

}
