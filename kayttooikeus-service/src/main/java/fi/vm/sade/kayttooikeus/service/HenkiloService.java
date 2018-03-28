package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.HenkiloReadDto;
import fi.vm.sade.kayttooikeus.dto.HenkilohakuCriteriaDto;
import fi.vm.sade.kayttooikeus.dto.OmatTiedotDto;
import fi.vm.sade.kayttooikeus.enumeration.OrderByHenkilohaku;
import fi.vm.sade.kayttooikeus.repositories.dto.HenkilohakuResultDto;
import fi.vm.sade.kayttooikeus.dto.KayttooikeudetDto;
import fi.vm.sade.kayttooikeus.repositories.criteria.OrganisaatioHenkiloCriteria;

import java.util.Collection;
import java.util.List;

public interface HenkiloService {

    /**
     * Palauttaa henkilön tiedot OID:n perusteella.
     *
     * @param oid henkilön oid
     * @return henkilö
     */
    HenkiloReadDto getByOid(String oid);

    /**
     * Palauttaa henkilön tiedot käyttäjätunnuksen perusteella.
     *
     * @param kayttajatunnus käyttäjätunnus
     * @return henkilö
     */
    HenkiloReadDto getByKayttajatunnus(String kayttajatunnus);

    /**
     * Palauttaa henkilöiden oid:t joiden tietoihin annetulla henkilöllä on
     * oikeutus.
     *
     * @param henkiloOid henkilö oid jonka oikeutuksia tarkistetaan
     * @param criteria haun lisäehdot
     * @return sallitut henkilö oid:t
     */
    KayttooikeudetDto getKayttooikeudet(String henkiloOid, OrganisaatioHenkiloCriteria criteria);

    void disableHenkiloOrganisationsAndKayttooikeus(String henkiloOid, String kasittelijaOid);

    Collection<HenkilohakuResultDto> henkilohaku(HenkilohakuCriteriaDto henkilohakuCriteriaDto, Long offset, OrderByHenkilohaku orderBy);

    Long henkilohakuCount(HenkilohakuCriteriaDto henkilohakuCriteriaDto);

    boolean isVahvastiTunnistettu(String oidHenkilo);

    boolean isVahvastiTunnistettuByUsername(String username);

    void updateHenkiloToLdap(String oid, LdapSynchronizationService.LdapSynchronizationType ldapSynchronization);

    /**
     * UI:ta varten henkilön käyttöoikeuksien tutkimiseen
     * @return henkilön tiedot
     */
    OmatTiedotDto getOmatTiedot();
}
