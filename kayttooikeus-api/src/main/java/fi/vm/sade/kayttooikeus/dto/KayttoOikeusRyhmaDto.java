package fi.vm.sade.kayttooikeus.dto;

import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static fi.vm.sade.kayttooikeus.dto.TextGroupDto.localizeLaterById;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KayttoOikeusRyhmaDto implements Serializable, LocalizableDto {

    private Long id;
    private String tunniste;
    private String rooliRajoite;
    private List<OrganisaatioViiteDto> organisaatioViite = new ArrayList<>();
    private TextGroupDto nimi;
    private TextGroupDto kuvaus;
    private boolean passivoitu;
    private boolean ryhmaRestriction;

    public void setNimiId(Long id) {
        this.nimi = localizeLaterById(id);
    }

    public void setKuvausId(Long id) {
        this.kuvaus = localizeLaterById(id);
    }

    @Override
    public Stream<Localizable> localizableTexts() {
        return LocalizableDto.of(nimi, kuvaus).localizableTexts();
    }

    /**
     * Palauttaa käyttöoikeusryhmän tunnisteen. Metodi on lisätty vain tukemaan
     * vanhaa formaattia.
     *
     * @return tunniste
     * @deprecated käytä getTunniste()
     */
    @Deprecated
    public String getName() {
        return tunniste;
    }

    /**
     * Palauttaa käyttöoikeusryhmän nimen. Metodi on lisätty vain tukemaan
     * vanhaa formaattia.
     *
     * @return nimi
     * @deprecated käytä getNimi()
     */
    @Deprecated
    public TextGroupDto getDescription() {
        return nimi;
    }

}
