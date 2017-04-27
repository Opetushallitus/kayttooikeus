package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.model.OrganisaatioHenkilo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface OrganisaatioHenkiloDataRepository extends CrudRepository<OrganisaatioHenkilo, Long> {
    List<OrganisaatioHenkilo> findByHenkiloOidHenkilo(String oidHenkilo);

    Optional<OrganisaatioHenkilo> findByHenkiloOidHenkiloAndOrganisaatioOid(String oidHenkilo, String organisaatioOid);
}
