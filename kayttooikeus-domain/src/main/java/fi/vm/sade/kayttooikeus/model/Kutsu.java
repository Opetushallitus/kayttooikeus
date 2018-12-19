package fi.vm.sade.kayttooikeus.model;

import fi.vm.sade.kayttooikeus.dto.KutsunTila;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "kutsu", schema = "public")
public class Kutsu extends IdentifiableAndVersionedEntity {
    
    @Column(name = "aikaleima", nullable = false)
    private LocalDateTime aikaleima = LocalDateTime.now();
    
    @Column(name = "kutsuja_oid", nullable = false)
    private String kutsuja;
    
    @Column(name = "kieli_koodi", nullable = false)
    private String kieliKoodi;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tila", nullable = false)
    private KutsunTila tila = KutsunTila.AVOIN;
    
    @Column(name = "etunimi", nullable = false)
    private String etunimi;

    @Column(name = "sukunimi", nullable = false)
    private String sukunimi;
    
    @Column(name = "sahkoposti", nullable = false) 
    private String sahkoposti;
    
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "kutsu")
    private Set<KutsuOrganisaatio> organisaatiot = new HashSet<>(0);
    
    @Column(name = "salaisuus") 
    private String salaisuus; // verification hash
    
    private LocalDateTime kaytetty;
    
    private LocalDateTime poistettu;
    
    @Column(name = "poistaja_oid")
    private String poistaja;

    @Column(name = "luotu_henkilo_oid")
    private String luotuHenkiloOid;

    private String temporaryToken;

    private LocalDateTime temporaryTokenCreated;

    private String hetu;

    private String hakaIdentifier;

    private String saate;

    public void poista(String poistaja) {
        if (this.tila != KutsunTila.AVOIN) {
            throw new IllegalStateException("Can not delete kutsu in state " + tila);
        }
        this.setPoistettu(LocalDateTime.now());
        this.setPoistaja(poistaja);
        this.setTila(KutsunTila.POISTETTU);
    }
}
