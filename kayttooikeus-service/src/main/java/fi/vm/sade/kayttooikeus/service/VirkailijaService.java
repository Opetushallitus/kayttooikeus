package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.VirkailijaCreateDto;

/**
 * Virkailijoihin liittyvät toiminnot.
 *
 * @see HenkiloService yleiskäyttöisempi palvelu henkilöiden käsittelyyn
 */
public interface VirkailijaService {

    /**
     * Luo virkailijan. Tarkoitettu vain testikäyttöön, tuotannossa virkailijat luodaan kutsun kautta.
     *
     * @param dto luotavan virkailijan tiedot
     * @return luodun virkailijan oid
     */
    String create(VirkailijaCreateDto dto);

}
