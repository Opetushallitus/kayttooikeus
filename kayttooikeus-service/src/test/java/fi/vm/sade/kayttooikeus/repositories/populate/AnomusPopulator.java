package fi.vm.sade.kayttooikeus.repositories.populate;

import com.google.common.collect.Sets;
import fi.vm.sade.kayttooikeus.model.AnomuksenTila;
import fi.vm.sade.kayttooikeus.model.Anomus;
import fi.vm.sade.kayttooikeus.model.HaettuKayttoOikeusRyhma;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;

public class AnomusPopulator implements Populator<Anomus> {
    private final String sahkoposti;
    private AnomuksenTila tila;
    private String organisaatioOid;
    private Populator<HaettuKayttoOikeusRyhma> haettuKayttoOikeusRyhmaPopulator;
    private LocalDateTime anottuPvm;

    private AnomusPopulator(String sahkoposti) {
        this.sahkoposti = sahkoposti;
    }

    public static AnomusPopulator anomus(String sahkoposti) {
        return new AnomusPopulator(sahkoposti);
    }

    public AnomusPopulator tila(AnomuksenTila anomuksenTila) {
        this.tila = anomuksenTila;
        return this;
    }

    public AnomusPopulator organisaatioOid(String organisaatioOid) {
        this.organisaatioOid = organisaatioOid;
        return this;
    }

    public AnomusPopulator withHaettuRyhma(Populator<HaettuKayttoOikeusRyhma> haettuKayttoOikeusRyhmaPopulator) {
        this.haettuKayttoOikeusRyhmaPopulator = haettuKayttoOikeusRyhmaPopulator;
        return this;
    }

    public AnomusPopulator anottuPvm(LocalDateTime anottuPvm) {
        this.anottuPvm = anottuPvm;
        return this;
    }

    @Override
    public Anomus apply(EntityManager entityManager) {
        Anomus anomus = new Anomus();
        anomus.setSahkopostiosoite(this.sahkoposti);
        anomus.setAnomuksenTila(this.tila);
        anomus.setOrganisaatioOid(this.organisaatioOid);
        anomus.setAnottuPvm(this.anottuPvm);

        if (haettuKayttoOikeusRyhmaPopulator != null) {
            HaettuKayttoOikeusRyhma haettuKayttoOikeusRyhma = this.haettuKayttoOikeusRyhmaPopulator.apply(entityManager);
            anomus.setHaettuKayttoOikeusRyhmas(Sets.newHashSet(haettuKayttoOikeusRyhma));
            haettuKayttoOikeusRyhma.setAnomus(anomus);
        }

        entityManager.persist(anomus);

        return anomus;
    }
}
