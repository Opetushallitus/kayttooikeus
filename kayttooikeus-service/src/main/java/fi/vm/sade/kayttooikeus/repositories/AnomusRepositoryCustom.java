package fi.vm.sade.kayttooikeus.repositories;

import com.querydsl.core.types.Predicate;
import fi.vm.sade.kayttooikeus.model.Anomus;
import fi.vm.sade.kayttooikeus.model.QAnomus;

import java.time.Period;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public interface AnomusRepositoryCustom {

    List<Anomus> findBy(Function<QAnomus, Predicate> criteria);

    List<Anomus> findBy(Function<QAnomus, Predicate> criteria, Long limit, Long offset);

    Collection<Anomus> findExpiredApplications(Period threshold);
}
