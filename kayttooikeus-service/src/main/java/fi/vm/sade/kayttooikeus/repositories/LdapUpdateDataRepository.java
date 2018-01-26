package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.model.LdapUpdateData;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LdapUpdateDataRepository extends QueryDslPredicateExecutor, JpaRepository<LdapUpdateData, Long>, LdapUpdateDataRepositoryCustom {

    Optional<LdapUpdateData> findById(Long id);

    Long countByKayttoOikeusRyhmaId(Long kayttoOikeusRyhmaId);

    Iterable<LdapUpdateData> findByHenkiloOid(String henkiloOid);

    Iterable<LdapUpdateData> findByHenkiloOidIsNotNull();
}
