package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.dto.MyonnettyKayttoOikeusDto;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioPalveluRooliDto;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.MyonnettyKayttoOikeusRyhmaTapahtuma;
import fi.vm.sade.kayttooikeus.repositories.criteria.KayttooikeusCriteria;

import java.time.LocalDate;
import java.util.List;

public interface MyonnettyKayttoOikeusRyhmaTapahtumaRepositoryCustom {
    List<Long> findMasterIdsByHenkilo(String henkiloOid);

    List<MyonnettyKayttoOikeusDto> findByHenkiloInOrganisaatio(String henkiloOid, String organisaatioOid);

    List<MyonnettyKayttoOikeusRyhmaTapahtuma> findByVoimassaLoppuPvmBefore(LocalDate voimassaLoppuPvm);

    /**
     * Hakee henkilön käyttöoikeudet annetun kriteerin perusteella. Henkilöllä on oltava aktiivinen organisaatio ja
     * käyttöoikeusryhmä. Jos henkilöllä on passivoituja ja ei-passivoituja organisaatioita tai käyttöoikeusryhmiä
     * nämä palautetaan myös.
     * @param criteria Hakukriteeri
     * @param limit Montako henkilöä palautetaan
     * @param offset Poikkeama henkilöinä
     * @return Kriteerin täyttävät henkilöt.
     */
    List<Henkilo> listCurrentKayttooikeusForHenkilo(KayttooikeusCriteria criteria, Long limit, Long offset);

    List<OrganisaatioPalveluRooliDto> findOrganisaatioPalveluRooliByOid(String oid);

}
