package fi.vm.sade.kayttooikeus.repositories.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.QBean;
import com.querydsl.jpa.impl.JPAQuery;
import fi.vm.sade.kayttooikeus.dto.KayttoOikeudenTila;
import fi.vm.sade.kayttooikeus.dto.KayttoOikeusRyhmaDto;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.KayttoOikeusRyhmaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static fi.vm.sade.kayttooikeus.model.QKayttoOikeus.kayttoOikeus;
import static fi.vm.sade.kayttooikeus.model.QKayttoOikeusRyhma.kayttoOikeusRyhma;

@Repository
public class KayttoOikeusRyhmaRepositoryImpl extends BaseRepositoryImpl<KayttoOikeusRyhma> implements KayttoOikeusRyhmaRepository {

    private QBean<KayttoOikeusRyhmaDto> KayttoOikeusRyhmaDtoBean() {
        return Projections.bean(KayttoOikeusRyhmaDto.class,
                kayttoOikeusRyhma.id.as("id"),
                kayttoOikeusRyhma.tunniste.as("tunniste"),
                kayttoOikeusRyhma.rooliRajoite.as("rooliRajoite"),
                kayttoOikeusRyhma.nimi.id.as("nimiId"),
                kayttoOikeusRyhma.kuvaus.id.as("kuvausId"),
                kayttoOikeusRyhma.passivoitu.as("passivoitu"));
    }

    @Override
    public List<KayttoOikeusRyhmaDto> findByIdList(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)){
            return new ArrayList<>();
        }

        BooleanBuilder booleanBuilder = new BooleanBuilder()
                .and(kayttoOikeusRyhma.passivoitu.eq(false))
                .and(kayttoOikeusRyhma.id.in(idList));

        return jpa().from(kayttoOikeusRyhma)
                .leftJoin(kayttoOikeusRyhma.nimi)
                .where(booleanBuilder)
                .orderBy(kayttoOikeusRyhma.id.asc())
                .select(KayttoOikeusRyhmaDtoBean())
                .fetch();
    }

    @Override
    public Optional<KayttoOikeusRyhma> findByRyhmaId(Long id) {
        return Optional.ofNullable(from(kayttoOikeusRyhma)
                .where(kayttoOikeusRyhma.passivoitu.eq(false)
                        .and(kayttoOikeusRyhma.id.eq(id)))
                .distinct()
                .select(kayttoOikeusRyhma).fetchFirst());
    }

    @Override
    public Boolean ryhmaNameFiExists(String ryhmaNameFi) {
        return exists(jpa().from(kayttoOikeusRyhma)
                .where(kayttoOikeusRyhma.nimi.texts.any().lang.eq("FI"),
                        kayttoOikeusRyhma.nimi.texts.any().text.eq(ryhmaNameFi))
                .select(kayttoOikeusRyhma));
    }

    @Override
    public List<KayttoOikeusRyhmaDto> listAll() {
        return jpa().from(kayttoOikeusRyhma)
                .leftJoin(kayttoOikeusRyhma.nimi)
                .where(kayttoOikeusRyhma.passivoitu.eq(false))
                .orderBy(kayttoOikeusRyhma.id.asc())
                .select(KayttoOikeusRyhmaDtoBean())
                .fetch();
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
