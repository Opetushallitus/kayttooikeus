package fi.vm.sade.kayttooikeus.repositories;

import com.querydsl.core.Tuple;
import fi.vm.sade.kayttooikeus.model.KayttoOikeusRyhma;

import java.util.List;

public interface KayttoOikeusRyhmaRepository {
    List<KayttoOikeusRyhma> listAll();

    List<Tuple> findOrganisaatioOidAndRyhmaIdByHenkiloOid(String oid);

}
