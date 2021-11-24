package fi.vm.sade.kayttooikeus.controller;


import fi.vm.sade.kayttooikeus.model.Kutsu;
import fi.vm.sade.kayttooikeus.model.KutsuOrganisaatio;
import fi.vm.sade.kayttooikeus.dto.KutsunTila;
import fi.vm.sade.kayttooikeus.repositories.populate.Populator;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KutsuPopulator implements Populator<Kutsu> {
    private final List<Populator<KutsuOrganisaatio>> organisaatiot = new ArrayList<>();
    private final String etunimi;
    private final String sukunimi;
    private final String sahkoposti;
    private String salaisuus = UUID.randomUUID().toString();
    private KutsunTila tila = KutsunTila.AVOIN;
    private String kieliKoodi = "fi";
    private String kutsuja = "kutsuja";
    private String luotuHenkiloOid;
    private LocalDateTime aikaleima = LocalDateTime.now();
    private String temporaryToken;
    private String hetu;
    private String hakaIdentifier;
    private LocalDateTime poistettu;

    public KutsuPopulator(String etunimi, String sukunimi, String sahkoposti) {
        this.etunimi = etunimi;
        this.sukunimi = sukunimi;
        this.sahkoposti = sahkoposti;
    }
    
    public static KutsuPopulator kutsu(String etunimi, String sukunimi, String sahkoposti) {
        return new KutsuPopulator(etunimi, sukunimi, sahkoposti);
    }

    public KutsuPopulator hetu(String hetu) {
        this.hetu = hetu;
        return this;
    }
    
    public KutsuPopulator kutsuja(String kutsuja) {
        this.kutsuja = kutsuja;
        return this;
    }

    public KutsuPopulator tila(KutsunTila tila) {
        this.tila = tila;
        return this;
    }
    
    public KutsuPopulator aikaleima(LocalDateTime aikaleima) {
        this.aikaleima = aikaleima;
        return this;
    }

    public KutsuPopulator salaisuus(String salaisuus) {
        this.salaisuus = salaisuus;
        return this;
    }

    public KutsuPopulator organisaatio(Populator<KutsuOrganisaatio> kutsuOrganisaatio) {
        this.organisaatiot.add(kutsuOrganisaatio);
        return this;
    }
    
    public KutsuPopulator luotuHenkiloOid(String oid) {
        this.luotuHenkiloOid = oid;
        return this;
    }
    
    public KutsuPopulator kieliKoodi(String koodi) {
        this.kieliKoodi = koodi;
        return this;
    }

    public KutsuPopulator temporaryToken(String temporaryToken) {
        this.temporaryToken = temporaryToken;
        return this;
    }

    public KutsuPopulator hakaIdentifier(String hakaIdentifier) {
        this.hakaIdentifier = hakaIdentifier;
        return this;
    }

    public KutsuPopulator poistettu(LocalDateTime poistettu) {
        this.poistettu = poistettu;
        return this;
    }

    @Override
    public Kutsu apply(EntityManager entityManager) {
        return Populator.<Kutsu>firstOptional(entityManager.createQuery("select k " +
                    "from Kutsu k where k.sahkoposti = :sahkoposti")
                .setParameter("sahkoposti", sahkoposti)).orElseGet(() -> {
            Kutsu kutsu = new Kutsu();
            kutsu.setEtunimi(etunimi);
            kutsu.setSukunimi(sukunimi);
            kutsu.setAikaleima(aikaleima);
            kutsu.setTila(tila);
            kutsu.setSahkoposti(sahkoposti);
            kutsu.setSalaisuus(salaisuus);
            kutsu.setKutsuja(kutsuja);
            kutsu.setLuotuHenkiloOid(luotuHenkiloOid);
            kutsu.setKieliKoodi(kieliKoodi);
            kutsu.setTemporaryToken(this.temporaryToken);
            kutsu.setTemporaryTokenCreated(LocalDateTime.now());
            kutsu.setHetu(this.hetu);
            kutsu.setHakaIdentifier(this.hakaIdentifier);
            kutsu.setPoistettu(this.poistettu);
            entityManager.persist(kutsu);
            
            organisaatiot.forEach(organisaatioPopulator -> {
                KutsuOrganisaatio kutsuOrganisaatio = organisaatioPopulator.apply(entityManager);
                kutsu.getOrganisaatiot().add(kutsuOrganisaatio);
                kutsuOrganisaatio.setKutsu(kutsu);
            });
            
            return kutsu;
        });
    }
}
