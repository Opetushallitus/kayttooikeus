package fi.vm.sade.kayttooikeus.config.scheduling;

import fi.vm.sade.kayttooikeus.config.properties.KayttooikeusProperties;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * Ajastuksen aktivointi.
 *
 * @see ScheduledTasks ajastusten konfigurointi
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "kayttooikeus.scheduling.run-on-startup", matchIfMissing = true)
public class SchedulingConfiguration implements SchedulingConfigurer {

    private final KayttooikeusProperties kayttooikeusProperties;

    private final OrganisaatioClient organisaatioClient;

    private ScheduledFuture organisaatioRetryTask;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor());
        this.onStartup(taskRegistrar.getScheduler());
    }

    private void onStartup(TaskScheduler taskScheduler) {
        this.organisaatioRetryTask = taskScheduler.scheduleWithFixedDelay(() -> {
            log.info("Aloitetaan organisaatiocachen päivitystä");
            this.organisaatioClient.refreshCache();
            this.organisaatioRetryTask.cancel(false);
        }, this.kayttooikeusProperties.getScheduling().getOrganisaatioRetryTime());
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService taskExecutor() {
        return Executors.newScheduledThreadPool(1);
    }

}
