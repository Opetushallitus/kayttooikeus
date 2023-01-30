package fi.vm.sade.kayttooikeus.config.scheduling;


import com.github.kagkarlsson.scheduler.Scheduler;
import fi.vm.sade.kayttooikeus.config.properties.KayttooikeusProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Ajastuksen aktivointi.
 *
 * *Task-luokat sisältävät ajastusten konfiguroinnit
 */
@Configuration
@ConditionalOnProperty(name = "kayttooikeus.scheduling.enabled", matchIfMissing = true)
@RequiredArgsConstructor
public class SchedulingClusterConfiguration {
    private final KayttooikeusProperties kayttooikeusProperties;

    @Bean(destroyMethod = "stop")
    Scheduler scheduler(@Qualifier("dataSource") DataSource dataSource,
                        LahetaUusienAnomuksienIlmoituksetTask lahetaUusienAnomuksienIlmoituksetTask,
                        PoistaVanhentuneetKayttooikeudetTask poistaVanhentuneetKayttooikeudetTask,
                        KasitteleOrganisaatioLakkautusTask kasitteleOrganisaatioLakkautusTask,
                        SendExpirationRemindersTask sendExpirationRemindersTask,
                        CasClientSessionCleanerTask casClientSessionCleanerTask,
                        UpdateHenkiloNimiCacheTask updateHenkiloNimiCacheTask,
                        DiscardExpiredInvitationsTask discardExpiredInvitationsTask,
                        DiscardExpiredApplicationsTask discardExpiredApplicationsTask,
                        IdentificationCleanupTask identificationCleanupTask,
                        DisableInactiveServiceUsersTask disableInactiveServiceUsersTask) { // NOSONAR
        Scheduler scheduler = Scheduler.create(dataSource)
                .startTasks(lahetaUusienAnomuksienIlmoituksetTask,
                        poistaVanhentuneetKayttooikeudetTask,
                        kasitteleOrganisaatioLakkautusTask,
                        sendExpirationRemindersTask,
                        casClientSessionCleanerTask,
                        updateHenkiloNimiCacheTask,
                        discardExpiredInvitationsTask,
                        discardExpiredApplicationsTask,
                        identificationCleanupTask,
                        disableInactiveServiceUsersTask)
                .threads(this.kayttooikeusProperties.getScheduling().getPool_size())
                .build();
        scheduler.start();
        return scheduler;
    }
}
