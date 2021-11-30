package fi.vm.sade.kayttooikeus.config.scheduling;

import com.github.kagkarlsson.scheduler.task.Daily;
import fi.vm.sade.kayttooikeus.config.properties.KayttooikeusProperties;
import fi.vm.sade.kayttooikeus.model.Kutsu;
import fi.vm.sade.kayttooikeus.service.EmailService;
import fi.vm.sade.kayttooikeus.service.KutsuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.Period;

@Component
public class DiscardExpiredInvitationsTask extends AbstractExpiringEntitiesTask<Kutsu> {

    private static final String ENTITY_NAME = "invitation";

    @Autowired
    public DiscardExpiredInvitationsTask(KayttooikeusProperties kayttooikeusProperties, KutsuService service, EmailService emailService) {
        super(ENTITY_NAME, new Daily(LocalTime.of(
                        kayttooikeusProperties.getScheduling().getConfiguration().getPurgeExpiredApplicationsHour(),
                        kayttooikeusProperties.getScheduling().getConfiguration().getPurgeExpiredApplicationsMinute())),
                service, emailService, Period.ofMonths(kayttooikeusProperties.getScheduling().getConfiguration().getExpirationThreshold()));
    }

    @Override
    public void sendNotification(Kutsu invitation) {
        emailService.sendDiscardNotification(invitation);
    }
}
