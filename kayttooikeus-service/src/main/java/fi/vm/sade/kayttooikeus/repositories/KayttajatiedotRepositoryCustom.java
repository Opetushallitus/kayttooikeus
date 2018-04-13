package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.dto.KayttajatiedotReadDto;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;
import fi.vm.sade.kayttooikeus.service.dto.KayttajatiedotDto;

import java.util.Optional;

public interface KayttajatiedotRepositoryCustom {

    Optional<KayttajatiedotReadDto> findByHenkiloOid(String henkiloOid);

    Optional<Kayttajatiedot> findByUsername(String username);

    Optional<KayttajatiedotDto> findDtoByUsername(String username);

}
