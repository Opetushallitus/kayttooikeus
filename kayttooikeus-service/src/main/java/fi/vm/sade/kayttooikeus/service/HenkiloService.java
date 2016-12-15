package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.OrganisaatioOidsSearchDto;

import java.util.List;

public interface HenkiloService {
    List<String> findHenkilos(OrganisaatioOidsSearchDto organisaatioOidsSearchDto);

    String getUsernameByOidHenkilo(String oidHenkilo);
}
