package fi.vm.sade.kayttooikeus.model;

import fi.vm.sade.kayttooikeus.dto.KayttajaTyyppi;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Käyttöoikeusryhmä koostuu käyttöoikeuksista
 */
@Entity
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "kayttooikeusryhma", uniqueConstraints = {@UniqueConstraint(columnNames={"name"})})
@BatchSize(size = 20)
public class KayttoOikeusRyhma extends IdentifiableAndVersionedEntity {

    @Column(name = "name", nullable = false)
    private String tunniste;

    @ManyToMany(cascade = {CascadeType.ALL})
    @JoinTable(name = "kayttooikeusryhma_kayttooikeus", inverseJoinColumns = @JoinColumn(name = "kayttooikeus_id",
                referencedColumnName = "id"), joinColumns = @JoinColumn(name = "kayttooikeusryhma_id",
                referencedColumnName = "id"))
    @BatchSize(size = 50)
    private Set<KayttoOikeus> kayttoOikeus = new HashSet<>();
    
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "textgroup_id")
    private TextGroup nimi;
    
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "kuvaus_id", foreignKey = @ForeignKey(name = "fk_kayttooikeusryhma_textgroup_kuvaus"))
    private TextGroup kuvaus;

    @OneToMany(mappedBy = "kayttoOikeusRyhma", cascade = { CascadeType.MERGE, CascadeType.PERSIST,
            CascadeType.REFRESH }, fetch = FetchType.LAZY)
    private Set<OrganisaatioViite> organisaatioViite = new HashSet<>();
    
    @Column(name = "hidden", nullable = false)
    private boolean passivoitu;

    // Not used anywhere
    @Deprecated
    @Column(name = "rooli_rajoite")
    private String rooliRajoite;

    @Column(name = "ryhma_restriction", nullable = false)
    private boolean ryhmaRestriction;

    // Käyttäjätyyppi jolle tämän käyttöoikeusryhmän voi myöntää. Sallittu kaikille jos ei asetettu.
    @Column(name = "allowed_usertype")
    @Enumerated(EnumType.STRING)
    private KayttajaTyyppi sallittuKayttajatyyppi;

    public void addOrganisaatioViite(OrganisaatioViite organisaatioViite) {
        organisaatioViite.setKayttoOikeusRyhma(this);
        this.organisaatioViite.add(organisaatioViite);
    }
    
    public void removeOrganisaatioViite(OrganisaatioViite organisaatioViite) {
        this.organisaatioViite.remove(organisaatioViite);
    }
    
    public void removeAllOrganisaatioViites() {
        this.organisaatioViite.clear();
    }

    public void addKayttooikeus(KayttoOikeus kayttoOikeus) {
        if(this.kayttoOikeus == null) {
            this.kayttoOikeus = new HashSet<>();
        }
        this.kayttoOikeus.add(kayttoOikeus);
    }

    public boolean isSallittuKayttajatyypilla(KayttajaTyyppi kayttajaTyyppi) {
        return this.getSallittuKayttajatyyppi() == null || this.getSallittuKayttajatyyppi().equals(kayttajaTyyppi);
    }

}
