package fi.vm.sade.kayttooikeus.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KutsuReadDto {
    private Long id;
    private KutsunTila tila;
    private String kutsujaOid;
    private String etunimi;
    private String sukunimi;
    private String sahkoposti;
    private LocalDateTime aikaleima;
    private Asiointikieli asiointikieli;
    private Set<KutsuOrganisaatioReadDto> organisaatiot = new HashSet<>();
    private Boolean hakaIdentifier;
    private String saate;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KutsuOrganisaatioReadDto extends LocalizableOrganisaatio {
        private Set<KutsuKayttoOikeusRyhmaReadDto> kayttoOikeusRyhmat;
        private LocalDate voimassaLoppuPvm;

        public KutsuOrganisaatioReadDto(TextGroupMapDto nimi, String organisaatioOid, Set<KutsuKayttoOikeusRyhmaReadDto> kayttoOikeusRyhmat) {
            this.nimi = nimi;
            this.organisaatioOid = organisaatioOid;
            this.kayttoOikeusRyhmat = kayttoOikeusRyhmat;
        }
    }

    @Getter
    @Setter
    public static class KutsuKayttoOikeusRyhmaReadDto {
        private Long id;
        private TextGroupMapDto nimi;

    }

}
