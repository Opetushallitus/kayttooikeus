package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.service.HenkiloCacheService;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloHakuPerustietoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HenkiloCacheServiceImpl implements HenkiloCacheService {

    private final OppijanumerorekisteriClient oppijanumerorekisteriClient;
    private final HenkiloDataRepository henkiloDataRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean saveAll(long offset, long count, List<String> oidHenkiloList) {
        final List<Henkilo> saveList = new ArrayList<>();
        final List<HenkiloHakuPerustietoDto> onrHenkilohakuResultDto
                = this.oppijanumerorekisteriClient.getAllByOids(offset, count, oidHenkiloList);
        final List<Henkilo> matchingHenkiloList = this.henkiloDataRepository.findByOidHenkiloIn(
                onrHenkilohakuResultDto.stream().map(HenkiloHakuPerustietoDto::getOidHenkilo).collect(Collectors.toList()));

        onrHenkilohakuResultDto.forEach(henkiloHakuDto -> {
            // Find or create matching henkilo. Henkilo might not exist after kayttooikeus has separate database.
            Henkilo matchingHenkilo = matchingHenkiloList.stream()
                    .filter(henkilo -> henkilo.getOidHenkilo().equals(henkiloHakuDto.getOidHenkilo()))
                    .findFirst()
                    .orElseGet(() -> this.henkiloDataRepository.save(new Henkilo(henkiloHakuDto.getOidHenkilo())));
            matchingHenkilo.setEtunimetCached(henkiloHakuDto.getEtunimet());
            matchingHenkilo.setSukunimiCached(henkiloHakuDto.getSukunimi());
            matchingHenkilo.setKutsumanimiCached(henkiloHakuDto.getKutsumanimi());
            matchingHenkilo.setDuplicateCached(henkiloHakuDto.getDuplicate());
            matchingHenkilo.setPassivoituCached(henkiloHakuDto.getPassivoitu());
            matchingHenkilo.setHetuCached(henkiloHakuDto.getHetu());
            saveList.add(matchingHenkilo);
        });
        this.henkiloDataRepository.save(saveList);
        log.info(saveList.size() + " henkilöä tallennettiin cacheen");
        return onrHenkilohakuResultDto.isEmpty() || onrHenkilohakuResultDto.size() < count;
    }
}
