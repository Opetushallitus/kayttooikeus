package fi.vm.sade.kayttooikeus.aspects;

import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.auditlog.Target;
import fi.vm.sade.kayttooikeus.dto.KutsuCreateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KutsuHelper {

    private final AuditLogger auditLogger;

    void logCreateKutsu(KutsuCreateDto dto, Object result) {
        Target target = new Target.Builder()
                .setField("id", String.valueOf(result))
                .build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.CREATE_KUTSU, target, changes);
    }

    void logDeleteKutsu(Long id, Object result) {
        Target target = new Target.Builder()
                .setField("id", String.valueOf(id))
                .build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.DELETE_KUTSU, target, changes);
    }

}
