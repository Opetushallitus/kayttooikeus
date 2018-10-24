package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.repositories.criteria.OrganisaatioHenkiloCriteria;

import java.util.Collection;
import java.util.List;

public interface OrganisaatioHenkiloService {

    List<OrganisaatioHenkiloWithOrganisaatioDto> listOrganisaatioHenkilos(String henkiloOid, String compareByLang);

    List<OrganisaatioHenkiloWithOrganisaatioDto> listOrganisaatioHenkilos(String henkiloOid, String compareByLang, PalveluRooliGroup requiredRoles);

    List<KayttajaTyyppi> listPossibleHenkiloTypesAccessibleForCurrentUser();

    Collection<String> listOrganisaatioOidBy(OrganisaatioHenkiloCriteria criteria);

    OrganisaatioHenkiloDto findOrganisaatioHenkiloByHenkiloAndOrganisaatio(String henkiloOid, String organisaatioOid);

    List<OrganisaatioHenkiloDto> findOrganisaatioByHenkilo(String henkiloOid);

    /**
     * Lisää uudet organisaatiot henkilölle. Ei päivitä tai poista vanhoja
     * organisaatiotietoja.
     *
     * @param henkiloOid henkilö oid
     * @param organisaatioHenkilot henkilön organisaatiotiedot
     * @return kaikki henkilön organisaatiotiedot
     */
    List<OrganisaatioHenkiloDto> addOrganisaatioHenkilot(String henkiloOid, List<OrganisaatioHenkiloCreateDto> organisaatioHenkilot);

    List<OrganisaatioHenkiloDto> createOrUpdateOrganisaatioHenkilos(String henkiloOid,
                                                                    List<OrganisaatioHenkiloUpdateDto> organisaatioHenkiloDtoList);

    // Change organisaatiohenkilo passive and close all related myonnettykayttooikeusryhmatapahtumas
    void passivoiHenkiloOrganisation(String oidHenkilo, String henkiloOrganisationOid);


    // Passivoi organisaatiohenkilot joiden organisaatio on passivoitu ja poistaa näiltä organisaatiohenkilöiltä kaikki käyttöoikeudet
    void kasitteleOrganisaatioidenLakkautus(String kasittelijaOid);
}
