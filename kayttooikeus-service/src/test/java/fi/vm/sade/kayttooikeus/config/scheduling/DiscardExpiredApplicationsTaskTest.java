package fi.vm.sade.kayttooikeus.config.scheduling;

import fi.vm.sade.kayttooikeus.config.properties.KayttooikeusProperties;
import fi.vm.sade.kayttooikeus.model.Anomus;
import fi.vm.sade.kayttooikeus.service.EmailService;
import fi.vm.sade.kayttooikeus.service.KayttooikeusAnomusService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

import java.time.Period;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DiscardExpiredApplicationsTaskTest {

    private KayttooikeusProperties kayttooikeusProperties;

    private DiscardExpiredApplicationsTask discardExpiredApplicationsTask;

    private KayttooikeusAnomusService service;

    private EmailService emailService;

    @Before
    public void setUp() {
        kayttooikeusProperties = mock(KayttooikeusProperties.class, Answers.RETURNS_DEEP_STUBS);
        service = mock(KayttooikeusAnomusService.class, Answers.RETURNS_DEEP_STUBS);
        emailService = mock(EmailService.class, Answers.RETURNS_DEEP_STUBS);
        discardExpiredApplicationsTask = new DiscardExpiredApplicationsTask(kayttooikeusProperties, service, emailService);
    }

    @Test
    public void executeEmpty() {
        when(service.findExpired(any(Period.class))).thenReturn(Collections.emptyList());
        discardExpiredApplicationsTask.execute(null, null);
        verify(service, times(1)).findExpired(any(Period.class));
    }

    @Test
    public void executeSuccess() {
        Anomus application = mock(Anomus.class, Answers.RETURNS_DEEP_STUBS);
        when(service.findExpired(any(Period.class))).thenReturn(Collections.singletonList(application));
        discardExpiredApplicationsTask.execute(null, null);
        verify(service, times(1)).discard(application);
        verify(emailService, times(1)).sendDiscardNotification(application);
    }

    @Test
    public void executeFailure() {
        Anomus application = mock(Anomus.class, Answers.RETURNS_DEEP_STUBS);
        when(service.findExpired(any(Period.class))).thenReturn(Collections.singletonList(application));
        doThrow(new RuntimeException()).when(emailService).sendDiscardNotification(application);
        discardExpiredApplicationsTask.execute(null, null);
        verify(service, times(1)).discard(application);
        verify(emailService, times(1)).sendDiscardNotification(application);
    }
}
