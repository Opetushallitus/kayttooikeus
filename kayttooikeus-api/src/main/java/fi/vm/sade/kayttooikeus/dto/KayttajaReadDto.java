package fi.vm.sade.kayttooikeus.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KayttajaReadDto {

    private String oid;
    private String etunimet;
    private String kutsumanimi;
    private String sukunimi;
    private String asiointikieli;
    private String sahkoposti;

}
