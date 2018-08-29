package fi.vm.sade.kayttooikeus.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KayttooikeusAnomusDto {

    @NotNull
    private String organisaatioOrRyhmaOid;

    @Email
    private String email;

    @NotNull
    @Size(min=1)
    private List<Long> kayttooikeusRyhmaIds;

    private String perustelut;

}
