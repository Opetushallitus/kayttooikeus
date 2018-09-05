package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.OrganisaatioHenkilo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface OrganisaatioHenkiloRepository extends CrudRepository<OrganisaatioHenkilo, Long>, OrganisaatioHenkiloCustomRepository {

    List<OrganisaatioHenkilo> findByHenkilo(Henkilo henkilo);

    List<OrganisaatioHenkilo> findByHenkiloOidHenkilo(String oidHenkilo);

    Optional<OrganisaatioHenkilo> findByHenkiloOidHenkiloAndOrganisaatioOid(String oidHenkilo, String organisaatioOid);

    List<OrganisaatioHenkilo> findByOrganisaatioOidIn(Set<String> organisaatioOids);

}
