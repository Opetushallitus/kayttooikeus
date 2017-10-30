package fi.vm.sade.kayttooikeus.repositories.impl;

import com.querydsl.jpa.impl.JPAQuery;
import fi.vm.sade.kayttooikeus.enumeration.OrderByAnomus;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.HaettuKayttooikeusRyhmaRepositoryCustom;

import java.util.List;
import javax.persistence.EntityManager;

import fi.vm.sade.kayttooikeus.repositories.criteria.AnomusCriteria;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import org.springframework.data.jpa.repository.JpaContext;
import org.springframework.stereotype.Repository;

@Repository
public class HaettuKayttooikeusRyhmaRepositoryImpl implements HaettuKayttooikeusRyhmaRepositoryCustom {

    private final EntityManager entityManager;

    public HaettuKayttooikeusRyhmaRepositoryImpl(JpaContext jpaContext) {
        this.entityManager = jpaContext.getEntityManagerByManagedType(HaettuKayttoOikeusRyhma.class);
    }

    public List<HaettuKayttoOikeusRyhma> findBy(AnomusCriteria.AnomusCriteriaFunction<QAnomus, QKayttoOikeus, QHaettuKayttoOikeusRyhma> criteriaFunction,
                                                Long limit,
                                                Long offset,
                                                OrderByAnomus orderBy,
                                                Boolean adminView) {
        QHaettuKayttoOikeusRyhma qHaettuKayttoOikeusRyhma = QHaettuKayttoOikeusRyhma.haettuKayttoOikeusRyhma;
        QAnomus qAnomus = QAnomus.anomus;
        QKayttoOikeusRyhma qKayttoOikeusRyhma = QKayttoOikeusRyhma.kayttoOikeusRyhma;
        QKayttoOikeus qKayttoOikeus = QKayttoOikeus.kayttoOikeus;

        JPAQuery<HaettuKayttoOikeusRyhma> query = new JPAQuery<>(entityManager)
                .select(qHaettuKayttoOikeusRyhma)
                .from(qHaettuKayttoOikeusRyhma)
                .leftJoin(qHaettuKayttoOikeusRyhma.anomus, qAnomus)
                .leftJoin(qHaettuKayttoOikeusRyhma.kayttoOikeusRyhma, qKayttoOikeusRyhma);
        if (adminView != null && adminView) {
            query.leftJoin(qKayttoOikeusRyhma.kayttoOikeus, qKayttoOikeus);
        }
        query.where(criteriaFunction.apply(qAnomus, qKayttoOikeus, qHaettuKayttoOikeusRyhma));
        query.where(qKayttoOikeusRyhma.passivoitu.isFalse());

        if (limit != null) {
            query.limit(limit);
        }
        if (offset != null) {
            query.offset(offset);
        }
        if (orderBy != null && orderBy.getValue() != null) {
            query.orderBy(orderBy.getValue());
        }
        return query.fetch();
    }

    @Override
    public List<HaettuKayttoOikeusRyhma> findBy(AnomusCriteria.AnomusCriteriaFunction<QAnomus, QKayttoOikeus, QHaettuKayttoOikeusRyhma> criteriaFunction, Boolean adminView) {
        return this.findBy(criteriaFunction, null, null, null, adminView);
    }

}
