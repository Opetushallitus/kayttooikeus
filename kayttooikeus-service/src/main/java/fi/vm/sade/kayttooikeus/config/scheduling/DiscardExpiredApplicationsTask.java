package fi.vm.sade.kayttooikeus.config.scheduling;

import com.github.kagkarlsson.scheduler.task.Daily;
import fi.vm.sade.kayttooikeus.config.properties.KayttooikeusProperties;
import fi.vm.sade.kayttooikeus.model.Anomus;
import fi.vm.sade.kayttooikeus.service.EmailService;
import fi.vm.sade.kayttooikeus.service.KayttooikeusAnomusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.Period;

@Component
public class DiscardExpiredApplicationsTask extends AbstractExpiringEntitiesTask<Anomus> {

    private static final String ENTITY_NAME = "application";

    @Autowired
    public DiscardExpiredApplicationsTask(KayttooikeusProperties kayttooikeusProperties, KayttooikeusAnomusService service, EmailService emailService) {
        super(ENTITY_NAME, new Daily(LocalTime.of(
                kayttooikeusProperties.getScheduling().getConfiguration().getPurgeExpiredApplicationsHour(),
                kayttooikeusProperties.getScheduling().getConfiguration().getPurgeExpiredApplicationsMinute())),
                service, emailService, Period.ofMonths(kayttooikeusProperties.getScheduling().getConfiguration().getExpirationThreshold()));
    }

    @Override
    public void sendNotification(Anomus application) {
        emailService.sendDiscardNotification(application);
    }
}


