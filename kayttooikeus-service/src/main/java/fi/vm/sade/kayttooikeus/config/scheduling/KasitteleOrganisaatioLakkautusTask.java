package fi.vm.sade.kayttooikeus.config.scheduling;


import com.github.kagkarlsson.scheduler.task.Daily;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.RecurringTask;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.config.properties.KayttooikeusProperties;
import fi.vm.sade.kayttooikeus.service.OrganisaatioHenkiloService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;


/**
 *
 * @see SchedulingClusterConfiguration ajastuksen aktivointi
 */
@Slf4j
@Component
public class KasitteleOrganisaatioLakkautusTask extends RecurringTask {

    private final OrganisaatioHenkiloService organisaatioHenkiloService;
    private final CommonProperties commonProperties;

    @Autowired
    public KasitteleOrganisaatioLakkautusTask(KayttooikeusProperties kayttooikeusProperties,
                                              CommonProperties commonProperties,
                                              OrganisaatioHenkiloService organisaatioHenkiloService) {
        super("passivoi organisaatiohenkilöt, joiden organisaatiot on passivoitu task",
                new Daily(LocalTime.of(kayttooikeusProperties.getScheduling().getConfiguration().getLakkautetutOrganisaatiotHour(), 0)));
        this.organisaatioHenkiloService = organisaatioHenkiloService;
        this.commonProperties = commonProperties;
    }

    @Override
    public void execute(TaskInstance<Void> taskInstance, ExecutionContext executionContext) {
        this.organisaatioHenkiloService.kasitteleOrganisaatioidenLakkautus(this.commonProperties.getAdminOid());
    }


}
