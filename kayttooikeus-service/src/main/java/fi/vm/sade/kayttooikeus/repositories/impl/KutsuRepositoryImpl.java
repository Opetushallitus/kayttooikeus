package fi.vm.sade.kayttooikeus.repositories.impl;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import fi.vm.sade.kayttooikeus.dto.KutsunTila;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.KutsuRepositoryCustom;
import fi.vm.sade.kayttooikeus.repositories.criteria.KutsuCriteria;
import org.springframework.data.jpa.repository.JpaContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collection;
import java.util.List;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class KutsuRepositoryImpl implements KutsuRepositoryCustom {

    private final EntityManager em;

    public KutsuRepositoryImpl(JpaContext context) {
        this.em = context.getEntityManagerByManagedType(Kutsu.class);
    }

    @Override
    public List<Kutsu> listKutsuListDtos(KutsuCriteria criteria,
                                         List<OrderSpecifier> orderSpecifier, Long offset, Long amount) {
        QKutsu kutsu = QKutsu.kutsu;
        QKutsuOrganisaatio kutsuOrganisaatio = QKutsuOrganisaatio.kutsuOrganisaatio;
        QKayttoOikeusRyhma kutsuKayttoOikeusryhma = new QKayttoOikeusRyhma("kutsuKayttoOikeusryhma");
        QMyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma = QMyonnettyKayttoOikeusRyhmaTapahtuma.myonnettyKayttoOikeusRyhmaTapahtuma;
        QKayttoOikeusRyhma kutsujaKayttooikeusryhma = new QKayttoOikeusRyhma("kutsujaKayttooikeusryhma");
        QHenkilo henkilo = QHenkilo.henkilo;
        QOrganisaatioHenkilo organisaatioHenkilo = QOrganisaatioHenkilo.organisaatioHenkilo;
        JPAQuery<Kutsu> query = new JPAQueryFactory(this.em)
                .from(kutsuOrganisaatio, henkilo)
                .rightJoin(kutsuOrganisaatio.kutsu, kutsu)
                .leftJoin(kutsuOrganisaatio.ryhmat, kutsuKayttoOikeusryhma)
                .innerJoin(henkilo.organisaatioHenkilos, organisaatioHenkilo)
                .leftJoin(organisaatioHenkilo.myonnettyKayttoOikeusRyhmas, myonnettyKayttoOikeusRyhmaTapahtuma)
                .leftJoin(myonnettyKayttoOikeusRyhmaTapahtuma.kayttoOikeusRyhma, kutsujaKayttooikeusryhma)
                .select(kutsu)
                .where(henkilo.oidHenkilo.eq(kutsu.kutsuja))
                .where(criteria.onCondition(kutsuKayttoOikeusryhma, kutsujaKayttooikeusryhma))
                .orderBy(orderSpecifier.toArray(new OrderSpecifier[orderSpecifier.size()]));
        if (offset != null) {
            query.offset(offset);
        }
        if (amount != null) {
            query.limit(amount);
        }
        return query.distinct().fetch();
    }

    @Override
    public Collection<Kutsu> findExpired(Period threshold) {
        QKutsu kutsu = QKutsu.kutsu;
        return new JPAQueryFactory(this.em)
                .from(kutsu)
                .select(kutsu)
                .where(
                        kutsu.tila.eq(KutsunTila.AVOIN)
                                .and(kutsu.poistettu.isNull())
                                .and(kutsu.aikaleima.lt(LocalDateTime.now().minus(threshold))))
                .fetch();
    }
}
