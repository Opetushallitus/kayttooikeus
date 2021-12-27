package fi.vm.sade.kayttooikeus.model;

import fi.vm.sade.kayttooikeus.dto.KayttoOikeudenTila;
import fi.vm.sade.kayttooikeus.report.AccessRightReportRow;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "myonnetty_kayttooikeusryhma_tapahtuma")
@NamedNativeQuery(
        name = "AccessRightReport",
        resultSetMapping = "AccessRightReportMapping",
        query = "WITH organisaatiohenkilot AS " +
                "(SELECT DISTINCT id, henkilo_id, organisaatio_oid FROM organisaatiohenkilo oh WHERE oh.organisaatio_oid IN :oids) " +
                "SELECT " +
                "       mkt.id," +
                "       h.etunimet_cached || ' ' || h.sukunimi_cached as henkilo," +
                "       h.oidhenkilo," +
                "       '' as organisaatio_nimi," +
                "       oh.organisaatio_oid," +
                "       kor.name as ryhma_nimi," +
                "       kor.id as ryhma_id," +
                "       mkt.voimassaalkupvm," +
                "       mkt.voimassaloppupvm," +
                "       mkt.aikaleima," +
                "       h2.oidhenkilo as muokkaaja " +
                "FROM " +
                "       henkilo h," +
                "       henkilo h2," +
                "       organisaatiohenkilot oh," +
                "       kayttooikeusryhma kor," +
                "       myonnetty_kayttooikeusryhma_tapahtuma mkt " +
                "WHERE " +
                "       h.id = oh.henkilo_id " +
                "       AND h2.id = mkt.kasittelija_henkilo_id " +
                "       AND kor.id = mkt.kayttooikeusryhma_id " +
                "       AND mkt.organisaatiohenkilo_id = oh.id"
)
@SqlResultSetMapping(
        name = "AccessRightReportMapping",
        classes = {
                @ConstructorResult(
                        targetClass = AccessRightReportRow.class,
                        columns = {
                                @ColumnResult(name = "id"),
                                @ColumnResult(name = "henkilo"),
                                @ColumnResult(name = "oidhenkilo"),
                                @ColumnResult(name = "organisaatio_nimi"),
                                @ColumnResult(name = "organisaatio_oid"),
                                @ColumnResult(name = "ryhma_nimi"),
                                @ColumnResult(name = "ryhma_id"),
                                @ColumnResult(name = "voimassaalkupvm"),
                                @ColumnResult(name = "voimassaloppupvm"),
                                @ColumnResult(name = "aikaleima"),
                                @ColumnResult(name = "muokkaaja")
                        }
                )
        }
)
public class MyonnettyKayttoOikeusRyhmaTapahtuma extends IdentifiableAndVersionedEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kayttooikeusryhma_id", nullable = false)
    private KayttoOikeusRyhma kayttoOikeusRyhma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisaatiohenkilo_id", nullable = false)
    private OrganisaatioHenkilo organisaatioHenkilo;

    @Column(name = "syy")
    private String syy;

    @Enumerated(EnumType.STRING)
    @Column(name = "tila", nullable = false)
    private KayttoOikeudenTila tila;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kasittelija_henkilo_id")
    private Henkilo kasittelija;

    @Column(name = "aikaleima", nullable = false)
    private LocalDateTime aikaleima = LocalDateTime.now();

    @Column(name = "voimassaalkupvm", nullable = false)
    private LocalDate voimassaAlkuPvm;

    @Column(name = "voimassaloppupvm")
    private LocalDate voimassaLoppuPvm;

    @ManyToMany(mappedBy = "myonnettyKayttooikeusRyhmas", fetch = FetchType.LAZY)
    private Set<Anomus> anomus = new HashSet<>();

    public KayttoOikeusRyhmaTapahtumaHistoria toHistoria(LocalDateTime aikaleima, String syy) {
        return toHistoria(getKasittelija(), getTila(), aikaleima, syy);
    }

    public KayttoOikeusRyhmaTapahtumaHistoria toHistoria(Henkilo kasittelija, KayttoOikeudenTila tila, LocalDateTime aikaleima, String syy) {
        return new KayttoOikeusRyhmaTapahtumaHistoria(
                getKayttoOikeusRyhma(),
                getOrganisaatioHenkilo(),
                syy,
                tila,
                kasittelija,
                aikaleima
        );
    }

    public void addAnomus(Anomus anomus) {
        if (this.anomus == null) {
            this.anomus = new HashSet<>();
        }
        this.anomus.add(anomus);
    }
}
