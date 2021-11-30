package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.KutsuCreateDto;
import fi.vm.sade.kayttooikeus.dto.KutsuReadDto;
import fi.vm.sade.kayttooikeus.dto.KutsuUpdateDto;
import fi.vm.sade.kayttooikeus.enumeration.KutsuOrganisaatioOrder;
import fi.vm.sade.kayttooikeus.model.Kutsu;
import fi.vm.sade.kayttooikeus.repositories.criteria.KutsuCriteria;
import fi.vm.sade.kayttooikeus.repositories.dto.HenkiloCreateByKutsuDto;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloUpdateDto;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface KutsuService extends ExpiringEntitiesService<Kutsu> {
    List<KutsuReadDto> listKutsus(KutsuOrganisaatioOrder sortBy, Sort.Direction direction, KutsuCriteria kutsuListCriteria, Long offset, Long amount);

    /**
     * Uuden kutsun luominen
     *
     * @param dto kutsun luomiseen
     * @return Luodun kutsun id
     */
    long createKutsu(KutsuCreateDto dto);

    /**
     * Kutsun hakeminen id:llä
     *
     * @param id kutsun id
     * @return kutsu halutulla id:llä
     */
    KutsuReadDto getKutsu(Long id);

    /**
     * Kutsun uusiminen muuttamatta kutsun sisältöä. Jos ei ole oma kutsu vaatii tavallisilta käyttäjiltä
     * authorisoinnin organisaatiohierarkian kautta.
     *
     * @param id kutsun ID
     */
    void renewKutsu(long id);

    /**
     * Merkitsee kutsun tilan poistetuksi. Ei fyysisesti poista mitään.
     *
     * @param id poistettavan kutsun id
     * @return poistetun kutsun id
     */
    Kutsu deleteKutsu(long id);

    /**
     * Palauttaa kutsun väliaikaisen kutsutokenin perusteella
     *
     * @param temporaryToken käyttäjän vahvan tunnistuksen jälkeen generoitu väliaikainen kutsutoken
     * @return tokenia vastaava kutsu
     */
    KutsuReadDto getByTemporaryToken(String temporaryToken);

    /**
     * Henkilön luominen väliaikaisella kutsutokenilla
     *
     * @param temporaryToken          token generoitu kutsulle vahvan tunnistuksen jälkeen
     * @param henkiloCreateByKutsuDto haluttu henkilö
     * @return Luodun henkilön oid
     */
    HenkiloUpdateDto createHenkilo(String temporaryToken, HenkiloCreateByKutsuDto henkiloCreateByKutsuDto);

    void addEmailToExistingHenkiloUpdateDto(String henkiloOid, String kutsuEmail, HenkiloUpdateDto henkiloUpdateDto);

    /**
     * Päivittää haka tunnisteen kutsuun
     *
     * @param temporaryToken käyttäjän vahvan tunnistuksen jälkeen generoitu väliaikainen kutsutoken
     * @param kutsuUpdateDto haka tunnisteen sisältävä dto
     */
    void updateHakaIdentifierToKutsu(String temporaryToken, KutsuUpdateDto kutsuUpdateDto);
}
