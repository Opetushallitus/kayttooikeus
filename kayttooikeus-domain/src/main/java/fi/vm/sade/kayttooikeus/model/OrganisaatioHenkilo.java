package fi.vm.sade.kayttooikeus.model;

import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloTyyppi;
import lombok.*;

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

    @OneToMany(mappedBy = "organisaatioHenkilo", cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    private Set<MyonnettyKayttoOikeusRyhmaTapahtuma> myonnettyKayttoOikeusRyhmas = new HashSet<>();

    @OneToMany(mappedBy = "organisaatioHenkilo", cascade = CascadeType.ALL)
    private Set<KayttoOikeusRyhmaTapahtumaHistoria> kayttoOikeusRyhmaHistorias = new HashSet<>();
    
    @Column(name = "passivoitu", nullable = false)
    private boolean passivoitu;

    @Column(name = "voimassa_alku_pvm")
    private LocalDate voimassaAlkuPvm;

    @Column(name = "voimassa_loppu_pvm")
    private LocalDate voimassaLoppuPvm;

    @Column(name = "tehtavanimike")
    private String tehtavanimike;

    public void addMyonnettyKayttooikeusryhmaTapahtuma(MyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma) {
        if(this.myonnettyKayttoOikeusRyhmas == null) {
            this.myonnettyKayttoOikeusRyhmas = new HashSet<>();
        }
        this.myonnettyKayttoOikeusRyhmas.add(myonnettyKayttoOikeusRyhmaTapahtuma);
    }

    public boolean isAktiivinen() {
        return !isPassivoitu();
    }
}
