package fi.vm.sade.kayttooikeus.service.external;

import fi.vm.sade.kayttooikeus.service.dto.HenkiloVahvaTunnistusDto;
import fi.vm.sade.kayttooikeus.service.dto.HenkiloYhteystiedotDto;
import fi.vm.sade.oppijanumerorekisteri.dto.*;

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

    HenkiloDto getHenkiloByOid(String oid);

    Optional<HenkiloDto> findHenkiloByOid(String oid);

    Optional<HenkiloDto> getHenkiloByHetu(String hetu);

    Collection<HenkiloYhteystiedotDto> listYhteystiedot(HenkiloHakuCriteria criteria);

    String createHenkilo(HenkiloCreateDto henkiloCreateDto);

    void setStrongIdentifiedHetu(String oidHenkilo, HenkiloVahvaTunnistusDto henkiloVahvaTunnistusDto);

    void updateHenkilo(HenkiloUpdateDto henkiloUpdateDto);

    void yhdistaHenkilot(String oid, Collection<String> duplicateOids);

    /**
     * Palauttaa haetun käyttäjän omat tiedot
     * @param oidHenkilo käyttäjän henkilöoid
     * @return haetun käyttäjän omat tiedot
     */
    HenkiloOmattiedotDto getOmatTiedot(String oidHenkilo);
}
