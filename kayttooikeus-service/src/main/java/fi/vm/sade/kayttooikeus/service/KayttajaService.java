package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.KayttajaCriteriaDto;
import fi.vm.sade.kayttooikeus.dto.KayttajaReadDto;

public interface KayttajaService {

    Iterable<KayttajaReadDto> list(KayttajaCriteriaDto criteria);

}
