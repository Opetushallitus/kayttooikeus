package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.UpdateHaettuKayttooikeusryhmaDto;
import fi.vm.sade.kayttooikeus.model.Anomus;
import fi.vm.sade.kayttooikeus.model.Kutsu;
import fi.vm.sade.kayttooikeus.repositories.dto.ExpiringKayttoOikeusDto;

import java.util.List;
import java.util.Set;

public interface EmailService {

    void sendEmailAnomusKasitelty(Anomus anomus, UpdateHaettuKayttooikeusryhmaDto updateHaettuKayttooikeusryhmaDto, Long kayttooikeusryhmaId);

    void sendExpirationReminder(String henkiloOid, List<ExpiringKayttoOikeusDto> tapahtumas);

    void sendNewRequisitionNotificationEmails(Set<String> henkiloOids);

    void sendInvitationEmail(Kutsu kutsu);

}
