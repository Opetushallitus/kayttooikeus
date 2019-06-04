package fi.vm.sade.kayttooikeus.repositories.populate;

import fi.vm.sade.kayttooikeus.dto.KayttajaTyyppi;
import fi.vm.sade.kayttooikeus.model.KayttoOikeus;
import fi.vm.sade.kayttooikeus.model.KayttoOikeusRyhma;
import fi.vm.sade.kayttooikeus.model.OrganisaatioViite;
import fi.vm.sade.kayttooikeus.model.TextGroup;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

public class KayttoOikeusRyhmaPopulator implements Populator<KayttoOikeusRyhma> {
    private final String tunniste;
    private boolean passivoitu;
    private final List<String> organisaatiorajoitteet = new ArrayList<>();
    private final List<Populator<KayttoOikeus>> oikeus = new ArrayList<>();
    private Populator<TextGroup> kuvaus = Populator.constant(new TextGroup());
    private String rooliRajoite;
    private boolean ryhmaRestriction;
    private KayttajaTyyppi sallittu;

    public KayttoOikeusRyhmaPopulator(String tunniste) {
        this.tunniste = tunniste;
        this.passivoitu = false;
    }
    
    public static KayttoOikeusRyhmaPopulator kayttoOikeusRyhma(String tunniste) {
        return new KayttoOikeusRyhmaPopulator(tunniste);
    }
    
    public KayttoOikeusRyhmaPopulator withOikeus(Populator<KayttoOikeus> oikeus) {
        this.oikeus.add(oikeus);
        return this;
    }

    public KayttoOikeusRyhmaPopulator withSallittu(KayttajaTyyppi sallittu) {
        this.sallittu = sallittu;
        return this;
    }
    
    public KayttoOikeusRyhmaPopulator withNimi(Populator<TextGroup> nimi) {
        this.kuvaus = nimi;
        return this;
    }

    public KayttoOikeusRyhmaPopulator withOrganisaatiorajoite(String rajoite) {
        this.organisaatiorajoitteet.add(rajoite);
        return this;
    }

    public KayttoOikeusRyhmaPopulator withRooliRajoite(String rooliRajoite) {
        this.rooliRajoite = rooliRajoite;
        return this;
    }

    public KayttoOikeusRyhmaPopulator asRyhmaRestriction() {
        this.ryhmaRestriction = true;
        return this;
    }

    public KayttoOikeusRyhmaPopulator asPassivoitu() {
        this.passivoitu = true;
        return this;
    }

    @Override
    public KayttoOikeusRyhma apply(EntityManager entityManager) {
        KayttoOikeusRyhma ryhma = Populator.<KayttoOikeusRyhma>firstOptional(entityManager
                .createQuery("select kor from KayttoOikeusRyhma kor " +
                    "where kor.tunniste = :tunniste").setParameter("tunniste", tunniste)).orElseGet(() -> {
            KayttoOikeusRyhma r = new KayttoOikeusRyhma();
            r.setNimi(kuvaus.apply(entityManager));
            r.setTunniste(tunniste);
            r.setPassivoitu(passivoitu);
            r.setRooliRajoite(rooliRajoite);
            r.setRyhmaRestriction(ryhmaRestriction);
            r.setSallittuKayttajatyyppi(sallittu);
            entityManager.persist(r);
            return r;
        });

        ryhma.setSallittuKayttajatyyppi(sallittu);

        oikeus.forEach(o -> {
            KayttoOikeus kayttoOikeus = o.apply(entityManager);
            ryhma.addKayttooikeus(kayttoOikeus);
            kayttoOikeus.addKayttooikeusRyhma(ryhma);
        });
        organisaatiorajoitteet.forEach(rajoite -> {
            OrganisaatioViite organisaatioViite = new OrganisaatioViite(ryhma, rajoite);
            ryhma.addOrganisaatioViite(organisaatioViite);
            entityManager.persist(organisaatioViite);
        });
        entityManager.merge(ryhma);
        
        return ryhma;
    }
}
