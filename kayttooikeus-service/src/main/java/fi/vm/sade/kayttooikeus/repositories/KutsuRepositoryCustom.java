package fi.vm.sade.kayttooikeus.repositories;

import com.querydsl.core.types.OrderSpecifier;
import fi.vm.sade.kayttooikeus.model.Kutsu;
import fi.vm.sade.kayttooikeus.repositories.criteria.KutsuCriteria;

import java.time.Period;
import java.util.Collection;
import java.util.List;

public interface KutsuRepositoryCustom extends ExpiringEntities<Kutsu> {
    List<Kutsu> listKutsuListDtos(KutsuCriteria criteria, List<OrderSpecifier> orderSpecifier, Long offset, Long amount);
}
