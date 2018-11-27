package fi.vm.sade.kayttooikeus.repositories.impl;

import fi.vm.sade.kayttooikeus.model.LakkautettuOrganisaatio;
import fi.vm.sade.kayttooikeus.repositories.LakkautettuOrganisaatioRepositoryCustom;
import org.springframework.data.jpa.repository.JpaContext;

import javax.persistence.EntityManager;
import java.util.Collection;

public class LakkautettuOrganisaatioRepositoryImpl implements LakkautettuOrganisaatioRepositoryCustom {

    private final EntityManager entityManager;

    public LakkautettuOrganisaatioRepositoryImpl(JpaContext jpaContext) {
        this.entityManager = jpaContext.getEntityManagerByManagedType(LakkautettuOrganisaatio.class);
    }

    @Override
    public void persistInBatch(Collection<String> oids, int batchSize) {
        int i = 0;
        for (String oid : oids) {
            i++;
            entityManager.persist(new LakkautettuOrganisaatio(oid));
            if (i % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        if (i % batchSize != 0) {
            entityManager.flush();
            entityManager.clear();
        }
    }

}
