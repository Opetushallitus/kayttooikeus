package fi.vm.sade.kayttooikeus.repositories.impl;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import fi.vm.sade.kayttooikeus.dto.KayttoOikeudenTila;
import fi.vm.sade.kayttooikeus.enumeration.KayttooikeusRooli;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.AnomusRepositoryCustom;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;

import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import org.springframework.data.jpa.repository.JpaContext;
import org.springframework.stereotype.Repository;

@Repository
public class AnomusRepositoryImpl implements AnomusRepositoryCustom {

    private final EntityManager entityManager;

    public AnomusRepositoryImpl(JpaContext jpaContext) {
        this.entityManager = jpaContext.getEntityManagerByManagedType(Anomus.class);
    }

    @Override
    public List<Anomus> findBy(Function<QAnomus, Predicate> criteria) {
        return findBy(criteria, null, null);
    }

    @Override
    public List<Anomus> findBy(Function<QAnomus, Predicate> criteria, Long limit, Long offset) {
        QAnomus qAnomus = QAnomus.anomus;

        JPAQuery<Anomus> query = new JPAQuery<>(entityManager)
                .from(qAnomus)
                .where(criteria.apply(qAnomus))
                .select(qAnomus)
                .orderBy(qAnomus.anomusTilaTapahtumaPvm.desc());
        if (limit != null) {
            query.limit(limit);
        }
        if (offset != null) {
            query.offset(offset);
        }
        return query.fetch();
    }

    @Override
    public List<Anomus> getOphAdminAnomukset() {
        QHaettuKayttoOikeusRyhma qHaettuKayttoOikeusRyhma = QHaettuKayttoOikeusRyhma.haettuKayttoOikeusRyhma;
        QAnomus qAnomus = QAnomus.anomus;
        QKayttoOikeusRyhma qKayttoOikeusRyhma = QKayttoOikeusRyhma.kayttoOikeusRyhma;
        QKayttoOikeus qKayttoOikeus = QKayttoOikeus.kayttoOikeus;

        JPAQuery<Anomus> query = new JPAQuery<>(entityManager)
                .select(qAnomus)
                .from(qHaettuKayttoOikeusRyhma)
                .join(qHaettuKayttoOikeusRyhma.anomus, qAnomus)
                .join(qHaettuKayttoOikeusRyhma.kayttoOikeusRyhma, qKayttoOikeusRyhma)
                .join(qKayttoOikeusRyhma.kayttoOikeus, qKayttoOikeus)
                .where(
                        qKayttoOikeus.rooli.eq(KayttooikeusRooli.VASTUUKAYTTAJAT.getName())
                                .and(qAnomus.anomuksenTila.eq(AnomuksenTila.ANOTTU))
                                .and(qHaettuKayttoOikeusRyhma.tyyppi.isNull())
                                .and(qKayttoOikeusRyhma.passivoitu.isFalse())
                );

        return query.fetch();
    }

}
