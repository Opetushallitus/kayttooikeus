package fi.vm.sade.kayttooikeus.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
@Table(name = "henkilo", schema = "public")
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

}
