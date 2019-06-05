package fi.vm.sade.kayttooikeus.repositories.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.QBean;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import fi.vm.sade.kayttooikeus.dto.KayttoOikeudenTila;
import fi.vm.sade.kayttooikeus.dto.KayttoOikeusRyhmaDto;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.KayttoOikeusRyhmaRepositoryCustom;
import org.springframework.data.jpa.repository.JpaContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;


@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class KayttoOikeusRyhmaRepositoryImpl implements KayttoOikeusRyhmaRepositoryCustom {

    private final EntityManager em;

    public KayttoOikeusRyhmaRepositoryImpl(JpaContext context) {
        this.em = context.getEntityManagerByManagedType(KayttoOikeusRyhma.class);
    }

    private JPAQueryFactory jpa() {
        return new JPAQueryFactory(this.em);
    }

    private QBean<KayttoOikeusRyhmaDto> KayttoOikeusRyhmaDtoBean() {
        QKayttoOikeusRyhma kayttoOikeusRyhma = QKayttoOikeusRyhma.kayttoOikeusRyhma;
        return Projections.bean(KayttoOikeusRyhmaDto.class,
                kayttoOikeusRyhma.id.as("id"),
                kayttoOikeusRyhma.tunniste.as("tunniste"),
                kayttoOikeusRyhma.rooliRajoite.as("rooliRajoite"),
                kayttoOikeusRyhma.nimi.id.as("nimiId"),
                kayttoOikeusRyhma.kuvaus.id.as("kuvausId"),
                kayttoOikeusRyhma.passivoitu.as("passivoitu"),
                kayttoOikeusRyhma.ryhmaRestriction.as("ryhmaRestriction"),
                kayttoOikeusRyhma.sallittuKayttajatyyppi.as("sallittuKayttajatyyppi"));
    }

    @Override
    public List<KayttoOikeusRyhmaDto> findByIdList(Collection<Long> idList) {
        QKayttoOikeusRyhma kayttoOikeusRyhma = QKayttoOikeusRyhma.kayttoOikeusRyhma;
        if (CollectionUtils.isEmpty(idList)){
            return new ArrayList<>();
        }

        return jpa().from(kayttoOikeusRyhma)
                .leftJoin(kayttoOikeusRyhma.nimi)
                .where(kayttoOikeusRyhma.passivoitu.isFalse())
                .where(kayttoOikeusRyhma.id.in(idList))
                .orderBy(kayttoOikeusRyhma.id.asc())
                .select(KayttoOikeusRyhmaDtoBean())
                .fetch();
    }

    @Override
    public Optional<KayttoOikeusRyhma> findByRyhmaId(Long id, boolean naytaPassivoitu) {
        QKayttoOikeusRyhma kayttoOikeusRyhma = QKayttoOikeusRyhma.kayttoOikeusRyhma;
        JPAQuery<KayttoOikeusRyhma> query = jpa()
                .select(kayttoOikeusRyhma)
                .from(kayttoOikeusRyhma)
                .where(kayttoOikeusRyhma.id.eq(id))
                .distinct();
        if (!naytaPassivoitu) {
            query.where(kayttoOikeusRyhma.passivoitu.eq(false));
        }
        return Optional.ofNullable(query.fetchFirst());
    }

    @Override
    public Boolean ryhmaNameFiExists(String ryhmaNameFi) {
        QKayttoOikeusRyhma kayttoOikeusRyhma = QKayttoOikeusRyhma.kayttoOikeusRyhma;
        return jpa().from(kayttoOikeusRyhma)
                .where(kayttoOikeusRyhma.nimi.texts.any().lang.eq("FI"),
                        kayttoOikeusRyhma.nimi.texts.any().text.eq(ryhmaNameFi))
                .select(kayttoOikeusRyhma)
                .limit(1)
                .fetchCount() > 0;
    }

    @Override
    public List<KayttoOikeusRyhmaDto> listAll(boolean naytaPassivoidut) {
        QKayttoOikeusRyhma kayttoOikeusRyhma = QKayttoOikeusRyhma.kayttoOikeusRyhma;
        JPAQuery<KayttoOikeusRyhmaDto> query = jpa().from(kayttoOikeusRyhma)
                .leftJoin(kayttoOikeusRyhma.nimi)
                .orderBy(kayttoOikeusRyhma.id.asc())
                .select(KayttoOikeusRyhmaDtoBean());
        if (!naytaPassivoidut) {
            query.where(kayttoOikeusRyhma.passivoitu.eq(false));
        }
        return query.fetch();
    }

    @Override
    public List<Tuple> findOrganisaatioOidAndRyhmaIdByHenkiloOid(String oid) {
        QHenkilo henkilo = QHenkilo.henkilo;
        QKayttoOikeusRyhma kayttoOikeusRyhma = QKayttoOikeusRyhma.kayttoOikeusRyhma;
        QOrganisaatioHenkilo organisaatioHenkilo = QOrganisaatioHenkilo.organisaatioHenkilo;
        QMyonnettyKayttoOikeusRyhmaTapahtuma mkt = QMyonnettyKayttoOikeusRyhmaTapahtuma.myonnettyKayttoOikeusRyhmaTapahtuma;

        JPAQuery<Tuple> query = jpa().select(organisaatioHenkilo.organisaatioOid, mkt.kayttoOikeusRyhma.id);
        query.from(henkilo)
                .innerJoin(henkilo.organisaatioHenkilos, organisaatioHenkilo)
                .innerJoin(organisaatioHenkilo.myonnettyKayttoOikeusRyhmas, mkt)
                .innerJoin(mkt.kayttoOikeusRyhma, kayttoOikeusRyhma);
        query.where(henkilo.oidHenkilo.eq(oid)
                .and(kayttoOikeusRyhma.passivoitu.isFalse())
                .and(organisaatioHenkilo.passivoitu.isFalse())
                .and(mkt.tila.eq(KayttoOikeudenTila.MYONNETTY).or(mkt.tila.eq(KayttoOikeudenTila.UUSITTU)))
                .and(mkt.voimassaAlkuPvm.before(LocalDate.now()))
                .and(mkt.voimassaLoppuPvm.after(LocalDate.now())));
        return query.fetch();
    }

    @Override
    public List<KayttoOikeusRyhmaDto> findKayttoOikeusRyhmasByKayttoOikeusIds(List<Long> kayttoOikeusIds) {
        QKayttoOikeus kayttoOikeus = QKayttoOikeus.kayttoOikeus;
        QKayttoOikeusRyhma kayttoOikeusRyhma = QKayttoOikeusRyhma.kayttoOikeusRyhma;

        BooleanBuilder booleanBuilder = new BooleanBuilder()
                .and(kayttoOikeus.id.in(kayttoOikeusIds))
                .and(kayttoOikeusRyhma.passivoitu.eq(false));

        return jpa().from(kayttoOikeusRyhma)
                .innerJoin(kayttoOikeusRyhma.kayttoOikeus, kayttoOikeus)
                .where(booleanBuilder)
                .select(KayttoOikeusRyhmaDtoBean())
                .distinct()
                .fetch();
    }
}
