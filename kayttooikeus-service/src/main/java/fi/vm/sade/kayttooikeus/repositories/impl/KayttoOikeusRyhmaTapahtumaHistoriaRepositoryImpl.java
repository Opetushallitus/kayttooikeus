package fi.vm.sade.kayttooikeus.repositories.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import fi.vm.sade.kayttooikeus.dto.MyonnettyKayttoOikeusDto;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.KayttoOikeusRyhmaTapahtumaHistoriaRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

import static fi.vm.sade.kayttooikeus.dto.KayttoOikeudenTila.*;

@Repository
public class KayttoOikeusRyhmaTapahtumaHistoriaRepositoryImpl extends AbstractRepository implements KayttoOikeusRyhmaTapahtumaHistoriaRepository {

    @Override
    public List<MyonnettyKayttoOikeusDto> findByHenkiloInOrganisaatio(String henkiloOid, String organisaatioOid) {
        QKayttoOikeusRyhmaTapahtumaHistoria korth = QKayttoOikeusRyhmaTapahtumaHistoria.kayttoOikeusRyhmaTapahtumaHistoria;
        QOrganisaatioHenkilo oh = QOrganisaatioHenkilo.organisaatioHenkilo;
        QKayttoOikeusRyhma kor = new QKayttoOikeusRyhma("k1");
        QKayttoOikeus ko = new QKayttoOikeus("ko1");
        QPalvelu palvelu = new QPalvelu("p1");

        BooleanBuilder booleanBuilder = new BooleanBuilder()
                .and(korth.tila.in(Arrays.asList(HYLATTY, PERUUTETTU, SULJETTU, VANHENTUNUT)))
                .and(kor.passivoitu.eq(false))
                .and(oh.henkilo.oidHenkilo.eq(henkiloOid));

        if (StringUtils.isNotBlank(organisaatioOid)) {
            booleanBuilder.and(oh.organisaatioOid.eq(organisaatioOid));
        }

        return jpa().from(korth).distinct()
                .innerJoin(korth.kayttoOikeusRyhma, kor)
                .innerJoin(korth.organisaatioHenkilo, oh)
                .innerJoin(oh.henkilo)
                .leftJoin(kor.kayttoOikeus, ko)
                .leftJoin(ko.palvelu, palvelu)
                .select(Projections.bean(MyonnettyKayttoOikeusDto.class,
                        korth.kayttoOikeusRyhma.id.as("ryhmaId"),
                        korth.id.as("myonnettyTapahtumaId"),
                        korth.organisaatioHenkilo.tehtavanimike.as("tehtavanimike"),
                        korth.organisaatioHenkilo.organisaatioOid.as("organisaatioOid"),
                        korth.tila.as("tila"),
                        korth.kasittelija.oidHenkilo.as("kasittelijaOid"),
                        korth.kayttoOikeusRyhma.nimi.id.as("ryhmaNamesId"),
                        korth.aikaleima.as("kasitelty"),
                        korth.syy.as("muutosSyy")
                )).where(booleanBuilder).fetch();
    }
}
