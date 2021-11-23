package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.repositories.dto.ExpiringKayttoOikeusDto;
import fi.vm.sade.kayttooikeus.service.EmailService;
import fi.vm.sade.kayttooikeus.service.KayttoOikeusService;
import fi.vm.sade.kayttooikeus.service.KutsuService;
import fi.vm.sade.kayttooikeus.service.TaskExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;

@Service
public class TaskExecutorServiceImpl implements TaskExecutorService {
    private static final Logger logger = LoggerFactory.getLogger(TaskExecutorServiceImpl.class);

    private final KayttoOikeusService kayttoOikeusService;
    private final KutsuService kutsuService;
    private final EmailService emailService;

    @Autowired
    public TaskExecutorServiceImpl(KayttoOikeusService kayttoOikeusService,
                                   KutsuService kutsuService,
                                   EmailService emailService) {
        this.kayttoOikeusService = kayttoOikeusService;
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
    public void purgeExpiredInvitations(Period threshold) {
        Map<Boolean, Integer> result = kutsuService.findExpiredInvitations(threshold).stream()
                .map(id -> kutsuService.deleteKutsu(id))
                .map(invitation -> emailService.sendPurgeNotification(invitation))
                .collect(groupingBy(Boolean::booleanValue, summingInt(success -> 1)));
        if (!result.isEmpty()) {
            logger.info("Sent purge invitation notifications. {} success, {} failures",
                    result.getOrDefault(true, 0),
                    result.getOrDefault(false, 0));
        }
    }
}
