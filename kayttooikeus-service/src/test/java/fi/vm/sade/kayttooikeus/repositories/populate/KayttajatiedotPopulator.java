package fi.vm.sade.kayttooikeus.repositories.populate;

import fi.vm.sade.kayttooikeus.dto.MfaProvider;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;

import java.time.LocalDateTime;

import javax.persistence.EntityManager;

public class KayttajatiedotPopulator implements Populator<Kayttajatiedot> {

    private final Populator<Henkilo> henkilo;
    private final String username;
    private final MfaProvider mfaProvider;
    private final LocalDateTime mfaBypass;

    public KayttajatiedotPopulator(Populator<Henkilo> henkilo, String username, MfaProvider mfaProvider, LocalDateTime mfaBypass) {
        this.henkilo = henkilo;
        this.username = username;
        this.mfaProvider = mfaProvider;
        this.mfaBypass = mfaBypass;
    }

    public static KayttajatiedotPopulator kayttajatiedot(Populator<Henkilo> henkilo, String username) {
        return new KayttajatiedotPopulator(henkilo, username, null, null);
    }

    public static KayttajatiedotPopulator kayttajatiedot(Populator<Henkilo> henkilo, String username, MfaProvider mfaProvider) {
        return new KayttajatiedotPopulator(henkilo, username, mfaProvider, null);
    }

    public static KayttajatiedotPopulator kayttajatiedot(Populator<Henkilo> henkilo, String username, MfaProvider mfaProvider, LocalDateTime mfaBypass) {
        return new KayttajatiedotPopulator(henkilo, username, mfaProvider, mfaBypass);
    }

    @Override
    public Kayttajatiedot apply(EntityManager t) {
        Henkilo henkilo = this.henkilo.apply(t);
        Kayttajatiedot kayttajatiedot = new Kayttajatiedot();
        kayttajatiedot.setHenkilo(henkilo);
        kayttajatiedot.setUsername(username);
        kayttajatiedot.setMfaProvider(mfaProvider);
        kayttajatiedot.setMfaBypass(mfaBypass);
        t.persist(kayttajatiedot);
        henkilo.setKayttajatiedot(kayttajatiedot);
        return kayttajatiedot;
    }

}
