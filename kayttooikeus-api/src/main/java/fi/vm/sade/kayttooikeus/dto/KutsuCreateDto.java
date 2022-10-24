package fi.vm.sade.kayttooikeus.dto;

import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KutsuCreateDto {

    private String kutsujaOid;

    private String kutsujaForEmail;

    @NotEmpty
    private String etunimi;
    @NotEmpty
    private String sukunimi;
    @NotNull
    @Email
    private String sahkoposti;
    @NotNull
    private Asiointikieli asiointikieli;

    private String saate;

    @Valid
    @NotNull
    private Set<KutsuOrganisaatioCreateDto> organisaatiot;

    @Getter
    @Setter
    public static class KutsuOrganisaatioCreateDto {
        @NotNull
        private String organisaatioOid;
        @Valid
        @NotNull
        private Set<KutsuKayttoOikeusRyhmaCreateDto> kayttoOikeusRyhmat;
        @FutureOrPresent
        private LocalDate voimassaLoppuPvm;
    }

    @Getter
    @Setter
    public static class KutsuKayttoOikeusRyhmaCreateDto {
        @NotNull
        private Long id;
    }
}
