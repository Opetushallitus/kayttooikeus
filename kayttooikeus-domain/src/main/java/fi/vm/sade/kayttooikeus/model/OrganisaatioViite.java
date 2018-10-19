package fi.vm.sade.kayttooikeus.model;

import lombok.*;

import javax.persistence.*;

/**
 * Käyttöoikeusryhmälle määritelty organisaatiorajoite (=mihin organisaatioihin käyttöoikeusryhmä voidaan liittää).
 *
 * {@link #organisaatioTyyppi} voi olla jokin seuraavista:
 * <ol>
 *     <li>
 *         <strong>organisaatio oid</strong>: käyttöoikeusryhmä voidaan myöntää vain tietylle organisaatiolle.
 *         Esim. "1.2.246.562.10.79499343246" = käyttöoikeusryhmä voidaan myöntää vain Tampereen kaupungille.
*      </li>
 *     <li>
 *         <strong>organisaatiopalvelun ryhmän oid:n alkuosa (1.2.246.562.28)</strong>: käyttöoikeusryhmä voidaan
 *         myöntää vain organisaatiopalvelun ryhmille.
 *     </li>
 *     <li>
 *         <strong>oppilaitostyypin koodi</strong>: käyttöoikeusryhmä voidaan myöntää vain tietyille oppilaitoksille.
 *         Esim. "15" = käyttöoikeusryhmä voidaan myöntää lukioille.
 *     </li>
 *     <li>
 *         <strong>organisaatiotyypin koodiuri</strong>: käyttöoikeusryhmä voidaan myöntää vain tietyn tyypin
 *         organisaatioille. Esim. "organisaatiotyyppi_03" = käyttyöoikeus voidaan myöntää vain toimipisteille.
 *     </li>
 * </ol>
 */
@Entity
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "organisaatioviite")
public class OrganisaatioViite extends IdentifiableAndVersionedEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kayttooikeusryhma_id", nullable = false, unique = false)
    private KayttoOikeusRyhma kayttoOikeusRyhma;

    @Column(name = "organisaatio_tyyppi")
    private String organisaatioTyyppi;
}
