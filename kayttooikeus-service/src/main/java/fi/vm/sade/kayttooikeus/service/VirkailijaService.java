package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.VirkailijaCreateDto;
import fi.vm.sade.kayttooikeus.dto.VirkailijaCriteriaDto;
import fi.vm.sade.kayttooikeus.dto.KayttajaReadDto;

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

    Iterable<KayttajaReadDto> list(VirkailijaCriteriaDto criteria);

}
