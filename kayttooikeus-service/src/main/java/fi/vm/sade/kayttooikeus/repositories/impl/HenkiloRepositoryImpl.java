package fi.vm.sade.kayttooikeus.repositories.impl;

import com.querydsl.core.BooleanBuilder;
import fi.vm.sade.kayttooikeus.dto.HenkiloTyyppi;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.repositories.HenkiloHibernateRepository;
import org.joda.time.LocalDate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

import static fi.vm.sade.kayttooikeus.model.QHenkilo.henkilo;
import static fi.vm.sade.kayttooikeus.model.QKayttajatiedot.kayttajatiedot;
import static fi.vm.sade.kayttooikeus.model.QMyonnettyKayttoOikeusRyhmaTapahtuma.myonnettyKayttoOikeusRyhmaTapahtuma;
import static fi.vm.sade.kayttooikeus.model.QOrganisaatioHenkilo.organisaatioHenkilo;

@Repository
public class HenkiloRepositoryImpl extends BaseRepositoryImpl<Henkilo> implements HenkiloHibernateRepository {

    @Override
    public List<String> findHenkiloOids(HenkiloTyyppi henkiloTyyppi, List<String> ooids, String groupName) {
        BooleanBuilder booleanBuilder = new BooleanBuilder()
                .and(henkilo.henkiloTyyppi.eq(henkiloTyyppi));

        if (!CollectionUtils.isEmpty(ooids)) {
            booleanBuilder.and(organisaatioHenkilo.organisaatioOid.in(ooids));
        }
        if (!StringUtils.isEmpty(groupName)) {
            booleanBuilder.and(myonnettyKayttoOikeusRyhmaTapahtuma.kayttoOikeusRyhma.name.eq(groupName));
        }

        BooleanBuilder voimassa = new BooleanBuilder()
                .and(myonnettyKayttoOikeusRyhmaTapahtuma.voimassaAlkuPvm.loe(new LocalDate())
                        .or(myonnettyKayttoOikeusRyhmaTapahtuma.voimassaAlkuPvm.isNull()))
                .and(myonnettyKayttoOikeusRyhmaTapahtuma.voimassaLoppuPvm.gt(new LocalDate())
                        .or(myonnettyKayttoOikeusRyhmaTapahtuma.voimassaLoppuPvm.isNull()));
        booleanBuilder.and(voimassa);

        return jpa().from(henkilo)
                .leftJoin(henkilo.kayttajatiedot, kayttajatiedot)
                .leftJoin(henkilo.organisaatioHenkilos, organisaatioHenkilo)
                .leftJoin(organisaatioHenkilo.myonnettyKayttoOikeusRyhmas, myonnettyKayttoOikeusRyhmaTapahtuma)
                .distinct()
                .select(henkilo.oidHenkilo)
                .where(booleanBuilder)
                .fetch();
    }

    @Override
    public Optional<String> getUsernameByOidHenkilo(String oid) {
        return Optional.ofNullable(jpa().from(henkilo)
                .select(henkilo.kayttajatiedot.username)
                .where(henkilo.oidHenkilo.eq(oid))
                .fetchFirst());
    }
}
