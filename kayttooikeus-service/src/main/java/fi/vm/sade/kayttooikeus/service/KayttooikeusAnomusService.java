package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.GrantKayttooikeusryhmaDto;
import fi.vm.sade.kayttooikeus.dto.HaettuKayttooikeusryhmaDto;
import fi.vm.sade.kayttooikeus.dto.KayttooikeusAnomusDto;
import fi.vm.sade.kayttooikeus.dto.UpdateHaettuKayttooikeusryhmaDto;
import fi.vm.sade.kayttooikeus.enumeration.OrderByAnomus;
import fi.vm.sade.kayttooikeus.model.Anomus;
import fi.vm.sade.kayttooikeus.model.KayttoOikeusRyhma;
import fi.vm.sade.kayttooikeus.repositories.criteria.AnomusCriteria;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface KayttooikeusAnomusService extends ExpiringEntitiesService<Anomus> {

    List<HaettuKayttooikeusryhmaDto> listHaetutKayttoOikeusRyhmat(String oidHenkilo, boolean activeOnly);

    List<HaettuKayttooikeusryhmaDto> listHaetutKayttoOikeusRyhmat(AnomusCriteria criteria, Long limit, Long offset, OrderByAnomus orderBy);

    /**
     * Myöntää tai hylkää haetun käyttöoikeusryhmän anomukselta. Myönnettäessä oikeus lisätään anomukselle myönnettyihin
     * oikeuksiin ja poistetaan anomukselta. Hylättäessä haettu käyttöoikeusryhmä vain poistetaan anomukselta.
     * @param updateHaettuKayttooikeusryhmaDto Haetun käyttöoikeusryhmän käsittelytiedot
     */
    void updateHaettuKayttooikeusryhma(UpdateHaettuKayttooikeusryhmaDto updateHaettuKayttooikeusryhmaDto);

    /**
     * Suora käyttöoikeuksien myöntäminen henkilölle haluttuun organisaatioon.
     * @param anojaOid Henkilön oid jolle oikeudet myönnetään
     * @param organisaatioOid Organisaation oid johon oikeudet myönnetään
     * @param updateHaettuKayttooikeusryhmaDtoList Myönnettävät oikeudet
     */
    void grantKayttooikeusryhma(String anojaOid, String organisaatioOid, List<GrantKayttooikeusryhmaDto> updateHaettuKayttooikeusryhmaDtoList);

    /**
     * Myöntää henkilölle suoraan käyttöoikeudet ilman tarkistuksia. Oletetaan, että myönnettävät oikeudet on jo validoitu
     * ja tätä funktiokutsua ei suorita kirjautunut käyttäjä.
     * @param anoja Henkilön oid jolle oikeudet myönnetään
     * @param organisaatioOid Organisaation oid johon oikeudet myönnetään
     * @param voimassaLoppuPvm Oikeuksien sulkeutumispäivämäärä
     * @param kayttooikeusryhmas Myönnettävät oikeudet
     * @param myontaja Henkilön oid joka katsotaan myöntäväksi henkilöksi
     */
    void grantPreValidatedKayttooikeusryhma(String anoja,
                                            String organisaatioOid,
                                            LocalDate voimassaLoppuPvm,
                                            Collection<KayttoOikeusRyhma> kayttooikeusryhmas,
                                            String myontaja);

    Long createKayttooikeusAnomus(String anojaOid, KayttooikeusAnomusDto kayttooikeusAnomusDto);

    void cancelKayttooikeusAnomus(Long kayttooikeusRyhmaId);

    void lahetaUusienAnomuksienIlmoitukset(LocalDate anottuPvm);

    void removePrivilege(String oidHenkilo, Long id, String organisaatioOid);

    Map<String, Set<Long>> findCurrentHenkiloCanGrant(String accessedHenkiloOid);
}
