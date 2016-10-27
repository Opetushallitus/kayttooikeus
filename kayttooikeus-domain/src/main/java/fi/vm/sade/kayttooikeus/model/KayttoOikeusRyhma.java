package fi.vm.sade.kayttooikeus.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Käyttöoikeusryhmä koostuu käyttöoikeuksista
 */
@Entity
@Getter @Setter
@Table(name = "kayttooikeusryhma", uniqueConstraints = {@UniqueConstraint(columnNames={"name"})})
public class KayttoOikeusRyhma extends IdentifiableAndVersionedEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToMany(cascade = {CascadeType.ALL})
    @JoinTable(name = "kayttooikeusryhma_kayttooikeus", inverseJoinColumns = @JoinColumn(name = "kayttooikeus_id",
                referencedColumnName = "id"), joinColumns = @JoinColumn(name = "kayttooikeusryhma_id",
                referencedColumnName = "id"))
    private Set<KayttoOikeus> kayttoOikeus = new HashSet<>();
    
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "textgroup_id")
    private TextGroup description;
    
    @OneToMany(mappedBy = "kayttoOikeusRyhma", cascade = { CascadeType.MERGE, CascadeType.PERSIST,
            CascadeType.REFRESH }, fetch = FetchType.LAZY)
    private Set<OrganisaatioViite> organisaatioViite = new HashSet<>();
    
    @Column(name = "hidden", nullable = false)
    private boolean hidden;
    
    @Column(name = "rooli_rajoite")
    private String rooliRajoite;

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

}
