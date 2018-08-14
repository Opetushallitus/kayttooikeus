package fi.vm.sade.kayttooikeus.service.external;

import fi.vm.sade.kayttooikeus.service.dto.HenkiloVahvaTunnistusDto;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloCreateDto;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloHakuPerustietoDto;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloUpdateDto;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.singletonList;

public interface OppijanumerorekisteriClient {

    List<HenkiloPerustietoDto> getHenkilonPerustiedot(Collection<String> henkiloOid);

    default Optional<HenkiloPerustietoDto> getHenkilonPerustiedot(String henkiloOid) {
        if (henkiloOid == null) {
            return Optional.empty();
        }
        return getHenkilonPerustiedot(singletonList(henkiloOid)).stream()
                .filter(h -> henkiloOid.equals(h.getOidHenkilo())).findFirst();
    }

    Set<String> getAllOidsForSamePerson(String personOid);

    String getOidByHetu(String hetu);

    List<HenkiloHakuPerustietoDto> getAllByOids(long page, long count, List<String> oidHenkiloList);

    List<String> getModifiedSince(LocalDateTime dateTime, long offset, long amount);

    HenkiloPerustietoDto getPerustietoByOid(String oidHenkilo);

    HenkiloDto getHenkiloByOid(String oid);

    Optional<HenkiloDto> getHenkiloByHetu(String hetu);

    Set<String> listOidByYhteystieto(String arvo);

    Optional<String> createHenkiloForKutsu(HenkiloCreateDto henkiloCreateDto);

    String createHenkilo(HenkiloCreateDto henkiloCreateDto);

    void setStrongIdentifiedHetu(String oidHenkilo, HenkiloVahvaTunnistusDto henkiloVahvaTunnistusDto);

    void updateHenkilo(HenkiloUpdateDto henkiloUpdateDto);

    void yhdistaHenkilot(String oid, Collection<String> duplicateOids);
}
