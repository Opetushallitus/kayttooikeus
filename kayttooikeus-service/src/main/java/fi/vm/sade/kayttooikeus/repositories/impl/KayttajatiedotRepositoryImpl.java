package fi.vm.sade.kayttooikeus.repositories.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotReadDto;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;
import fi.vm.sade.kayttooikeus.model.QHenkilo;
import fi.vm.sade.kayttooikeus.model.QKayttajatiedot;
import fi.vm.sade.kayttooikeus.repositories.KayttajatiedotRepositoryCustom;
import org.springframework.data.jpa.repository.JpaContext;

import javax.persistence.EntityManager;
import java.util.Optional;

public class KayttajatiedotRepositoryImpl implements KayttajatiedotRepositoryCustom {

    private final EntityManager em;

    public KayttajatiedotRepositoryImpl(JpaContext jpaContext) {
        this.em = jpaContext.getEntityManagerByManagedType(Kayttajatiedot.class);
    }

    @Override
    public Optional<KayttajatiedotReadDto> findByHenkiloOid(String henkiloOid) {
        QKayttajatiedot qKayttajatiedot = QKayttajatiedot.kayttajatiedot;
        QHenkilo qHenkilo = QHenkilo.henkilo;

        KayttajatiedotReadDto dto = new JPAQuery<>(em)
                .from(qKayttajatiedot).join(qKayttajatiedot.henkilo, qHenkilo)
                .where(qHenkilo.oidHenkilo.eq(henkiloOid))
                .select(Projections.constructor(KayttajatiedotReadDto.class, qKayttajatiedot.username))
                .fetchOne();
        return Optional.ofNullable(dto);
    }

    @Override
    public Optional<Kayttajatiedot> findByUsername(String username) {
        QKayttajatiedot qKayttajatiedot = QKayttajatiedot.kayttajatiedot;

        Kayttajatiedot entity = new JPAQuery<>(em)
                .from(qKayttajatiedot)
                .where(qKayttajatiedot.username.equalsIgnoreCase(username))
                .select(qKayttajatiedot)
                .fetchOne();
        return Optional.ofNullable(entity);
    }

    @Override
    public Optional<String> findOidByUsername(String username) {
        QKayttajatiedot qKayttajatiedot = QKayttajatiedot.kayttajatiedot;
        QHenkilo qHenkilo = QHenkilo.henkilo;

        String oid = new JPAQuery<>(em)
                .from(qKayttajatiedot)
                .join(qKayttajatiedot.henkilo, qHenkilo)
                .where(qKayttajatiedot.username.equalsIgnoreCase(username))
                .select(qHenkilo.oidHenkilo)
                .fetchOne();
        return Optional.ofNullable(oid);
    }

}
