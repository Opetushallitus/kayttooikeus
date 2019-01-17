package fi.vm.sade.kayttooikeus.model;

import fi.vm.sade.kayttooikeus.dto.PalveluTyyppi;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
@Table(name = "palvelu", uniqueConstraints = {@UniqueConstraint(columnNames={"name"})})
@BatchSize(size = 50)
public class Palvelu extends IdentifiableAndVersionedEntity {
    
    @Column(name = "name", nullable = false, unique = true)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "palvelutyyppi")
    private PalveluTyyppi palveluTyyppi;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "textgroup_id")
    private TextGroup description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kokoelma_id")
    private Palvelu kokoelma;

    @OneToMany(mappedBy = "kokoelma")
    private Set<Palvelu> enclosedPalvelus = new HashSet<Palvelu>();

    @OneToMany(mappedBy = "palvelu")
    private Set<KayttoOikeus> kayttoOikeus = new HashSet<KayttoOikeus>();

    public void addKayttooikeus(KayttoOikeus kayttoOikeus) {
        if(this.kayttoOikeus == null) {
            this.kayttoOikeus = new HashSet<>();
        }
        this.kayttoOikeus.add(kayttoOikeus);
    }
}
