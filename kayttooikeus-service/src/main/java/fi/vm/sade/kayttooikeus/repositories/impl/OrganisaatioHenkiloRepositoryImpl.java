package fi.vm.sade.kayttooikeus.repositories.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.QBean;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloWithOrganisaatioDto;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloDto;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.OrganisaatioHenkiloCustomRepository;
import org.springframework.data.jpa.repository.JpaContext;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static fi.vm.sade.kayttooikeus.model.QHenkilo.henkilo;
import static fi.vm.sade.kayttooikeus.model.QOrganisaatioHenkilo.organisaatioHenkilo;

@Repository
public class OrganisaatioHenkiloRepositoryImpl implements OrganisaatioHenkiloCustomRepository {

    private final EntityManager em;

    public OrganisaatioHenkiloRepositoryImpl(JpaContext context) {
        this.em = context.getEntityManagerByManagedType(OrganisaatioHenkilo.class);
    }

    private JPAQueryFactory jpa() {
        return new JPAQueryFactory(this.em);
    }

    public static BooleanExpression voimassa(QOrganisaatioHenkilo oh, LocalDate at) {
        return oh.passivoitu.eq(false)
                .and(oh.voimassaAlkuPvm.isNull().or(oh.voimassaAlkuPvm.loe(at)))
                .and(oh.voimassaLoppuPvm.isNull().or(oh.voimassaLoppuPvm.goe(at)));
    }

    @Override
    public List<String> findDistinctOrganisaatiosForHenkiloOid(String henkiloOid) {
        return jpa().from(organisaatioHenkilo)
                .innerJoin(organisaatioHenkilo.henkilo, henkilo)
                .where(voimassa(organisaatioHenkilo, LocalDate.now())
                        .and(henkilo.oidHenkilo.eq(henkiloOid)))
                .select(organisaatioHenkilo.organisaatioOid).distinct().fetch();
    }

    @Override
    public List<String> findUsersOrganisaatioHenkilosByPalveluRoolis(String henkiloOid, Map<String, Set<String>> henkilohakuPalveluRoolis) {
        QHenkilo henkilo = QHenkilo.henkilo;
        QOrganisaatioHenkilo organisaatioHenkilo = QOrganisaatioHenkilo.organisaatioHenkilo;
        QMyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma = QMyonnettyKayttoOikeusRyhmaTapahtuma.myonnettyKayttoOikeusRyhmaTapahtuma;
        QKayttoOikeusRyhma kayttoOikeusRyhma = QKayttoOikeusRyhma.kayttoOikeusRyhma;
        QKayttoOikeus kayttoOikeus = QKayttoOikeus.kayttoOikeus;
        QPalvelu palvelu = QPalvelu.palvelu;

        BooleanBuilder hasPalveluRooliBooleanBuilder = new BooleanBuilder();
        henkilohakuPalveluRoolis.forEach((p,r) -> {
            hasPalveluRooliBooleanBuilder.or(palvelu.name.eq(p).and(kayttoOikeus.rooli.in(r)));
        });

        return jpa().from(organisaatioHenkilo)
                .innerJoin(organisaatioHenkilo.henkilo, henkilo)
                .innerJoin(organisaatioHenkilo.myonnettyKayttoOikeusRyhmas, myonnettyKayttoOikeusRyhmaTapahtuma)
                .innerJoin(myonnettyKayttoOikeusRyhmaTapahtuma.kayttoOikeusRyhma, kayttoOikeusRyhma)
                .innerJoin(kayttoOikeusRyhma.kayttoOikeus, kayttoOikeus)
                .innerJoin(kayttoOikeus.palvelu, palvelu)
                .where(voimassa(organisaatioHenkilo, LocalDate.now())
                        .and(henkilo.oidHenkilo.eq(henkiloOid))
                        .and(hasPalveluRooliBooleanBuilder))
                .select(organisaatioHenkilo.organisaatioOid)
                .distinct().fetch();

    }

    @Override
    public List<OrganisaatioHenkiloWithOrganisaatioDto> findActiveOrganisaatioHenkiloListDtos(String henkiloOoid) {
        return this.findActiveOrganisaatioHenkiloListDtos(henkiloOoid, false);
    }


    @Override
    public List<OrganisaatioHenkiloWithOrganisaatioDto> findActiveOrganisaatioHenkiloListDtos(String henkiloOoid, boolean piilotaOikeudettomat) {
//        JPAQuery<?> query = ;
//
//        if(piilotaOikeudettomat) {
//            query.where(organisaatioHenkilo.myonnettyKayttoOikeusRyhmas.isNotEmpty());
//        }

       return jpa().from(organisaatioHenkilo)
               .innerJoin(organisaatioHenkilo.henkilo, henkilo)
               .where(voimassa(organisaatioHenkilo, LocalDate.now())
                       .and(henkilo.oidHenkilo.eq(henkiloOoid))).select(organisaatioHenkiloDtoProjection(OrganisaatioHenkiloWithOrganisaatioDto.class))
               .orderBy(organisaatioHenkilo.organisaatioOid.asc()).fetch();

    }

    @Override
    public Optional<OrganisaatioHenkiloDto> findByHenkiloOidAndOrganisaatioOid(String henkiloOid, String organisaatioOid) {
        return Optional.ofNullable(jpa().from(organisaatioHenkilo)
                .join(organisaatioHenkilo.henkilo)
                .where(organisaatioHenkilo.organisaatioOid.eq(organisaatioOid),
                        organisaatioHenkilo.henkilo.oidHenkilo.eq(henkiloOid)
                ).select(organisaatioHenkiloDtoProjection(OrganisaatioHenkiloDto.class))
                .distinct().fetchOne());
    }

    @Override
    public List<OrganisaatioHenkiloDto> findOrganisaatioHenkilosForHenkilo(String henkiloOid) {
        return jpa().from(organisaatioHenkilo)
                .join(organisaatioHenkilo.henkilo)
                .where(organisaatioHenkilo.henkilo.oidHenkilo.eq(henkiloOid))
                .select(organisaatioHenkiloDtoProjection(OrganisaatioHenkiloDto.class))
                .fetch();
    }

    private<T extends OrganisaatioHenkiloDto> QBean<T> organisaatioHenkiloDtoProjection(Class<T> clz) {
        return Projections.bean(clz,
                organisaatioHenkilo.id.as("id"),
                organisaatioHenkilo.organisaatioOid.as("organisaatioOid"),
                organisaatioHenkilo.organisaatioHenkiloTyyppi.as("organisaatioHenkiloTyyppi"),
                organisaatioHenkilo.tehtavanimike.as("tehtavanimike"),
                organisaatioHenkilo.passivoitu.as("passivoitu"),
                organisaatioHenkilo.voimassaAlkuPvm.as("voimassaAlkuPvm"),
                organisaatioHenkilo.voimassaLoppuPvm.as("voimassaLoppuPvm")
        );
    }

    @Override
    public boolean isHenkiloInOrganisaatio(String henkiloOid, String organisaatioOid, boolean passivoitu) {
        QOrganisaatioHenkilo qOrganisaatioHenkilo = QOrganisaatioHenkilo.organisaatioHenkilo;
        return jpa().from(qOrganisaatioHenkilo)
                .where(qOrganisaatioHenkilo.henkilo.oidHenkilo.eq(henkiloOid))
                .where(qOrganisaatioHenkilo.organisaatioOid.eq(organisaatioOid))
                .where(qOrganisaatioHenkilo.passivoitu.eq(passivoitu))
                .select(Expressions.ONE).fetchFirst() != null;
    }

    @Override
    public Set<String> findValidByKayttooikeus(String oidHenkilo, String palveluName, String rooli) {
        QHenkilo henkilo = QHenkilo.henkilo;
        QOrganisaatioHenkilo organisaatioHenkilo = QOrganisaatioHenkilo.organisaatioHenkilo;
        QKayttoOikeusRyhma kayttoOikeusRyhma = QKayttoOikeusRyhma.kayttoOikeusRyhma;
        QKayttoOikeus kayttoOikeus = QKayttoOikeus.kayttoOikeus;
        QPalvelu palvelu = QPalvelu.palvelu;
        QMyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma = QMyonnettyKayttoOikeusRyhmaTapahtuma.myonnettyKayttoOikeusRyhmaTapahtuma;
        return new HashSet<>(jpa().from(organisaatioHenkilo)
                .innerJoin(organisaatioHenkilo.henkilo, henkilo)
                .innerJoin(organisaatioHenkilo.myonnettyKayttoOikeusRyhmas, myonnettyKayttoOikeusRyhmaTapahtuma)
                .innerJoin(myonnettyKayttoOikeusRyhmaTapahtuma.kayttoOikeusRyhma, kayttoOikeusRyhma)
                .innerJoin(kayttoOikeusRyhma.kayttoOikeus, kayttoOikeus)
                .innerJoin(kayttoOikeus.palvelu, palvelu)
                .where(organisaatioHenkilo.passivoitu.isFalse())
                .where(palvelu.name.eq(palveluName))
                .where(kayttoOikeus.rooli.eq(rooli))
                .select(organisaatioHenkilo.organisaatioOid)
                .distinct()
                .fetch());
    }
}
