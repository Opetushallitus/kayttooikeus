package fi.vm.sade.kayttooikeus.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PalvelukayttajaReadDto {

    private String oid;
    private String nimi;
    private String kayttajatunnus;

}
