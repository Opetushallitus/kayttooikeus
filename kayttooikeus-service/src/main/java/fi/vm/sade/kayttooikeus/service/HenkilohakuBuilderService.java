package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.HenkilohakuCriteriaDto;
import fi.vm.sade.kayttooikeus.util.HenkilohakuBuilder;

public interface HenkilohakuBuilderService {

    HenkilohakuBuilder getBuilder(HenkilohakuCriteriaDto criteria);

}
