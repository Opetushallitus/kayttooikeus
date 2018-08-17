package fi.vm.sade.kayttooikeus.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;

/**
 * Käyttäjältä kysyttävät lisätiedot vahvan tunnistautumisen yhteydessä.
 */
@Getter
@Setter
public class VahvaTunnistusRequestDto {

    // käyttäjä vaihtaa salasanan uusien salasanakäytäntöjen mukaiseksi
    private String salasana;

    // käyttäjä täyttää työsähköpostiosoitteen (jos se puuttuu)
    @Email
    private String tyosahkopostiosoite;

}
