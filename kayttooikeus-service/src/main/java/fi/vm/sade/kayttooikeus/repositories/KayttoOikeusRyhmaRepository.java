package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.dto.KayttoOikeusRyhmaDto;
import com.querydsl.core.Tuple;
import fi.vm.sade.kayttooikeus.model.KayttoOikeusRyhma;

import java.util.List;
import java.util.Optional;

public interface KayttoOikeusRyhmaRepository {
    List<KayttoOikeusRyhmaDto> findByIdList(List<Long> idList);

    Optional<KayttoOikeusRyhma> findByRyhmaId(Long id);

    Boolean ryhmaNameFiExists(String ryhmaNameFi);

    List<KayttoOikeusRyhmaDto> listAll();

    List<Tuple> findOrganisaatioOidAndRyhmaIdByHenkiloOid(String oid);

}
