package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.model.Anomus;
import fi.vm.sade.kayttooikeus.model.Kutsu;
import fi.vm.sade.kayttooikeus.repositories.dto.ExpiringKayttoOikeusDto;
import fi.vm.sade.kayttooikeus.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;

@Service
public class TaskExecutorServiceImpl implements TaskExecutorService {
    private static final Logger logger = LoggerFactory.getLogger(TaskExecutorServiceImpl.class);

    private final KayttoOikeusService kayttoOikeusService;
    private final KayttooikeusAnomusService anomusService;
    private final KutsuService kutsuService;
    private final EmailService emailService;

    @Autowired
    public TaskExecutorServiceImpl(KayttoOikeusService kayttoOikeusService,
                                   KayttooikeusAnomusService anomusService,
                                   KutsuService kutsuService,
                                   EmailService emailService) {
        this.kayttoOikeusService = kayttoOikeusService;
        this.anomusService = anomusService;
        this.kutsuService = kutsuService;
        this.emailService = emailService;
    }

    @Override
    @SuppressWarnings("TransactionalAnnotations")
    // non transactional for possible future feature of sending each message in a separate transaction
    // and marking that up in the db (allowing search by range rather than simple date and retries)
    public int sendExpirationReminders(Period... expireThresholds) {
        int remindersSent = 0;
        for (Map.Entry<String, List<ExpiringKayttoOikeusDto>> tapahtumasByHenkilo : kayttoOikeusService
                .findToBeExpiringMyonnettyKayttoOikeus(LocalDate.now(), expireThresholds)
                .stream().collect(groupingBy(ExpiringKayttoOikeusDto::getHenkiloOid, toList()))
                .entrySet()) {
            try {
                emailService.sendExpirationReminder(tapahtumasByHenkilo.getKey(), tapahtumasByHenkilo.getValue());
                ++remindersSent;
            } catch (Exception e) {
                logger.error("Failed to send expiration reminder for "
                        + "henkiloOid=" + tapahtumasByHenkilo.getKey()
                        + " tapahtumas=[" + tapahtumasByHenkilo.getValue().stream()
                        .map(ExpiringKayttoOikeusDto::toString).collect(joining(", ")) + "]"
                        + ": reason: " + e.getMessage(), e);
            }
        }
        return remindersSent;
    }

    @Override
    @Transactional
    public void discardExpiredInvitations(Period threshold) {
        Collection<Kutsu> invitations = kutsuService.findExpiredInvitations(threshold);
        logger.info("Discarding {} expired invitations", invitations.size());
        Map<Boolean, Integer> result = invitations.stream()
                .map(this::discardInvitation)
                .collect(groupingBy(Boolean::booleanValue, summingInt(success -> 1)));
        if (!result.isEmpty()) {
            logger.info("Sent discarded invitation notifications. {} success, {} failures",
                    result.getOrDefault(true, 0),
                    result.getOrDefault(false, 0));
        }
        if (result.containsKey(false)) {
            logger.error("There were errors while discarding invitations, please check the logs");
        }
    }

    private boolean discardInvitation(Kutsu invitation) {
        try {
            kutsuService.discardInvitation(invitation);
            emailService.sendDiscardedInvitationNotification(invitation);
        } catch (Exception e) {
            logger.warn("Error while discarding invitation {}", invitation.getId(), e);
            return false;
        }
        return true;
    }

    @Override
    @Transactional
    public void discardExpiredApplications(Period threshold) {
        Collection<Anomus> applications = anomusService.findExpiredApplications(threshold);
        logger.info("Discarding {} expired applications", applications.size());
        Map<Boolean, Integer> result = applications.stream()
                .map(this::discardApplication)
                .collect(groupingBy(Boolean::booleanValue, summingInt(success -> 1)));
        if (!result.isEmpty()) {
            logger.info("Application discard process finished. {} success, {} failures",
                    result.getOrDefault(true, 0),
                    result.getOrDefault(false, 0));
        }
        if (result.containsKey(false)) {
            logger.error("There were errors while discarding applications, please check the logs");
        }
    }

    private boolean discardApplication(Anomus application) {
        try {
            anomusService.discardApplication(application);
            emailService.sendDiscardedApplicationNotification(application);
        } catch (Exception e) {
            logger.warn("Error while discarding application {}", application.getId(), e);
            return false;
        }
        return true;
    }
}
