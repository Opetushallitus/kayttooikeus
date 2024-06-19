package fi.vm.sade.kayttooikeus;

import fi.vm.sade.kayttooikeus.dto.KayttajaTyyppi;
import fi.vm.sade.kayttooikeus.dto.MfaProvider;
import fi.vm.sade.kayttooikeus.model.Identification;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;

import java.util.Optional;

public record CasUserAttributes(
        String oidHenkilo,
        String username,
        MfaProvider mfaProvider,
        KayttajaTyyppi kayttajaTyyppi,
        @Deprecated KayttajaTyyppi henkiloTyyppi, // for SAMLAuthenticationHandler, removed later when it reads kayttajaTyyppi field
        String idpEntityId,
        @Deprecated KayttajatiedotForCas kayttajatiedot // for SAMLAuthenticationHandler, removed later when it reads username from top level field
) {
    public static CasUserAttributes fromIdentification(Identification identification) {
        var h = identification.getHenkilo();
        var kt = Optional.ofNullable(h.getKayttajatiedot());
        var username = kt.map(Kayttajatiedot::getUsername).orElse(null);
        var mfaProvider = kt.map(Kayttajatiedot::getMfaProvider).orElse(null);
        return new CasUserAttributes(
                h.getOidHenkilo(),
                username,
                mfaProvider,
                h.getKayttajaTyyppi(),
                h.getKayttajaTyyppi(),
                identification.getIdpEntityId(),
                new KayttajatiedotForCas(username)
        );
    }

    public static CasUserAttributes fromKayttajatiedot(Kayttajatiedot kt) {
        var h = kt.getHenkilo();
        return new CasUserAttributes(
                kt.getHenkilo().getOidHenkilo(),
                kt.getUsername(),
                kt.getMfaProvider(),
                h.getKayttajaTyyppi(),
                h.getKayttajaTyyppi(),
                null,
                new KayttajatiedotForCas(kt.getUsername())
        );
    }
}
