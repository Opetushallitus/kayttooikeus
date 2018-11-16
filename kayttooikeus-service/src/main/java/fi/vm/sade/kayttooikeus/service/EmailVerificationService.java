package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.EmailVerificationResponseDto;
import fi.vm.sade.kayttooikeus.dto.enumeration.LoginTokenValidationCode;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloUpdateDto;

public interface EmailVerificationService {

    /*
     * Päivittää henkilon tiedot, kun käyttäjä on tarkistanut sähköpostiosoitteensa
     */
    EmailVerificationResponseDto emailVerification(HenkiloUpdateDto henkiloUpdate, String loginToken);

    /*
     * Palauttaa uudelleenohjausurlin logintokenin perusteella
     */
    EmailVerificationResponseDto redirectUrlByLoginToken(String loginToken);

    /*
     * Hakee henkilön tiedot loginTokenin perusteella.
     */
    HenkiloDto getHenkiloByLoginToken(String loginToken);

    /*
     * Tarkistaa onko loginToken validi henkilön tietojen päivittämiseen
     */
    LoginTokenValidationCode getLoginTokenValidationCode(String loginToken);

}
