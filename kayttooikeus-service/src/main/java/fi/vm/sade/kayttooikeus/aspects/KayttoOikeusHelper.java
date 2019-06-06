package fi.vm.sade.kayttooikeus.aspects;

import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.auditlog.Target;
import fi.vm.sade.kayttooikeus.repositories.dto.ExpiringKayttoOikeusDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class KayttoOikeusHelper {

    private final AuditLogger auditLogger;

    /* Käyttöoikeus */
    void logSendKayttooikeusReminder(String henkiloOid, List<ExpiringKayttoOikeusDto> tapahtumas, Object result) {
        Target target = new Target.Builder()
                .setField("oid", henkiloOid)
                .build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.SEND_KAYTTOOIKEUS_EXPIRATION_REMINDER, target, changes);
    }

    /* Myönnetty Kayttooikeus */
    void logRemoveExpiredKayttooikeudet(String kasittelijaOid, Object result) {
        Target target = new Target.Builder()
                .build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.REMOVE_EXPIRED_KAYTTOOIKEUDET, target, changes);
    }

}
