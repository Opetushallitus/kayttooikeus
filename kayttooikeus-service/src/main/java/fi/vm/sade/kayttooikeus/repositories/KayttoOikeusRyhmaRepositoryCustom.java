package fi.vm.sade.kayttooikeus.repositories;

import com.querydsl.core.Tuple;
import fi.vm.sade.kayttooikeus.dto.KayttoOikeusRyhmaDto;
import fi.vm.sade.kayttooikeus.model.KayttoOikeusRyhma;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface KayttoOikeusRyhmaRepositoryCustom {
    List<KayttoOikeusRyhmaDto> findByIdList(Collection<Long> idList);

    Optional<KayttoOikeusRyhma> findByRyhmaId(Long id, boolean naytaPassivoitu);

    Boolean ryhmaNameFiExists(String ryhmaNameFi);

    List<KayttoOikeusRyhmaDto> listAll(boolean naytaPassivoidut);

    List<Tuple> findOrganisaatioOidAndRyhmaIdByHenkiloOid(String oid);

    List<KayttoOikeusRyhmaDto> findKayttoOikeusRyhmasByKayttoOikeusIds(List<Long> kayttoOikeusIds);
}
