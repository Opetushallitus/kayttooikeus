package fi.vm.sade.kayttooikeus.dto;

import fi.vm.sade.kayttooikeus.dto.validate.ContainsLanguages;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;
import javax.validation.Valid;

@Getter
@Setter
@Builder
@NoArgsConstructor
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
    // Not used anywhere
    @Deprecated
    private String rooliRajoite;
    private List<Long> slaveIds;
    @NotNull
    private boolean ryhmaRestriction;
    private KayttajaTyyppi sallittuKayttajatyyppi;


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
