package fi.vm.sade.kayttooikeus.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;
import java.util.HashSet;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OmatTiedotDto extends KayttooikeusPerustiedotDto {
    Boolean isAdmin;
    Boolean isMiniAdmin;
    Collection<Long> anomusilmoitus = new HashSet<>();
    MfaProvider mfaProvider;
    String idpEntityId;
}
