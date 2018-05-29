package fi.vm.sade.kayttooikeus.repositories.impl;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import fi.vm.sade.kayttooikeus.dto.HenkiloLinkitysDto;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.QHenkilo;
import fi.vm.sade.kayttooikeus.model.QHenkiloVarmentaja;
import fi.vm.sade.kayttooikeus.model.QKayttajatiedot;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepositoryCustom;
import org.springframework.data.jpa.repository.JpaContext;

import javax.persistence.EntityManager;
import java.util.Optional;

public class HenkiloDataRepositoryImpl implements HenkiloDataRepositoryCustom {

    private final EntityManager entityManager;

    public HenkiloDataRepositoryImpl(JpaContext jpaContext) {
        this.entityManager = jpaContext.getEntityManagerByManagedType(Henkilo.class);
    }

    @Override
    public Optional<Henkilo> findByKayttajatiedotUsername(String kayttajatunnus) {
        QHenkilo qHenkilo = QHenkilo.henkilo;
        QKayttajatiedot qKayttajatiedot = QKayttajatiedot.kayttajatiedot;

        Henkilo entity = new JPAQuery<>(entityManager)
                .from(qHenkilo)
                .join(qHenkilo.kayttajatiedot, qKayttajatiedot)
                .where(qKayttajatiedot.username.equalsIgnoreCase(kayttajatunnus))
                .select(qHenkilo)
                .fetchOne();
        return Optional.ofNullable(entity);
    }

    @Override
    public HenkiloLinkitysDto findLinkityksetByOid(String oidHenkilo) {
        QHenkilo henkilo = QHenkilo.henkilo;
        QHenkilo varmentajaHenkilo = new QHenkilo("varmentajaHenkilo");
        QHenkilo varmennettavaHenkilo = new QHenkilo("varmennettavaHenkilo");
        QHenkiloVarmentaja varmennettava = new QHenkiloVarmentaja("varmennettavas");
        QHenkiloVarmentaja varmentaja = new QHenkiloVarmentaja("varmentajas");

        JPAQuery<Tuple> query = new JPAQueryFactory(this.entityManager)
                .select(henkilo.oidHenkilo,
                        varmennettavaHenkilo.oidHenkilo,
                        varmentajaHenkilo.oidHenkilo)
                .from(henkilo)
                .leftJoin(henkilo.henkiloVarmennettavas, varmennettava)
                .leftJoin(varmennettava.varmennettavaHenkilo, varmennettavaHenkilo)
                .leftJoin(henkilo.henkiloVarmentajas, varmentaja)
                .leftJoin(varmentaja.varmentavaHenkilo, varmentajaHenkilo)
                .where(henkilo.oidHenkilo.eq(oidHenkilo));

        return query.fetch().stream().reduce(new HenkiloLinkitysDto(),
                (heniloLinkitysDto, tuple) -> {
                    String varmennettavaOid = tuple.get(varmennettavaHenkilo.oidHenkilo);
                    if (varmennettavaOid != null) {
                        heniloLinkitysDto.getHenkiloVarmennettavas().add(varmennettavaOid);
                    }
                    String varmentajaOid = tuple.get(varmentajaHenkilo.oidHenkilo);
                    if (varmentajaOid != null) {
                        heniloLinkitysDto.getHenkiloVarmentajas().add(varmentajaOid);
                    }
                    return heniloLinkitysDto;
                },
                (a,b) -> a);
    }
}
