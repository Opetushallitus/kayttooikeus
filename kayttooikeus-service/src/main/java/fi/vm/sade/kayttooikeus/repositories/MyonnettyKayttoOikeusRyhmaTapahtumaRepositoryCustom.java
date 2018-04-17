package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.dto.AccessRightTypeDto;
import fi.vm.sade.kayttooikeus.dto.GroupTypeDto;
import fi.vm.sade.kayttooikeus.dto.KayttooikeusPerustiedotDto;
import fi.vm.sade.kayttooikeus.dto.MyonnettyKayttoOikeusDto;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioPalveluRooliDto;
import fi.vm.sade.kayttooikeus.model.MyonnettyKayttoOikeusRyhmaTapahtuma;
import fi.vm.sade.kayttooikeus.repositories.criteria.KayttooikeusCriteria;

import java.time.LocalDate;
import java.util.List;

public interface MyonnettyKayttoOikeusRyhmaTapahtumaRepositoryCustom {
    List<Long> findMasterIdsByHenkilo(String henkiloOid);

    List<MyonnettyKayttoOikeusDto> findByHenkiloInOrganisaatio(String henkiloOid, String organisaatioOid);

    List<AccessRightTypeDto> findValidAccessRightsByOid(String oid);

    List<GroupTypeDto> findValidGroupsByHenkilo(String oid);

    List<MyonnettyKayttoOikeusRyhmaTapahtuma> findByVoimassaLoppuPvmBefore(LocalDate voimassaLoppuPvm);

    List<KayttooikeusPerustiedotDto> listCurrentKayttooikeusForHenkilo(KayttooikeusCriteria criteria, Long limit, Long offset);

    List<OrganisaatioPalveluRooliDto> findOrganisaatioPalveluRooliByOid(String oid);

}
