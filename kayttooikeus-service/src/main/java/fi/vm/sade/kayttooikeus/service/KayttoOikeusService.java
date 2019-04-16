package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.repositories.criteria.KayttooikeusCriteria;
import fi.vm.sade.kayttooikeus.repositories.dto.ExpiringKayttoOikeusDto;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;

public interface KayttoOikeusService {
    KayttoOikeusDto findKayttoOikeusById(long kayttoOikeusId);

    List<KayttoOikeusRyhmaDto> listAllKayttoOikeusRyhmas(Boolean passiiviset);

    List<PalveluKayttoOikeusDto> listKayttoOikeusByPalvelu(String palveluName);

    List<KayttoOikeusHistoriaDto> listMyonnettyKayttoOikeusHistoriaForCurrentUser();

    List<KayttooikeusPerustiedotDto> listMyonnettyKayttoOikeusForUser(KayttooikeusCriteria criteria, Long limit, Long offset);

    List<ExpiringKayttoOikeusDto> findToBeExpiringMyonnettyKayttoOikeus(LocalDate at, Period... expirationPeriods);

    Map<String, List<Integer>> findKayttooikeusryhmatAndOrganisaatioByHenkiloOid(String henkiloOid);

    List<KayttoOikeusRyhmaDto> listPossibleRyhmasByOrganization(String organisaatioOid);

    /**
     * Listaa käyttöoikeusryhmät jotka käyttäjä voi myöntää annetulle henkilölle annettuun organisaatioon.
     * @param oid Henkilön jolle mahdolliset käyttöoikeudet voidaan myöntää
     * @param organisaatioOid Organisaatio johon mahdolliset käyttöoikeudet voidaan myöntää
     * @param currentUserOid Käyttäjä, joka myöntää oikeudet
     * @return Lista myönnettäviä käyttöoikeusryhmiä
     */
    List<MyonnettyKayttoOikeusDto> listMyonnettyKayttoOikeusRyhmasMergedWithHenkilos(String oid, String organisaatioOid, String currentUserOid);

    /**
     * Listaa kaikki käyttöoikeusryhmät, mukaanlukien vanhat oikeudet, annetulle henkilölle kyseiseen organisaatioon
     * @param oid Henkilön oid jonka oikeuksia palautetaan
     * @param organisaatioOid Organisaation oid johon rajataan palautettavat oikeudet
     * @return Lista käyttäjän oikeuksia
     */
    List<MyonnettyKayttoOikeusDto> listMyonnettyKayttoOikeusRyhmasByHenkiloAndOrganisaatio(String oid, String organisaatioOid);

    KayttoOikeusRyhmaDto findKayttoOikeusRyhma(long id, Boolean passiiviset);

    List<KayttoOikeusRyhmaDto> findSubRyhmasByMasterRyhma(long id);

    List<PalveluRooliDto> findPalveluRoolisByKayttoOikeusRyhma(long id);

    RyhmanHenkilotDto findHenkilotByKayttoOikeusRyhma(long id);

    long createKayttoOikeusRyhma(KayttoOikeusRyhmaModifyDto uusiRyhma);

    long createKayttoOikeus(KayttoOikeusCreateDto kayttoOikeus);

    void updateKayttoOikeusForKayttoOikeusRyhma(long id, KayttoOikeusRyhmaModifyDto ryhmaData);

    /**
     * Passivoi aktiivisen käyttöoikeusryhmän. Poistaa myönnetyt käyttöoikeudet tähän ryhmään.
     * @param id Katiivisen käyttöoikeusryhmän ID
     */
    void passivoiKayttooikeusryhma(long id);

    List<KayttoOikeusRyhmaDto> findKayttoOikeusRyhmasByKayttoOikeusList(Map<String, String> kayttoOikeusList);

    /**
     * Aktivoi passiivisen käyttöoikeusryhmän
     * @param id Passiivisen käyttöoikeusryhmän ID
     */
    void aktivoiKayttooikeusryhma(Long id);
}
