package fi.vm.sade.kayttooikeus.repositories.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.KayttoOikeusRyhmaMyontoViiteRepository;
import fi.vm.sade.kayttooikeus.repositories.criteria.MyontooikeusCriteria;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.set;
import static com.querydsl.core.types.dsl.Expressions.allOf;
import static fi.vm.sade.kayttooikeus.model.QKayttoOikeusRyhmaMyontoViite.kayttoOikeusRyhmaMyontoViite;
import static java.util.stream.Collectors.toSet;

@Repository
public class KayttoOikeusRyhmaMyontoViiteRepositoryImpl
        extends BaseRepositoryImpl<KayttoOikeusRyhmaMyontoViite>
        implements KayttoOikeusRyhmaMyontoViiteRepository {

    @Override
    public Set<Long> getMasterIdsBySlaveIds(Set<Long> slaveIds) {
        QKayttoOikeusRyhmaMyontoViite qMyontoViite = kayttoOikeusRyhmaMyontoViite;

        return jpa().from(qMyontoViite)
                .where(qMyontoViite.slaveId.in(slaveIds))
                .distinct().select(qMyontoViite.masterId).fetch().stream().collect(toSet());
    }

    @Override
    public List<Long> getSlaveIdsByMasterIds(List<Long> masterIds) {
        QKayttoOikeusRyhmaMyontoViite myontoViite = kayttoOikeusRyhmaMyontoViite;

        if (CollectionUtils.isEmpty(masterIds)) {
            return new ArrayList<>();
        }

        return jpa().from(myontoViite)
                .where(myontoViite.masterId.in(masterIds))
                .distinct().select(myontoViite.slaveId).fetch();
    }

    @Override
    public Map<String, Set<Long>> getSlaveIdsByMasterHenkiloOid(String henkiloOid, MyontooikeusCriteria criteria) {
        QKayttoOikeusRyhmaMyontoViite myontoViite = kayttoOikeusRyhmaMyontoViite;
        QKayttoOikeusRyhma kayttoOikeusRyhma = QKayttoOikeusRyhma.kayttoOikeusRyhma;
        QMyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma = QMyonnettyKayttoOikeusRyhmaTapahtuma.myonnettyKayttoOikeusRyhmaTapahtuma;
        QOrganisaatioHenkilo organisaatioHenkilo = QOrganisaatioHenkilo.organisaatioHenkilo;
        QHenkilo henkilo = QHenkilo.henkilo;

        JPAQuery<?> query = jpa()
                .from(myontoViite, myonnettyKayttoOikeusRyhmaTapahtuma)
                .join(myonnettyKayttoOikeusRyhmaTapahtuma.kayttoOikeusRyhma, kayttoOikeusRyhma)
                .join(myonnettyKayttoOikeusRyhmaTapahtuma.organisaatioHenkilo, organisaatioHenkilo)
                .join(organisaatioHenkilo.henkilo, henkilo)
                .where(kayttoOikeusRyhma.id.eq(myontoViite.masterId))
                .where(henkilo.oidHenkilo.eq(henkiloOid))
                .distinct();

        if (criteria.getKayttooikeudet() != null) {
            QKayttoOikeus kayttoOikeus = QKayttoOikeus.kayttoOikeus;
            QPalvelu palvelu = QPalvelu.palvelu;

            query.join(kayttoOikeusRyhma.kayttoOikeus, kayttoOikeus);
            query.join(kayttoOikeus.palvelu, palvelu);

            query.where(criteria.getKayttooikeudet().entrySet().stream()
                    .map(entry -> allOf(palvelu.name.eq(entry.getKey()), kayttoOikeus.rooli.in(entry.getValue())))
                    .reduce(new BooleanBuilder(), BooleanBuilder::or, BooleanBuilder::or));
        }

        return query.transform(groupBy(organisaatioHenkilo.organisaatioOid).as(set(myontoViite.slaveId)));
    }

    @Override
    public boolean isCyclicMyontoViite(Long masterId, List<Long> slaveIds) {
        if (slaveIds.isEmpty()) {
            return false;
        }
        QKayttoOikeusRyhmaMyontoViite myontoViite = kayttoOikeusRyhmaMyontoViite;
        return exists(jpa().from(myontoViite)
                .where(
                        myontoViite.masterId.in(slaveIds),
                        myontoViite.slaveId.eq(masterId)
                ));
    }

    @Override
    public List<KayttoOikeusRyhmaMyontoViite> getMyontoViites(Long masterId) {
        QKayttoOikeusRyhmaMyontoViite myontoViite = kayttoOikeusRyhmaMyontoViite;
        return jpa().from(myontoViite)
                .where(myontoViite.masterId.eq(masterId))
                .select(myontoViite)
                .fetch();
    }

}
