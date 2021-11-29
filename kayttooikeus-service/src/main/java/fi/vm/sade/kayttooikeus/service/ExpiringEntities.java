package fi.vm.sade.kayttooikeus.service;

import java.time.Period;
import java.util.Collection;

public interface ExpiringEntities<T> {

    Collection<T> findExpired(Period threshold);

    void discard(T Entity);
}
