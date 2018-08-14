package fi.vm.sade.kayttooikeus.model;

import fi.vm.sade.kayttooikeus.dto.KayttajaTyyppi;
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
public class Henkilo implements Identifiable {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @GeneratedValue
    private Long id;

    @Column(nullable = false, name = "oidhenkilo")
    private String oidHenkilo;

    @Column(name = "henkilotyyppi")
    @Enumerated(EnumType.STRING)
    private KayttajaTyyppi kayttajaTyyppi;

    @OneToOne(mappedBy = "henkilo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Kayttajatiedot kayttajatiedot;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "henkilo", cascade = { CascadeType.MERGE, CascadeType.PERSIST,
            CascadeType.REFRESH })
    private Set<OrganisaatioHenkilo> organisaatioHenkilos = new HashSet<>();

    private String etunimetCached;

    private String kutsumanimiCached;

    private String sukunimiCached;

    private Boolean passivoituCached;

    private Boolean duplicateCached;

    private Boolean vahvastiTunnistettu;

    private String hetuCached;

    private Boolean anomusilmoitus;

    // Suhteet, joissa henkilö on varmentajana
    @OneToMany(mappedBy = "varmentavaHenkilo", fetch = FetchType.LAZY)
    private Set<HenkiloVarmentaja> henkiloVarmennettavas = new HashSet<>();

    // Suhteet, joissa henkilö on varmennettavana
    @OneToMany(mappedBy = "varmennettavaHenkilo", fetch = FetchType.LAZY)
    private Set<HenkiloVarmentaja> henkiloVarmentajas = new HashSet<>();

    public Henkilo(String oidHenkilo) {
        this.oidHenkilo = oidHenkilo;
    }

    public OrganisaatioHenkilo addOrganisaatioHenkilo(OrganisaatioHenkilo organisaatioHenkilo) {
        this.organisaatioHenkilos.add(organisaatioHenkilo);
        return organisaatioHenkilo;
    }
}
