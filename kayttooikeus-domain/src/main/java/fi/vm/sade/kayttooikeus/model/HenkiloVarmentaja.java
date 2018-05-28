package fi.vm.sade.kayttooikeus.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Henkilö voi poikkeustapauksissa varmentaa toisen henkilön joka ei pysty suorittamaan vahvaa tunnistusta.
 */
@Entity
@Table(name = "henkilo_varmentaja_suhde")
@Getter
@Setter
public class HenkiloVarmentaja extends IdentifiableAndVersionedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Henkilo varmennettavaHenkilo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Henkilo varmentavaHenkilo;

    @Column(nullable = false)
    private boolean tila;

    private LocalDateTime aikaleima;
}
