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

    private static final Period EXPIRATION_THRESHOLD = Period.ofMonths(2);

    private final TaskExecutorService taskExecutorService;

    @Autowired
    public DiscardExpiredInvitationsTask(KayttooikeusProperties kayttooikeusProperties, TaskExecutorService taskExecutorService) {
        super(DiscardExpiredInvitationsTask.class.getName(), new Daily(LocalTime.of(
                kayttooikeusProperties.getScheduling().getConfiguration().getPurgeExpiredInvitationsHour(),
                kayttooikeusProperties.getScheduling().getConfiguration().getPurgeExpiredInvitationsMinute())));
        this.taskExecutorService = taskExecutorService;
    }

    @Override
    public void execute(TaskInstance<Void> taskInstance, ExecutionContext executionContext) {
        taskExecutorService.discardExpiredInvitations(EXPIRATION_THRESHOLD);
    }
}
