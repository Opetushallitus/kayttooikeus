package fi.vm.sade.kayttooikeus.aspects;

import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.auditlog.Target;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganisaatioHelper {

    private final AuditLogger auditLogger;

    void logUpdateOrganisationCache() {
        Target target = new Target.Builder()
                .build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.UPDATE_ORGANISAATIO_CACHE, target, changes);
    }

}
