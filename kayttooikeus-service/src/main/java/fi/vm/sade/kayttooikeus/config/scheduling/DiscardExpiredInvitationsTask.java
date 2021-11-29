package fi.vm.sade.kayttooikeus.config.scheduling;

import com.github.kagkarlsson.scheduler.task.Daily;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.RecurringTask;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import fi.vm.sade.kayttooikeus.config.properties.KayttooikeusProperties;
import fi.vm.sade.kayttooikeus.service.TaskExecutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.Period;

@Component
public class DiscardExpiredInvitationsTask extends RecurringTask {

    private final Period expirationThreshold;

    private final TaskExecutorService taskExecutorService;

    @Autowired
    public DiscardExpiredInvitationsTask(KayttooikeusProperties kayttooikeusProperties, TaskExecutorService taskExecutorService) {
        super(DiscardExpiredInvitationsTask.class.getName(), new Daily(LocalTime.of(
                kayttooikeusProperties.getScheduling().getConfiguration().getPurgeExpiredInvitationsHour(),
                kayttooikeusProperties.getScheduling().getConfiguration().getPurgeExpiredInvitationsMinute())));
        this.expirationThreshold = Period.ofMonths(kayttooikeusProperties.getScheduling().getConfiguration().getExpirationThreshold());
        this.taskExecutorService = taskExecutorService;
    }

    @Override
    public void execute(TaskInstance<Void> taskInstance, ExecutionContext executionContext) {
        taskExecutorService.discardExpiredInvitations(expirationThreshold);
    }
}
