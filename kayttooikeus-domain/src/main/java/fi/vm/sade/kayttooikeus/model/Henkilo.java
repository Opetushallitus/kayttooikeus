package fi.vm.sade.kayttooikeus.model;

import com.sun.org.apache.xpath.internal.operations.Bool;
import fi.vm.sade.kayttooikeus.dto.HenkiloTyyppi;
import lombok.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "henkilo", schema = "public")
@NamedEntityGraphs({
    @NamedEntityGraph(
        name = "henkilohaku",
        attributeNodes = {
            @NamedAttributeNode("organisaatioHenkilos"),
        }
    )
})
public class Henkilo extends IdentifiableAndVersionedEntity {

    @Column(nullable = false, name = "oidhenkilo")
    private String oidHenkilo;

    @Column(name = "passivoitu", nullable = false)
    private boolean passivoitu;

    @Column(name = "henkilotyyppi", nullable = false)
    @Enumerated(EnumType.STRING)
    private HenkiloTyyppi henkiloTyyppi;

    @OneToOne(mappedBy = "henkilo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Kayttajatiedot kayttajatiedot;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "henkilo", cascade = { CascadeType.MERGE, CascadeType.PERSIST,
            CascadeType.REFRESH })
    private Set<OrganisaatioHenkilo> organisaatioHenkilos = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "henkilo", cascade = { CascadeType.MERGE, CascadeType.PERSIST,
            CascadeType.REFRESH })
    private Set<Identification> identifications = new HashSet<>();

    private String etunimetCached;

    private String sukunimiCached;

    private Boolean passivoituCached;

    private Boolean duplicateCached;

    public Henkilo(String oidHenkilo) {
        this.oidHenkilo = oidHenkilo;
    }

    public OrganisaatioHenkilo addOrganisaatioHenkilo(OrganisaatioHenkilo organisaatioHenkilo) {
        this.organisaatioHenkilos.add(organisaatioHenkilo);
        return organisaatioHenkilo;
    }
}
