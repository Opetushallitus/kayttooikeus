package fi.vm.sade.kayttooikeus.aspects;

import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.auditlog.Target;
import fi.vm.sade.kayttooikeus.dto.KayttoOikeusCreateDto;
import fi.vm.sade.kayttooikeus.dto.KayttoOikeusRyhmaModifyDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KayttoOikeusRyhmaHelper {

    private final AuditLogger auditLogger;

    void logCreateKayttooikeusryhma(KayttoOikeusRyhmaModifyDto ryhma, Object result) {
        Target target = new Target.Builder()
                .setField("id", String.valueOf(result))
                .build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.CREATE_KAYTTOOIKEUSRYHMA, target, changes);
    }

    void logCreateKayttooikeus(KayttoOikeusCreateDto kayttoOikeus, Object result) {
        Target target = new Target.Builder()
                .setField("id", String.valueOf(result))
                .build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.CREATE_KAYTTOIKEUS, target, changes);
    }

    void logUpdateKayttoOikeusForKayttoOikeusRyhma(long id, KayttoOikeusRyhmaModifyDto ryhmaData, Object result) {
        Target target = new Target.Builder()
                .setField("id", String.valueOf(id))
                .build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.UPDATE_KAYTTOOIKEUSRYHMA, target, changes);
    }


}
