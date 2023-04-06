package fi.vm.sade.kayttooikeus.aspects;

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.auditlog.Target;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotCreateDto;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotUpdateDto;
import fi.vm.sade.kayttooikeus.repositories.dto.HenkiloCreateByKutsuDto;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class HenkiloHelper {

    private final AuditLogger auditLogger;

    public void logPassivoiHenkilo(String henkiloOid, String kasittelijaOid, Object returnHenkilo) {
        Target target = new Target.Builder()
                .setField("oid", henkiloOid)
                .build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.PASSIVOI_HENKILO, target, changes);
    }

    void logChangePassword(String henkiloOid, String password, Object result) {
        Target target = new Target.Builder()
                .setField("oid", henkiloOid)
                .build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.CHANGE_PASSWORD, target, changes);
    }

    void logUpdateHakaTunnisteet(String henkiloOid, String ipdKey, Set<String> hakatunnisteet, Object result) {
        Target target = new Target.Builder()
                .setField("oid", henkiloOid)
                .build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.UPDATE_HAKATUNNISTEET, target, changes);
    }

    void logCreateKayttajatiedot(String henkiloOid, KayttajatiedotCreateDto kayttajatiedot, Object result) {
        Target target = new Target.Builder()
                .setField("oid", henkiloOid)
                .build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.CREATE_KAYTTAJATIEDOT, target, changes);
    }

    void logUpdateKayttajatiedot(String henkiloOid, KayttajatiedotUpdateDto kayttajatiedot, Object result) {
        Target target = new Target.Builder()
                .setField("oid", henkiloOid)
                .build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.UPDATE_KAYTTAJATIEDOT, target, changes);
    }

    void logCreateHenkilo(String temporaryToken, HenkiloCreateByKutsuDto henkiloCreateByKutsuDto, Object result) throws JsonProcessingException {
        Target.Builder targetBuilder = new Target.Builder();
        Optional.ofNullable(result)
                .filter(HenkiloUpdateDto.class::isInstance)
                .map(HenkiloUpdateDto.class::cast)
                .map(HenkiloUpdateDto::getOidHenkilo)
                .ifPresent(oid -> targetBuilder.setField("oid", oid));
        Target target = targetBuilder.build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.CREATE_HENKILO_BY_KUTSU, target, changes);
    }

    public void logEnableGauthMfa(String henkiloOid) {
        Target target = new Target.Builder()
                .setField("oid", henkiloOid)
                .build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.ENABLE_MFA_GAUTH, target, changes);
    }

    public void logDisableGauthMfa(String henkiloOid) {
        Target target = new Target.Builder()
                .setField("oid", henkiloOid)
                .build();
        Changes changes = new Changes.Builder()
                .build();
        auditLogger.log(KayttooikeusOperation.DISABLE_MFA_GAUTH, target, changes);
    }
}
