package fi.vm.sade.kayttooikeus.repositories;

import com.querydsl.core.types.OrderSpecifier;
import fi.vm.sade.kayttooikeus.model.Kutsu;
import fi.vm.sade.kayttooikeus.repositories.criteria.KutsuCriteria;

import java.util.List;

public interface KutsuRepositoryCustom extends ExpiringEntitiesRepository<Kutsu> {
    List<Kutsu> listKutsuListDtos(KutsuCriteria criteria, List<OrderSpecifier> orderSpecifier, Long offset, Long amount);
}
