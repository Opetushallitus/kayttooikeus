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
    private final List<Populator<OrganisaatioViite>> viitteet = new ArrayList<>();
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
    
    public KayttoOikeusRyhmaPopulator withViite(Populator<OrganisaatioViite> viite) {
        this.viitteet.add(viite);
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

    public static Populator<OrganisaatioViite> viite(Populator<KayttoOikeusRyhma> ryhma, String organisaatioTyyppi) {
        return em -> {
            KayttoOikeusRyhma kayttoOikeusRyhma = ryhma.apply(em);
            OrganisaatioViite viite = new OrganisaatioViite();
            viite.setKayttoOikeusRyhma(kayttoOikeusRyhma);
            viite.setOrganisaatioTyyppi(organisaatioTyyppi);
            kayttoOikeusRyhma.addOrganisaatioViite(viite);
            em.persist(viite);
            return viite;
        };
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
        viitteet.forEach(v -> ryhma.getOrganisaatioViite().add(v.apply(entityManager)));
        entityManager.merge(ryhma);
        
        return ryhma;
    }
}
