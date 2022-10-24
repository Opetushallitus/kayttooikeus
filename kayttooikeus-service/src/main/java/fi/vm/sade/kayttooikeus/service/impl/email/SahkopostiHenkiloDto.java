package fi.vm.sade.kayttooikeus.service.impl.email;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SahkopostiHenkiloDto {

    private String etunimet;
    private String kutsumanimi;
    private String sukunimi;

    @Override
    public String toString() {
        return kutsumanimi + " " + sukunimi;
    }
}
