package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.dto.MyonnettyKayttoOikeusDto;

import java.util.List;

public interface KayttoOikeusRyhmaTapahtumaHistoriaRepository {
    /**
     * Listaa henkilön vanhat oikeudet annettuun organisaatioon
     * @param henkiloOid Henkilön oid
     * @param organisaatioOid Organisaation oid
     * @return Lista vanhoja oikeuksia
     */
    List<MyonnettyKayttoOikeusDto> findByHenkiloInOrganisaatio(String henkiloOid, String organisaatioOid);
}
