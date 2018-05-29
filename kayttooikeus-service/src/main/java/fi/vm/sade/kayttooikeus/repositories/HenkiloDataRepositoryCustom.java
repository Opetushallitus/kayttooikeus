package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.dto.HenkiloLinkitysDto;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import java.util.Optional;

public interface HenkiloDataRepositoryCustom {

    Optional<Henkilo> findByKayttajatiedotUsername(String kayttajatunnus);

    HenkiloLinkitysDto findLinkityksetByOid(String oidHenkilo);
}
