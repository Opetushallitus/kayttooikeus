package fi.vm.sade.kayttooikeus.repositories.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;

import fi.vm.sade.kayttooikeus.dto.KayttajaTyyppi;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotReadDto;
import fi.vm.sade.kayttooikeus.dto.MfaProvider;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.KayttajatiedotRepositoryCustom;
import org.springframework.data.jpa.repository.JpaContext;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Collection;
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
                .select(Projections.constructor(KayttajatiedotReadDto.class, qKayttajatiedot.username, qKayttajatiedot.mfaProvider, qHenkilo.kayttajaTyyppi))
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

    @Override
    public Optional<String> findMfaProviderByUsername(String username) {
        QKayttajatiedot qKayttajatiedot = QKayttajatiedot.kayttajatiedot;

        MfaProvider mfaProvider = new JPAQuery<>(em)
                .from(qKayttajatiedot)
                .where(qKayttajatiedot.username.equalsIgnoreCase(username))
                .select(qKayttajatiedot.mfaProvider)
                .fetchOne();
        return Optional.ofNullable(mfaProvider).flatMap(m -> Optional.of(m.getMfaProvider()));
    }

    @Override
    public Optional<GoogleAuthToken> findGoogleAuthToken(String username) {
        QGoogleAuthToken qGoogleAuthToken = QGoogleAuthToken.googleAuthToken;
        QKayttajatiedot qKayttajatiedot = QKayttajatiedot.kayttajatiedot;
        QHenkilo qHenkilo = QHenkilo.henkilo;

        GoogleAuthToken mfaToken = new JPAQuery<>(em)
                .from(qGoogleAuthToken)
                .join(qGoogleAuthToken.henkilo, qHenkilo)
                .join(qHenkilo.kayttajatiedot, qKayttajatiedot)
                .where(qKayttajatiedot.username.equalsIgnoreCase(username))
                .select(qGoogleAuthToken)
                .fetchOne();

        return Optional.ofNullable(mfaToken);
    }

    @Override
    public Collection<Henkilo> findPassiveServiceUsers(LocalDateTime passiveSince) {
        QKayttajatiedot qKayttajatiedot = QKayttajatiedot.kayttajatiedot;
        return new JPAQuery<Henkilo>(em)
                .select(qKayttajatiedot.henkilo)
                .from(qKayttajatiedot)
                .leftJoin(qKayttajatiedot.loginCounter, QLoginCounter.loginCounter)
                .where(
                        qKayttajatiedot.henkilo.kayttajaTyyppi.eq(KayttajaTyyppi.PALVELU).and(
                                (qKayttajatiedot.loginCounter.id.isNotNull()
                                        .and(qKayttajatiedot.loginCounter.lastLogin.before(passiveSince)))
                                        .or(qKayttajatiedot.loginCounter.id.isNull()
                                                .and(qKayttajatiedot.createdAt.before(passiveSince)))))
                .fetch();
    }
}
