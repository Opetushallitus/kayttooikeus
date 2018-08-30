package fi.vm.sade.kayttooikeus.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class KutsuCreateDto {
    @NotEmpty
    private String etunimi;
    @NotEmpty
    private String sukunimi;
    @NotNull
    @Email
    private String sahkoposti;
    @NotNull
    private Asiointikieli asiointikieli;
    @Valid
    @NotNull
    private Set<KutsuOrganisaatioDto> organisaatiot;

    @Getter
    @Setter
    public static class KutsuOrganisaatioDto {
        @NotNull
        private String organisaatioOid;
        @Valid
        @NotNull
        private Set<KayttoOikeusRyhmaDto> kayttoOikeusRyhmat;
        @FutureOrPresent
        private LocalDate voimassaLoppuPvm;
    }

    @Getter
    @Setter
    public static class KayttoOikeusRyhmaDto {
        @NotNull
        private Long id;
    }
}
