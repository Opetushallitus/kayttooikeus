package fi.vm.sade.kayttooikeus.model;

import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloTyyppi;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "organisaatiohenkilo", uniqueConstraints = @UniqueConstraint(name = "UK_organisaatiohenkilo_01",
        columnNames = { "organisaatio_oid", "henkilo_id" }))
public class OrganisaatioHenkilo extends IdentifiableAndVersionedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "henkilo_id", nullable = false)
    private Henkilo henkilo;

    @Column(name = "organisaatio_oid", nullable = false)
    private String organisaatioOid;

    @Column(name = "tyyppi") 
    @Enumerated(EnumType.STRING)
    private OrganisaatioHenkiloTyyppi organisaatioHenkiloTyyppi;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "organisaatio_oid", insertable = false, updatable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private OrganisaatioCache organisaatioCache;
    
    @OneToMany(mappedBy = "organisaatioHenkilo", cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    private Set<MyonnettyKayttoOikeusRyhmaTapahtuma> myonnettyKayttoOikeusRyhmas = new HashSet<>();

    @OneToMany(mappedBy = "organisaatioHenkilo", cascade = CascadeType.ALL)
    private Set<KayttoOikeusRyhmaTapahtumaHistoria> kayttoOikeusRyhmaHistorias = new HashSet<>();
    
    @Column(name = "passivoitu", nullable = false)
    private boolean passivoitu;

    @Temporal(TemporalType.DATE)
    @Column(name = "voimassa_alku_pvm")
    private LocalDate voimassaAlkuPvm;

    @Temporal(TemporalType.DATE)
    @Column(name = "voimassa_loppu_pvm")
    private LocalDate voimassaLoppuPvm;

    @Column(name = "tehtavanimike")
    private String tehtavanimike;
}
