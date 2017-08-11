package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.repositories.dto.HenkiloCreateByKutsuDto;
import fi.vm.sade.kayttooikeus.dto.KutsuCreateDto;
import fi.vm.sade.kayttooikeus.dto.KutsuReadDto;
import fi.vm.sade.kayttooikeus.enumeration.KutsuOrganisaatioOrder;
import fi.vm.sade.kayttooikeus.model.Kutsu;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface KutsuService {
    List<KutsuReadDto> listAvoinKutsus(KutsuOrganisaatioOrder sortBy, Sort.Direction direction, boolean onlyOwnKutsus);

    long createKutsu(KutsuCreateDto dto);

    KutsuReadDto getKutsu(Long id);

    Kutsu deleteKutsu(long id);

    KutsuReadDto getByTemporaryToken(String temporaryToken);

    String createHenkilo(String temporaryToken, HenkiloCreateByKutsuDto henkiloCreateByKutsuDto);

    void updateIdentifierToKutsu(String temporaryToken, String identifier);
}
