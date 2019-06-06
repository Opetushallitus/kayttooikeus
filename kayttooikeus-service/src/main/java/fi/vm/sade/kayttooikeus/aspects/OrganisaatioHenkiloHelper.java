package fi.vm.sade.kayttooikeus.aspects;

import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.auditlog.Target;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloCreateDto;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.stream.Collectors.joining;

@Component
@RequiredArgsConstructor
public class OrganisaatioHenkiloHelper {

    private final AuditLogger auditLogger;

    void logCreateOrUpdateOrganisaatioHenkilo(String henkiloOid, List<OrganisaatioHenkiloUpdateDto> organisaatioHenkiloDtoList,
                                              Object result) {
        String organisaatioOids = organisaatioHenkiloDtoList.stream()
                .map(OrganisaatioHenkiloUpdateDto::getOrganisaatioOid)
                .collect(joining(";"));

        Target target = new Target.Builder()
                .setField("oid", henkiloOid)
                .build();
        Changes changes = new Changes.Builder()
                .added("organisaatioOids", organisaatioOids)
                .build();
        auditLogger.log(KayttooikeusOperation.CREATE_OR_UPDATE_ORGANISAATIO_HENKILO, target, changes);
    }

    void logFindOrCreateOrganisaatioHenkilot(String henkiloOid, List<OrganisaatioHenkiloCreateDto> organisaatioHenkilot, Object result) {
        String organisaatioOids = organisaatioHenkilot.stream()
                .map(OrganisaatioHenkiloCreateDto::getOrganisaatioOid)
                .collect(joining(";"));

        Target target = new Target.Builder()
                .setField("oid", henkiloOid)
                .build();
        Changes changes = new Changes.Builder()
                .added("organisaatioOids", organisaatioOids)
                .build();
        auditLogger.log(KayttooikeusOperation.FIND_OR_CREATE_ORGANISAATIO_HENKILOT, target, changes);
    }

    void logPassivoiOrganisaatioHenkilo(String oidHenkilo, String henkiloOrganisationOid, Object result) {
        Target target = new Target.Builder()
                .setField("henkiloOid", oidHenkilo)
                .setField("organisaatioOid", henkiloOrganisationOid)
                .build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.PASSIVOI_ORGANISAATIO_HENKILO, target, changes);
    }

}
