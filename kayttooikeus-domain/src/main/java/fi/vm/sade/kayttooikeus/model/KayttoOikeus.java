package fi.vm.sade.kayttooikeus.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Käyttöoikeus liittää tietyn roolin tiettyyn palveluun
 */
@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "kayttooikeus", uniqueConstraints=@UniqueConstraint(columnNames={"palvelu_id", "textgroup_id"}))
@BatchSize(size = 10)
public class KayttoOikeus extends IdentifiableAndVersionedEntity {

    @Column(name = "rooli", nullable = false)
    private String rooli;
    
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "textgroup_id", nullable = false)
    private TextGroup textGroup;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "kayttoOikeus")
    private Set<KayttoOikeusRyhma> kayttooikeusRyhmas = new HashSet<KayttoOikeusRyhma>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "palvelu_id", nullable = false)
    private Palvelu palvelu;

    public KayttoOikeus(String rooli, Palvelu palvelu) {
        this.rooli = rooli;
        this.palvelu = palvelu;
    }

    public void addKayttooikeusRyhma(KayttoOikeusRyhma kayttoOikeusRyhma) {
        if(this.kayttooikeusRyhmas == null) {
            this.kayttooikeusRyhmas = new HashSet<>();
        }
        this.kayttooikeusRyhmas.add(kayttoOikeusRyhma);
    }
}
