package fi.vm.sade.kayttooikeus.repositories.impl;

import com.querydsl.core.types.dsl.BooleanExpression;
import fi.vm.sade.kayttooikeus.model.QKayttoOikeus;
import fi.vm.sade.kayttooikeus.model.QKayttoOikeusRyhma;
import fi.vm.sade.kayttooikeus.model.QMyonnettyKayttoOikeusRyhmaTapahtuma;
import fi.vm.sade.kayttooikeus.model.QPalvelu;
import fi.vm.sade.kayttooikeus.repositories.KayttoOikeusRepository;
import org.joda.time.LocalDate;
import org.springframework.stereotype.Repository;

import static fi.vm.sade.kayttooikeus.model.QKayttoOikeus.kayttoOikeus;
import static fi.vm.sade.kayttooikeus.model.QMyonnettyKayttoOikeusRyhmaTapahtuma.myonnettyKayttoOikeusRyhmaTapahtuma;

@Repository
public class KayttoOikeusRepositoryImpl extends AbstractRepository implements KayttoOikeusRepository {
    public static BooleanExpression voimassa(QMyonnettyKayttoOikeusRyhmaTapahtuma tapahtuma, LocalDate at) {
        return tapahtuma.voimassaAlkuPvm.loe(at).and(tapahtuma.voimassaLoppuPvm.isNull().or(tapahtuma.voimassaLoppuPvm.goe(at)));
    }
    
    @Override
    public boolean isHenkiloMyonnettyKayttoOikeusToPalveluInRole(String henkiloOid, String palveluName, String role) {
        QMyonnettyKayttoOikeusRyhmaTapahtuma tapahtuma = myonnettyKayttoOikeusRyhmaTapahtuma;
        QKayttoOikeusRyhma ryhma = tapahtuma.kayttoOikeusRyhma;
        QKayttoOikeus oikeus = new QKayttoOikeus("oikeus");
        QPalvelu palvelu = oikeus.palvelu;
        return exists(jpa().from(tapahtuma)
                .innerJoin(ryhma.kayttoOikeus, oikeus)
                .where(tapahtuma.organisaatioHenkilo.henkilo.oidHenkilo.eq(henkiloOid)
                    .and(oikeus.rooli.eq(role))
                    .and(palvelu.name.eq(palveluName))
                    .and(voimassa(tapahtuma, new LocalDate()))
                    .and(OrganisaatioHenkiloRepositoryImpl.voimassa(tapahtuma.organisaatioHenkilo, new LocalDate()))
                ).select(tapahtuma.id));
    }
}
