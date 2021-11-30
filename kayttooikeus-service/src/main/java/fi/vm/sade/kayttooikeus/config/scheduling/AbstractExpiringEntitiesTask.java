package fi.vm.sade.kayttooikeus.config.scheduling;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.RecurringTask;
import com.github.kagkarlsson.scheduler.task.Schedule;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import fi.vm.sade.kayttooikeus.model.Identifiable;
import fi.vm.sade.kayttooikeus.service.EmailService;
import fi.vm.sade.kayttooikeus.service.ExpiringEntitiesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Period;

public abstract class AbstractExpiringEntitiesTask<T extends Identifiable> extends RecurringTask implements ExpiringEntitiesTask<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractExpiringEntitiesTask.class);

    private final ExpiringEntitiesService<T> service;
    protected final EmailService emailService;
    private final String entityName;
    private final Period threshold;

    public AbstractExpiringEntitiesTask(String entityName, Schedule schedule, ExpiringEntitiesService<T> service, EmailService emailService, Period threshold) {
        super(String.format("expire-%ss-task", entityName), schedule);
        this.entityName = entityName;
        this.service = service;
        this.emailService = emailService;
        this.threshold = threshold;
    }

    @Override
    @Transactional
    public void execute(TaskInstance<Void> taskInstance, ExecutionContext executionContext) {
        expire(entityName, service, logger, threshold);
    }
}
