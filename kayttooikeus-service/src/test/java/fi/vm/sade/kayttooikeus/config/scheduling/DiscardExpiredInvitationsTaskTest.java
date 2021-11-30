package fi.vm.sade.kayttooikeus.config.scheduling;

import fi.vm.sade.kayttooikeus.config.properties.KayttooikeusProperties;
import fi.vm.sade.kayttooikeus.model.Kutsu;
import fi.vm.sade.kayttooikeus.service.EmailService;
import fi.vm.sade.kayttooikeus.service.KutsuService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

import java.time.Period;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DiscardExpiredInvitationsTaskTest {

    private KayttooikeusProperties kayttooikeusProperties;

    private DiscardExpiredInvitationsTask discardExpiredInvitationsTask;

    private KutsuService service;

    private EmailService emailService;

    @Before
    public void setUp() {
        kayttooikeusProperties = mock(KayttooikeusProperties.class, Answers.RETURNS_DEEP_STUBS);
        service = mock(KutsuService.class, Answers.RETURNS_DEEP_STUBS);
        emailService = mock(EmailService.class, Answers.RETURNS_DEEP_STUBS);
        discardExpiredInvitationsTask = new DiscardExpiredInvitationsTask(kayttooikeusProperties, service, emailService);
    }

    @Test
    public void executeEmpty() {
        when(service.findExpired(any(Period.class))).thenReturn(Collections.emptyList());
        discardExpiredInvitationsTask.execute(null, null);
        verify(service, times(1)).findExpired(any(Period.class));
    }

    @Test
    public void executeSuccess() {
        Kutsu invitation = mock(Kutsu.class, Answers.RETURNS_DEEP_STUBS);
        when(service.findExpired(any(Period.class))).thenReturn(Collections.singletonList(invitation));
        discardExpiredInvitationsTask.execute(null, null);
        verify(service, times(1)).discard(invitation);
        verify(emailService, times(1)).sendDiscardNotification(invitation);
    }

    @Test
    public void executeFailure() {
        Kutsu invitation = mock(Kutsu.class, Answers.RETURNS_DEEP_STUBS);
        when(service.findExpired(any(Period.class))).thenReturn(Collections.singletonList(invitation));
        doThrow(new RuntimeException()).when(emailService).sendDiscardNotification(invitation);
        discardExpiredInvitationsTask.execute(null, null);
        verify(service, times(1)).discard(invitation);
        verify(emailService, times(1)).sendDiscardNotification(invitation);
    }
}
