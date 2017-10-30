package fi.vm.sade.kayttooikeus.dto;

import fi.vm.sade.kayttooikeus.dto.validate.ContainsLanguages;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.util.List;
import javax.validation.Valid;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class KayttoOikeusRyhmaModifyDto {

    @NotNull
    @ContainsLanguages
    private TextGroupDto nimi;
    @ContainsLanguages
    private TextGroupDto kuvaus;
    @NotNull
    @Valid
    private List<PalveluRooliModifyDto> palvelutRoolit;
    private List<String> organisaatioTyypit;
    private String rooliRajoite;
    private List<Long> slaveIds;
    private boolean passivoitu;
    private boolean ryhmaRestriction;


    /**
     * Asettaa käyttöoikeusryhmän nimen. Metodi on lisätty vain tukemaan vanhaa
     * formaattia.
     *
     * @param ryhmaName käyttöoikeusryhmän nimi
     * @deprecated käytä setNimi()
     */
    @Deprecated
    public void setRyhmaName(TextGroupDto ryhmaName) {
        this.nimi = ryhmaName;
    }

}
