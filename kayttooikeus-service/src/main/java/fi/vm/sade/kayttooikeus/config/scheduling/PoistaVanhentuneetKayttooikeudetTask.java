package fi.vm.sade.kayttooikeus.config.scheduling;

import com.github.kagkarlsson.scheduler.task.Daily;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.RecurringTask;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.config.properties.KayttooikeusProperties;
import fi.vm.sade.kayttooikeus.dto.KayttoOikeudenTila;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.service.MyonnettyKayttoOikeusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Optional;

/**
 *
 * @see SchedulingClusterConfiguration ajastuksen aktivointi
 */
@Slf4j
@Component
public class PoistaVanhentuneetKayttooikeudetTask extends RecurringTask {
    private final CommonProperties commonProperties;
    private final MyonnettyKayttoOikeusService myonnettyKayttoOikeusService;
    private final HenkiloDataRepository henkiloDataRepository;

    @Autowired
    public PoistaVanhentuneetKayttooikeudetTask(KayttooikeusProperties kayttooikeusProperties,
                                                MyonnettyKayttoOikeusService myonnettyKayttoOikeusService,
                                                HenkiloDataRepository henkiloDataRepository,
                                                CommonProperties commonProperties) {
        super("poista vanhentuneet kayttooikeudet task",
                new Daily(LocalTime.of(kayttooikeusProperties.getScheduling().getConfiguration().getVanhentuneetkayttooikeudetHour(), 0)));
        this.myonnettyKayttoOikeusService = myonnettyKayttoOikeusService;
        this.henkiloDataRepository = henkiloDataRepository;
        this.commonProperties = commonProperties;
    }

    @Override
    public void execute(TaskInstance<Void> taskInstance, ExecutionContext executionContext) {
        Optional<Henkilo> kasittelija = henkiloDataRepository.findByOidHenkilo(commonProperties.getAdminOid());
        this.myonnettyKayttoOikeusService.poistaVanhentuneet(new MyonnettyKayttoOikeusService.DeleteDetails(
                kasittelija.get(), KayttoOikeudenTila.VANHENTUNUT, "Oikeuksien poisto, vanhentunut"));
    }
}
