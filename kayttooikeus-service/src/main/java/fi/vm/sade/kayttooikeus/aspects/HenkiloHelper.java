package fi.vm.sade.kayttooikeus.aspects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.sade.auditlog.Audit;
import fi.vm.sade.auditlog.kayttooikeus.KayttoOikeusLogMessage;
import fi.vm.sade.auditlog.kayttooikeus.KayttoOikeusOperation;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotCreateDto;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotUpdateDto;
import fi.vm.sade.kayttooikeus.repositories.dto.HenkiloCreateByKutsuDto;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class HenkiloHelper extends AbstractAuditlogAspectHelper {

    public HenkiloHelper(Audit audit, ObjectMapper mapper) {
        super(audit, mapper);
    }

    public void logPassivoiHenkilo(String henkiloOid, String kasittelijaOid, Object returnHenkilo) {
        KayttoOikeusLogMessage.LogMessageBuilder logMessage = KayttoOikeusLogMessage.builder()
                .kohdeTunniste(henkiloOid)
                .lisatieto("Henkilön kaikki organisaatiot ja käyttöoikeudet passivoitu.")
                .setOperaatio(KayttoOikeusOperation.PASSIVOI_HENKILO);
        finishLogging(logMessage);
    }

    void logChangePassword(String henkiloOid, String password, Object result) {
        KayttoOikeusLogMessage.LogMessageBuilder logMessage = KayttoOikeusLogMessage.builder()
                .kohdeTunniste(henkiloOid)
                .lisatieto("Henkilön salasana muutettu.")
                .setOperaatio(KayttoOikeusOperation.CHANGE_PASSWORD);
        finishLogging(logMessage);
    }

    void logUpdateHakaTunnisteet(String henkiloOid, String ipdKey, Set<String> hakatunnisteet, Object result) {
        KayttoOikeusLogMessage.LogMessageBuilder logMessage = KayttoOikeusLogMessage.builder()
                .kohdeTunniste(henkiloOid)
                .lisatieto("Henkilön hakatunnisteita muokattu.")
                .setOperaatio(KayttoOikeusOperation.UPDATE_HAKATUNNISTEET);
        finishLogging(logMessage);
    }

    void logCreateKayttajatiedot(String henkiloOid, KayttajatiedotCreateDto kayttajatiedot, Object result) {
        KayttoOikeusLogMessage.LogMessageBuilder logMessage = KayttoOikeusLogMessage.builder()
                .kohdeTunniste(henkiloOid)
                .lisatieto("Henkilölle luotu käyttäjätiedot.")
                .setOperaatio(KayttoOikeusOperation.CREATE_KAYTTAJATIEDOT);
        finishLogging(logMessage);
    }

    void logUpdateKayttajatiedot(String henkiloOid, KayttajatiedotUpdateDto kayttajatiedot, Object result) {
        KayttoOikeusLogMessage.LogMessageBuilder logMessage = KayttoOikeusLogMessage.builder()
                .kohdeTunniste(henkiloOid)
                .lisatieto("Henkilön käyttäjätietoja muokattu.")
                .setOperaatio(KayttoOikeusOperation.UPDATE_KAYTTAJATIEDOT);
        finishLogging(logMessage);
    }

    void logCreateHenkilo(String temporaryToken, HenkiloCreateByKutsuDto henkiloCreateByKutsuDto, Object result) throws JsonProcessingException {
        KayttoOikeusLogMessage.LogMessageBuilder logMessage = KayttoOikeusLogMessage.builder()
                .kohdeTunniste(temporaryToken)
                .lisatieto("Henkilön luonti.")
                .setOperaatio(KayttoOikeusOperation.CREATE_HENKILO_BY_KUTSU);
        finishLogging(logMessage);
    }
}
