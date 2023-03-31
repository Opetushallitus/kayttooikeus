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
    private final Integer mfaBypassCount;

    public KayttajatiedotPopulator(Populator<Henkilo> henkilo, String username, MfaProvider mfaProvider,
            LocalDateTime mfaBypass, Integer mfaBypassCount) {
        this.henkilo = henkilo;
        this.username = username;
        this.mfaProvider = mfaProvider;
        this.mfaBypass = mfaBypass;
        this.mfaBypassCount = mfaBypassCount;
    }

    public static KayttajatiedotPopulator kayttajatiedot(Populator<Henkilo> henkilo, String username) {
        return new KayttajatiedotPopulator(henkilo, username, null, null, 0);
    }

    public static KayttajatiedotPopulator kayttajatiedot(Populator<Henkilo> henkilo, String username,
            MfaProvider mfaProvider) {
        return new KayttajatiedotPopulator(henkilo, username, mfaProvider, null, 0);
    }

    public static KayttajatiedotPopulator kayttajatiedot(Populator<Henkilo> henkilo, String username,
            MfaProvider mfaProvider, LocalDateTime mfaBypass, Integer mfaBypassCount) {
        return new KayttajatiedotPopulator(henkilo, username, mfaProvider, mfaBypass, mfaBypassCount);
    }

    @Override
    public Kayttajatiedot apply(EntityManager t) {
        Henkilo henkilo = this.henkilo.apply(t);
        Kayttajatiedot kayttajatiedot = new Kayttajatiedot();
        kayttajatiedot.setHenkilo(henkilo);
        kayttajatiedot.setUsername(username);
        kayttajatiedot.setMfaProvider(mfaProvider);
        kayttajatiedot.setMfaBypass(mfaBypass);
        kayttajatiedot.setMfaBypassCount(mfaBypassCount);
        t.persist(kayttajatiedot);
        henkilo.setKayttajatiedot(kayttajatiedot);
        return kayttajatiedot;
    }

}
