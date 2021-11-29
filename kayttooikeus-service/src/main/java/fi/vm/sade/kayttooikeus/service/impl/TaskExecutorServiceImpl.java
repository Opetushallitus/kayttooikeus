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
import java.util.function.Consumer;
import java.util.function.Function;

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
        expire("invitation", kutsuService, discardInvitation()).accept(threshold);
    }

    @Override
    @Transactional
    public void discardExpiredApplications(Period threshold) {
        expire("application", anomusService, discardApplication()).accept(threshold);
    }

    private <T> Consumer<Period> expire(String name, ExpiringEntities<T> service, Function<T, Boolean> discard) {
        return threshold -> {
            Collection<T> entities = service.findExpired(threshold);
            logger.info("Discarding {} expired {}s", entities.size(), name);
            Map<Boolean, Integer> result = entities.stream()
                    .map(discard)
                    .collect(groupingBy(Boolean::booleanValue, summingInt(success -> 1)));
            if (!result.isEmpty()) {
                logger.info("Sent discarded {} notifications. {} success, {} failures",
                        name,
                        result.getOrDefault(true, 0),
                        result.getOrDefault(false, 0));
            }
            if (result.containsKey(false)) {
                logger.error("There were errors while discarding {}, please check the logs", name);
            }
        };
    }

    private Function<Kutsu, Boolean> discardInvitation() {
        return invitation -> {
            try {
                kutsuService.discard(invitation);
                emailService.sendDiscardedInvitationNotification(invitation);
            } catch (Exception e) {
                logger.warn("Error while discarding invitation {}", invitation.getId(), e);
                return false;
            }
            return true;
        };
    }

    private Function<Anomus, Boolean> discardApplication() {
        return application -> {
            try {
                anomusService.discard(application);
                emailService.sendDiscardedApplicationNotification(application);
            } catch (Exception e) {
                logger.warn("Error while discarding application {}", application.getId(), e);
                return false;
            }
            return true;
        };
    }
}
