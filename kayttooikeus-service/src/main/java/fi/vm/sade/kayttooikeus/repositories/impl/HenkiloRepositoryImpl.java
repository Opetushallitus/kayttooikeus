package fi.vm.sade.kayttooikeus.repositories.impl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.HenkiloHibernateRepository;
import fi.vm.sade.kayttooikeus.repositories.criteria.HenkiloCriteria;
import fi.vm.sade.kayttooikeus.repositories.criteria.OrganisaatioHenkiloCriteria;
import fi.vm.sade.kayttooikeus.repositories.dto.HenkilohakuResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.querydsl.core.types.ExpressionUtils.eq;
import static java.util.stream.Collectors.toSet;

@Repository
@RequiredArgsConstructor
public class HenkiloRepositoryImpl extends BaseRepositoryImpl<Henkilo> implements HenkiloHibernateRepository {

    @Override
    public Set<String> findOidsBy(OrganisaatioHenkiloCriteria criteria) {
        QOrganisaatioHenkilo qOrganisaatio = QOrganisaatioHenkilo.organisaatioHenkilo;
        QHenkilo qHenkilo = QHenkilo.henkilo;

        JPAQuery<String> query = jpa()
                .select(qHenkilo.oidHenkilo).distinct()
                .from(qOrganisaatio)
                .join(qOrganisaatio.henkilo, qHenkilo);

        Optional.ofNullable(criteria.getKayttajaTyyppi()).ifPresent(kayttajaTyyppi
                -> query.where(qHenkilo.kayttajaTyyppi.eq(kayttajaTyyppi)));
        Optional.ofNullable(criteria.getPassivoitu()).ifPresent(passivoitu
                -> query.where(qOrganisaatio.passivoitu.eq(passivoitu)));
        Optional.ofNullable(criteria.getOrganisaatioOids()).ifPresent(organisaatioOid
                -> query.where(qOrganisaatio.organisaatioOid.in(organisaatioOid)));

        if (criteria.getKayttoOikeusRyhmaNimet() != null || criteria.getKayttooikeudet() != null) {
            QMyonnettyKayttoOikeusRyhmaTapahtuma qMyonnettyKayttoOikeusRyhma = QMyonnettyKayttoOikeusRyhmaTapahtuma.myonnettyKayttoOikeusRyhmaTapahtuma;
            QKayttoOikeusRyhma qKayttoOikeusRyhma = QKayttoOikeusRyhma.kayttoOikeusRyhma;

            query.join(qOrganisaatio.myonnettyKayttoOikeusRyhmas, qMyonnettyKayttoOikeusRyhma);
            query.join(qMyonnettyKayttoOikeusRyhma.kayttoOikeusRyhma, qKayttoOikeusRyhma);

            if (criteria.getKayttoOikeusRyhmaNimet() != null) {
                query.where(qKayttoOikeusRyhma.tunniste.in(criteria.getKayttoOikeusRyhmaNimet()));
            }
            if (criteria.getKayttooikeudet() != null) {
                QKayttoOikeus qKayttoOikeus = QKayttoOikeus.kayttoOikeus;
                QPalvelu qPalvelu = QPalvelu.palvelu;

                query.join(qKayttoOikeusRyhma.kayttoOikeus, qKayttoOikeus);
                query.join(qKayttoOikeus.palvelu, qPalvelu);
                query.where(qPalvelu.name.concat("_").concat(qKayttoOikeus.rooli).in(criteria.getKayttooikeudet()));
            }
        }

        return new LinkedHashSet<>(query.fetch());
    }

    @Override
    public Set<String> findOidsBySamaOrganisaatio(String henkiloOid, OrganisaatioHenkiloCriteria criteria) {
        QHenkilo qHenkilo = QHenkilo.henkilo;
        QOrganisaatioHenkilo qOrganisaatio = QOrganisaatioHenkilo.organisaatioHenkilo;
        QHenkilo qHenkiloTarget = new QHenkilo("henkiloTarget");
        QOrganisaatioHenkilo qOrganisaatioTarget = new QOrganisaatioHenkilo("organisaatioTarget");

        JPAQuery<String> query = jpa()
                .select(qHenkiloTarget.oidHenkilo).distinct()
                .from(qHenkilo, qHenkiloTarget)
                .join(qHenkilo.organisaatioHenkilos, qOrganisaatio)
                .join(qHenkiloTarget.organisaatioHenkilos, qOrganisaatioTarget)
                .where(qHenkilo.oidHenkilo.eq(henkiloOid))
                .where(eq(qOrganisaatio.organisaatioOid, qOrganisaatioTarget.organisaatioOid));

        Optional.ofNullable(criteria.getKayttajaTyyppi()).ifPresent(kayttajaTyyppi
                -> query.where(qHenkilo.kayttajaTyyppi.eq(kayttajaTyyppi)));
        Optional.ofNullable(criteria.getPassivoitu()).ifPresent(passivoitu -> {
            query.where(qOrganisaatio.passivoitu.eq(passivoitu));
            query.where(qOrganisaatioTarget.passivoitu.eq(passivoitu));
        });
        Optional.ofNullable(criteria.getOrganisaatioOids()).ifPresent(organisaatioOid
                -> query.where(qOrganisaatio.organisaatioOid.in(organisaatioOid)));

        if (criteria.getKayttoOikeusRyhmaNimet() != null || criteria.getKayttooikeudet() != null) {
            QMyonnettyKayttoOikeusRyhmaTapahtuma qMyonnettyKayttoOikeusRyhma = QMyonnettyKayttoOikeusRyhmaTapahtuma.myonnettyKayttoOikeusRyhmaTapahtuma;
            QKayttoOikeusRyhma qKayttoOikeusRyhma = QKayttoOikeusRyhma.kayttoOikeusRyhma;

            query.join(qOrganisaatio.myonnettyKayttoOikeusRyhmas, qMyonnettyKayttoOikeusRyhma);
            query.join(qMyonnettyKayttoOikeusRyhma.kayttoOikeusRyhma, qKayttoOikeusRyhma);

            if (criteria.getKayttoOikeusRyhmaNimet() != null) {
                query.where(qKayttoOikeusRyhma.tunniste.in(criteria.getKayttoOikeusRyhmaNimet()));
            }
            if (criteria.getKayttooikeudet() != null) {
                QKayttoOikeus qKayttoOikeus = QKayttoOikeus.kayttoOikeus;
                QPalvelu qPalvelu = QPalvelu.palvelu;

                query.join(qKayttoOikeusRyhma.kayttoOikeus, qKayttoOikeus);
                query.join(qKayttoOikeus.palvelu, qPalvelu);
                query.where(qPalvelu.name.concat("_").concat(qKayttoOikeus.rooli).in(criteria.getKayttooikeudet()));
            }
        }

        return new LinkedHashSet<>(query.fetch());
    }

    @Override
    public List<HenkilohakuResultDto> findByUsername(HenkiloCriteria criteria,
                                                     Long offset) {
        QHenkilo qHenkilo = QHenkilo.henkilo;
        QKayttajatiedot qKayttajatiedot = QKayttajatiedot.kayttajatiedot;
        QOrganisaatioHenkilo qOrganisaatioHenkilo = QOrganisaatioHenkilo.organisaatioHenkilo;
        QMyonnettyKayttoOikeusRyhmaTapahtuma qMyonnettyKayttoOikeusRyhmaTapahtuma = QMyonnettyKayttoOikeusRyhmaTapahtuma.myonnettyKayttoOikeusRyhmaTapahtuma;
        List<Tuple> fetchByUsernameResult = new ArrayList<>();
        if ((StringUtils.hasLength(criteria.getNameQuery()) || StringUtils.hasLength(criteria.getKayttajatunnus())) && (offset == null || offset == 0L)) {
            // Should return 0 or 1 results since username is unique.
            fetchByUsernameResult = getFindByCriteriaQuery(criteria, offset, null, null, qHenkilo, qOrganisaatioHenkilo, qMyonnettyKayttoOikeusRyhmaTapahtuma, qKayttajatiedot, new ArrayList<>()).fetch();
        }

        return fetchByUsernameResult.stream().map(tuple -> new HenkilohakuResultDto(
                tuple.get(qHenkilo.oidHenkilo),
                tuple.get(qHenkilo.etunimetCached),
                tuple.get(qHenkilo.sukunimiCached),
                tuple.get(qHenkilo.kayttajatiedot.username)
        )).collect(Collectors.toList());
    }

    @Override
    public List<HenkilohakuResultDto> findByCriteria(HenkiloCriteria criteria,
                                                     Long offset,
                                                     Long limit,
                                                     List<OrderSpecifier> orderBy) {
        QHenkilo qHenkilo = QHenkilo.henkilo;
        QOrganisaatioHenkilo qOrganisaatioHenkilo = QOrganisaatioHenkilo.organisaatioHenkilo;
        QMyonnettyKayttoOikeusRyhmaTapahtuma qMyonnettyKayttoOikeusRyhmaTapahtuma
                = QMyonnettyKayttoOikeusRyhmaTapahtuma.myonnettyKayttoOikeusRyhmaTapahtuma;
        QKayttajatiedot qKayttajatiedot = QKayttajatiedot.kayttajatiedot;

        JPAQuery<Tuple> query = getFindByCriteriaQuery(criteria, offset, limit, orderBy, qHenkilo, qOrganisaatioHenkilo, qMyonnettyKayttoOikeusRyhmaTapahtuma, qKayttajatiedot, new ArrayList<>());

        return query.fetch().stream().map(tuple -> new HenkilohakuResultDto(
                tuple.get(qHenkilo.oidHenkilo),
                tuple.get(qHenkilo.etunimetCached),
                tuple.get(qHenkilo.sukunimiCached),
                tuple.get(qHenkilo.kayttajatiedot.username)
        )).collect(Collectors.toList());
    }

    @Override
    public Long findByCriteriaCount(HenkiloCriteria criteria, List<String> henkiloOids) {
        QHenkilo qHenkilo = QHenkilo.henkilo;
        QOrganisaatioHenkilo qOrganisaatioHenkilo = QOrganisaatioHenkilo.organisaatioHenkilo;
        QMyonnettyKayttoOikeusRyhmaTapahtuma qMyonnettyKayttoOikeusRyhmaTapahtuma
                = QMyonnettyKayttoOikeusRyhmaTapahtuma.myonnettyKayttoOikeusRyhmaTapahtuma;
        QKayttajatiedot qKayttajatiedot = QKayttajatiedot.kayttajatiedot;
        return getFindByCriteriaQuery(criteria, null, null, null, qHenkilo, qOrganisaatioHenkilo, qMyonnettyKayttoOikeusRyhmaTapahtuma, qKayttajatiedot, henkiloOids).fetchCount();
    }

    private JPAQuery<Tuple> getFindByCriteriaQuery(HenkiloCriteria criteria, Long offset, Long limit, List<OrderSpecifier> orderBy, QHenkilo qHenkilo, QOrganisaatioHenkilo qOrganisaatioHenkilo, QMyonnettyKayttoOikeusRyhmaTapahtuma qMyonnettyKayttoOikeusRyhmaTapahtuma,
                                                   QKayttajatiedot qKayttajatiedot, List<String> henkiloOids) {
        JPAQuery<Tuple> query = jpa().from(qHenkilo)
                .leftJoin(qHenkilo.kayttajatiedot, qKayttajatiedot)
                // Organisaatiohenkilos need to be added later (enrichment)
                .select(qHenkilo.sukunimiCached,
                        qHenkilo.etunimetCached,
                        qHenkilo.oidHenkilo,
                        qHenkilo.kayttajatiedot.username)
                .distinct();

        if (!Boolean.TRUE.equals(criteria.getNoOrganisation())) {
            // Exclude henkilos without active organisation
            query.innerJoin(qHenkilo.organisaatioHenkilos, qOrganisaatioHenkilo)
                    .on(qOrganisaatioHenkilo.passivoitu.isFalse());
            if (!CollectionUtils.isEmpty(criteria.getOrganisaatioOids())) {
                query.on(qOrganisaatioHenkilo.organisaatioOid.in(criteria.getOrganisaatioOids()));
            }
            query.leftJoin(qOrganisaatioHenkilo.myonnettyKayttoOikeusRyhmas, qMyonnettyKayttoOikeusRyhmaTapahtuma);
        }
        else {
            // Not excluding henkilos without organisation
            query.leftJoin(qHenkilo.organisaatioHenkilos, qOrganisaatioHenkilo)
                    .leftJoin(qOrganisaatioHenkilo.myonnettyKayttoOikeusRyhmas, qMyonnettyKayttoOikeusRyhmaTapahtuma);
        }

        if (offset != null) {
            query.offset(offset);
        }

        if (limit != null) {
            query.limit(limit);
        }

        if (orderBy != null) {
            orderBy.forEach(query::orderBy);
        }

        query.where(criteria.condition(qHenkilo, qOrganisaatioHenkilo, qMyonnettyKayttoOikeusRyhmaTapahtuma));

        // Exclude henkilos with given oids.
        if(henkiloOids != null && henkiloOids.size() > 0) {
            query.where(qHenkilo.oidHenkilo.notIn(henkiloOids));
        }
        return query;

    }

    @Override
    public List<Henkilo> findByKayttoOikeusRyhmatAndOrganisaatiot(Set<Long> kayttoOikeusRyhmaIds, Set<String> organisaatioOids) {
        QMyonnettyKayttoOikeusRyhmaTapahtuma qMyonnettyKayttoOikeusRyhma = QMyonnettyKayttoOikeusRyhmaTapahtuma.myonnettyKayttoOikeusRyhmaTapahtuma;
        QKayttoOikeusRyhma qKayttoOikeusRyhma = QKayttoOikeusRyhma.kayttoOikeusRyhma;
        QOrganisaatioHenkilo qOrganisaatioHenkilo = QOrganisaatioHenkilo.organisaatioHenkilo;
        QHenkilo qHenkilo = QHenkilo.henkilo;

        return jpa().from(qMyonnettyKayttoOikeusRyhma)
                .join(qMyonnettyKayttoOikeusRyhma.kayttoOikeusRyhma, qKayttoOikeusRyhma)
                .join(qMyonnettyKayttoOikeusRyhma.organisaatioHenkilo, qOrganisaatioHenkilo)
                .join(qOrganisaatioHenkilo.henkilo, qHenkilo)
                .where(qKayttoOikeusRyhma.id.in(kayttoOikeusRyhmaIds))
                .where(qOrganisaatioHenkilo.organisaatioOid.in(organisaatioOids))
                .where(qOrganisaatioHenkilo.passivoitu.isFalse())
                .select(qHenkilo).distinct().fetch();
    }

    @Override
    public Set<String> findOidsByKayttoOikeusRyhmaId(Long kayttoOikeusRyhmaId) {
        QMyonnettyKayttoOikeusRyhmaTapahtuma qMyonnettyKayttoOikeusRyhma = QMyonnettyKayttoOikeusRyhmaTapahtuma.myonnettyKayttoOikeusRyhmaTapahtuma;
        QKayttoOikeusRyhma qKayttoOikeusRyhma = QKayttoOikeusRyhma.kayttoOikeusRyhma;
        QOrganisaatioHenkilo qOrganisaatioHenkilo = QOrganisaatioHenkilo.organisaatioHenkilo;
        QHenkilo qHenkilo = QHenkilo.henkilo;

        return jpa().from(qMyonnettyKayttoOikeusRyhma)
                .join(qMyonnettyKayttoOikeusRyhma.kayttoOikeusRyhma, qKayttoOikeusRyhma)
                .join(qMyonnettyKayttoOikeusRyhma.organisaatioHenkilo, qOrganisaatioHenkilo)
                .join(qOrganisaatioHenkilo.henkilo, qHenkilo)
                .where(qKayttoOikeusRyhma.id.eq(kayttoOikeusRyhmaId))
                .where(qOrganisaatioHenkilo.passivoitu.isFalse())
                .select(qHenkilo.oidHenkilo).distinct()
                .fetch().stream().collect(toSet());
    }

    @Override
    public Set<String> findOidsByHavingUsername() {
        QKayttajatiedot qKayttajatiedot = QKayttajatiedot.kayttajatiedot;
        QHenkilo qHenkilo = QHenkilo.henkilo;

        return jpa().from(qKayttajatiedot)
                .join(qKayttajatiedot.henkilo, qHenkilo)
                .where(qKayttajatiedot.username.isNotNull())
                .select(qHenkilo.oidHenkilo).distinct()
                .fetch().stream().collect(toSet());
    }

}
