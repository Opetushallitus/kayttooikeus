package fi.vm.sade.kayttooikeus.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static fi.vm.sade.kayttooikeus.dto.TextGroupDto.localizeLaterById;

/**
 * Sisältää käyttöoikeusryhmän tiedot sekä käyttäjän voimassa olevan käyttöoikeuden tiedot jos käyttäjä voi myöntää
 * tämän käyttöoikeusryhmän.
 * Tätä käytetään myös vanhojen käyttöoikeuksien listaamiseen annettuun organisaatioon.
 */
@Setter
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyonnettyKayttoOikeusDto implements LocalizableDto, Serializable{
    private Long ryhmaId;
    private Long myonnettyTapahtumaId;
    private String organisaatioOid;
    private String tehtavanimike;
    private String ryhmaTunniste;
    private TextGroupDto ryhmaNames;
    private TextGroupDto ryhmaKuvaus;
    private KayttoOikeudenTila tila;
    private String tyyppi = "KORyhma";
    private LocalDate alkuPvm;
    private LocalDate voimassaPvm;
    private LocalDateTime kasitelty;
    private String kasittelijaOid;
    private String kasittelijaNimi;
    // Voiko käyttäjä myöntää tämän käyttöoikeuden (ryhmaId) tähän organisaatioon (organisaatioOid)
    private boolean selected;
    private boolean removed;
    private String muutosSyy;
    private KayttajaTyyppi sallittuKayttajatyyppi;

    public void setRyhmaNamesId(Long ryhmaNamesId) {
        this.ryhmaNames = localizeLaterById(ryhmaNamesId);
    }

    public void setRyhmaKuvausId(Long ryhmaKuvausId) {
        this.ryhmaKuvaus = localizeLaterById(ryhmaKuvausId);
    }

    @Override
    public Stream<Localizable> localizableTexts() {
        return LocalizableDto.of(ryhmaNames, ryhmaKuvaus).localizableTexts();
    }

}
