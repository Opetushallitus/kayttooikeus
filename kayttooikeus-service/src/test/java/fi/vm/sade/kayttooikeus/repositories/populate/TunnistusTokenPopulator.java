package fi.vm.sade.kayttooikeus.repositories.populate;

import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.TunnistusToken;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;

public class TunnistusTokenPopulator implements Populator<TunnistusToken> {

    private final Populator<Henkilo> henkilo;
    private String loginToken;
    private LocalDateTime aikaleima;

    private TunnistusTokenPopulator(Populator<Henkilo> henkilo) {
        this.henkilo = henkilo;
    }

    public static TunnistusTokenPopulator tunnistusToken(Populator<Henkilo> henkilo) {
        return new TunnistusTokenPopulator(henkilo);
    }

    public TunnistusTokenPopulator loginToken(String loginToken) {
        this.loginToken = loginToken;
        return this;
    }

    public TunnistusTokenPopulator aikaleima(LocalDateTime aikaleima) {
        this.aikaleima = aikaleima;
        return this;
    }

    @Override
    public TunnistusToken apply(EntityManager entityManager) {
        TunnistusToken tunnistusToken = new TunnistusToken();
        tunnistusToken.setHenkilo(henkilo.apply(entityManager));
        tunnistusToken.setLoginToken(loginToken);
        tunnistusToken.setAikaleima(aikaleima);
        entityManager.persist(tunnistusToken);
        return tunnistusToken;
    }

}
