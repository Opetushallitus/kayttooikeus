package fi.vm.sade.kayttooikeus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.enumeration.OrderByHenkilohaku;
import fi.vm.sade.kayttooikeus.repositories.dto.HenkilohakuResultDto;
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

    /**
     * Poistaa henkilöltä käyttäjätunnuksen ja käyttöoikeudet sekä passivoi henkilön organisaatiot ja varmentajat.
     * @param henkiloOid passivoitavan oid
     * @param kasittelijaOid käsittelijän oid (jos null, käytetään nykyisen käyttäjän oidia)
     */
    void passivoi(String henkiloOid, String kasittelijaOid);

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

    void updateAnomusilmoitus(String oid, boolean anomusilmoitus);

    /**
     * Hakee henkilön linkitykset muihin henkilöihin käyttöoikeuspalvelussa
     * @param oid henkilön oid
     * @param showPassive
     * @return Henkilön kaikki linkitykset sisältävä dto
     */
    HenkiloLinkitysDto getLinkitykset(String oid, boolean showPassive);

    /**
     *  Korvike vanhalle /cas/myroles rajapinnalle.
     * @return Käyttäjän roolit plus lisätietoa
     */
    @Deprecated
    List<String> getMyRoles();

    /**
     * Korvike vanhalle /cas/me rajapinnalle.
     * @return Käyttäjän tietoja.
     */
    @Deprecated
    MeDto getMe() throws JsonProcessingException;
}
