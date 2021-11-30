package fi.vm.sade.kayttooikeus.repositories;

import java.time.Period;
import java.util.Collection;

public interface ExpiringEntitiesRepository<T> {
    Collection<T> findExpired(Period period);
}
