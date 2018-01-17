package fi.vm.sade.kayttooikeus.dto;

import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Email;

/**
 * Käyttäjältä kysyttävät lisätiedot vahvan tunnistautumisen yhteydessä.
 */
@Getter
@Setter
public class VahvaTunnistusRequestDto {

    // käyttäjä vaihtaa salasanan uusien salasanakäytäntöjen mukaiseksi
    @NotNull
    private String salasana;

    // käyttäjä täyttää työsähköpostiosoitteen (jos se puuttuu)
    @Email
    private String tyosahkopostiosoite;

}
