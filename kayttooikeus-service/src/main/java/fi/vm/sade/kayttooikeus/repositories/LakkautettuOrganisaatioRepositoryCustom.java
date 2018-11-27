package fi.vm.sade.kayttooikeus.repositories;

import java.util.Collection;

public interface LakkautettuOrganisaatioRepositoryCustom {

    void persistInBatch(Collection<String> oids, int batchSize);

}
