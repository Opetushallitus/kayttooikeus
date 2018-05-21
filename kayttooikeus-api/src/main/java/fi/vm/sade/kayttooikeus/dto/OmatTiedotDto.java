package fi.vm.sade.kayttooikeus.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OmatTiedotDto extends KayttooikeusPerustiedotDto {
    Boolean isAdmin;
    Boolean isMiniAdmin;
    Boolean anomusilmoitus;
}
