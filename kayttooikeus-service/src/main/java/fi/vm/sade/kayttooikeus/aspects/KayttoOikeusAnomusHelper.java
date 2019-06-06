package fi.vm.sade.kayttooikeus.aspects;

import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.auditlog.Target;
import fi.vm.sade.kayttooikeus.dto.GrantKayttooikeusryhmaDto;
import fi.vm.sade.kayttooikeus.dto.KayttooikeusAnomusDto;
import fi.vm.sade.kayttooikeus.dto.UpdateHaettuKayttooikeusryhmaDto;
import lombok.RequiredArgsConstructor;
import org.joda.time.LocalDate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class KayttoOikeusAnomusHelper {

    private final AuditLogger auditLogger;

    void logApproveOrRejectKayttooikeusAnomus(UpdateHaettuKayttooikeusryhmaDto kayttooikeusryhma, Object result) {
        Target target = new Target.Builder()
                .setField("id", String.valueOf(kayttooikeusryhma.getId()))
                .build();
        Changes changes = new Changes.Builder()
                .added("tila", kayttooikeusryhma.getKayttoOikeudenTila())
                .build();
        auditLogger.log(KayttooikeusOperation.APPROVE_OR_REJECT_KAYTTOOIKEUSANOMUS, target, changes);
    }

    void logSendKayttooikeusAnomusNotification(LocalDate anottuPvm, Object result) {
        Target target = new Target.Builder()
                .build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.SEND_KAYTTOOIKEUSANOMUS_NOTIFICATION, target, changes);
    }

    void logCancelKayttooikeusAnomus(Long kayttooikeusRyhmaId, Object result) {
        Target target = new Target.Builder()
                .setField("id", String.valueOf(kayttooikeusRyhmaId))
                .build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.REMOVE_USER_FROM_KAYTTOOIKEUSANOMUS, target, changes);
    }

    void logCreateKayttooikeusAnomus(String anojaOid, KayttooikeusAnomusDto kayttooikeusAnomusDto, Object result) {
        Target target = new Target.Builder()
                .setField("henkiloOid", anojaOid)
                .setField("organisaatioOid", kayttooikeusAnomusDto.getOrganisaatioOrRyhmaOid())
                .build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.CREATE_KAYTTOOIKEUSANOMUS, target, changes);
    }

    void logGrantKayttooikeusryhma(String anojaOid, String organisaatioOid, List<GrantKayttooikeusryhmaDto> updateHaettuKayttooikeusryhmaDtoList, Object result) {
        Target target = new Target.Builder()
                .setField("henkiloOid", anojaOid)
                .setField("organisaatioOid", organisaatioOid)
                .build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.ADD_KAYTTOOIKEUSRYHMA_TO_HENKILO, target, changes);
    }

}
