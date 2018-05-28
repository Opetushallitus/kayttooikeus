package fi.vm.sade.kayttooikeus.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Henkilö voi poikkeustapauksissa varmentaa toisen henkilön joka ei pysty suorittamaan vahvaa tunnistusta.
 */
@Entity
@Table(name = "henkilo_varmentaja_suhde")
@Getter
@Setter
public class HenkiloVarmentaja extends IdentifiableAndVersionedEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    private Henkilo varmennettavaHenkilo;

    @ManyToOne(fetch = FetchType.LAZY)
    private Henkilo varmentavaHenkilo;

    private Boolean tila;

    private LocalDateTime aikaleima;
}
