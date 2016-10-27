package fi.vm.sade.kayttooikeus.repositories.impl;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import fi.vm.sade.kayttooikeus.dto.KayttoOikeudenTila;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.KayttoOikeusRyhmaRepository;
import org.joda.time.LocalDate;
import org.springframework.stereotype.Repository;

import java.util.List;

import static fi.vm.sade.kayttooikeus.model.QKayttoOikeusRyhma.kayttoOikeusRyhma;

@Repository
public class KayttoOikeusRyhmaRepositoryImpl extends AbstractRepository implements KayttoOikeusRyhmaRepository {

    @Override
    public List<KayttoOikeusRyhma> listAll() {
        return from(kayttoOikeusRyhma)
                .where(kayttoOikeusRyhma.hidden.eq(false))
                .orderBy(kayttoOikeusRyhma.id.asc())
                .distinct()
                .select(kayttoOikeusRyhma).fetch();
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
                .and(kayttoOikeusRyhma.hidden.isFalse())
                .and(organisaatioHenkilo.passivoitu.isFalse())
                .and(mkt.tila.eq(KayttoOikeudenTila.MYONNETTY).or(mkt.tila.eq(KayttoOikeudenTila.UUSITTU)))
                .and(mkt.voimassaAlkuPvm.before(LocalDate.now()))
                .and(mkt.voimassaLoppuPvm.after(LocalDate.now())));
        return query.fetch();
    }
}
