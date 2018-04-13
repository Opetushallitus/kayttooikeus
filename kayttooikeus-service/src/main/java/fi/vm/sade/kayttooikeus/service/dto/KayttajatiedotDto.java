package fi.vm.sade.kayttooikeus.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KayttajatiedotDto {

    private String oid;
    private String kayttajatunnus;
    private String salasana;

}
