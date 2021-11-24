package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.TextGroupDto;
import fi.vm.sade.kayttooikeus.model.Anomus;
import fi.vm.sade.kayttooikeus.model.Kutsu;
import fi.vm.sade.kayttooikeus.repositories.dto.ExpiringKayttoOikeusDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
public class TaskExecutorServiceTest extends AbstractServiceTest {

    private static final Period TEST_PERIOD = Period.ofMonths(2);
    private static final Kutsu INVITATION = Mockito.mock(Kutsu.class);
    private static final Anomus APPLICATION = Mockito.mock(Anomus.class);

    @Autowired
    private TaskExecutorService taskExecutorService;

    @MockBean
    private KayttoOikeusService kayttoOikeusService;

    @MockBean
    private KutsuService kutsuService;

    @MockBean
    private KayttooikeusAnomusService anomusService;

    @MockBean
    private EmailService emailService;

    @Test
    public void sendExpirationRemindersTest() {
        given(kayttoOikeusService.findToBeExpiringMyonnettyKayttoOikeus(LocalDate.now(),
                Period.ofWeeks(3), Period.ofWeeks(2))).willReturn(asList(
                ExpiringKayttoOikeusDto.builder()
                        .henkiloOid("1.2.3.4.5")
                        .myonnettyTapahtumaId(1L)
                        .voimassaLoppuPvm(LocalDate.now().plusWeeks(3))
                        .ryhmaDescription(new TextGroupDto())
                        .ryhmaName("RYHMA")
                        .build(),
                ExpiringKayttoOikeusDto.builder()
                        .henkiloOid("1.2.3.4.5")
                        .myonnettyTapahtumaId(1L)
                        .voimassaLoppuPvm(LocalDate.now().plusWeeks(3))
                        .ryhmaDescription(new TextGroupDto())
                        .ryhmaName("RYHMA2")
                        .build()
        ));

        int numberSent = taskExecutorService.sendExpirationReminders(Period.ofWeeks(3), Period.ofWeeks(2));
        assertEquals(1, numberSent);
    }

    @Test
    public void discardExpiredInvitations() {
        given(kutsuService.findExpiredInvitations(TEST_PERIOD)).willReturn(List.of(INVITATION, INVITATION));

        taskExecutorService.discardExpiredInvitations(TEST_PERIOD);

        verify(kutsuService, times(2)).discardInvitation(INVITATION);
        verify(emailService, times(2)).sendDiscardedInvitationNotification(INVITATION);
    }

    @Test
    public void discardExpiredInvitationsNoInvitations() {
        given(kutsuService.findExpiredInvitations(TEST_PERIOD)).willReturn(Collections.emptyList());

        taskExecutorService.discardExpiredInvitations(TEST_PERIOD);

        verify(kutsuService, never()).discardInvitation(any());
        verify(emailService, never()).sendDiscardedInvitationNotification(any());
    }

    @Test
    public void discardExpiredInvitationsHandlesExceptions() {
        given(kutsuService.findExpiredInvitations(TEST_PERIOD)).willReturn(List.of(INVITATION));
        doThrow(new RuntimeException("Miserable failure")).when(kutsuService).discardInvitation(INVITATION);

        taskExecutorService.discardExpiredInvitations(TEST_PERIOD);

        verify(kutsuService, times(1)).discardInvitation(INVITATION);
        verify(emailService, never()).sendDiscardedInvitationNotification(any());
    }

    @Test
    public void discardExpiredApplications() {
        given(anomusService.findExpiredApplications(TEST_PERIOD)).willReturn(List.of(APPLICATION, APPLICATION));

        taskExecutorService.discardExpiredApplications(TEST_PERIOD);

        verify(anomusService, times(2)).discardApplication(APPLICATION);
        verify(emailService, times(2)).sendDiscardedApplicationNotification(APPLICATION);
    }

    @Test
    public void discardExpiredApplicationsNoApplications() {
        given(kutsuService.findExpiredInvitations(TEST_PERIOD)).willReturn(Collections.emptyList());

        taskExecutorService.discardExpiredInvitations(TEST_PERIOD);

        verify(anomusService, never()).discardApplication(any());
        verify(emailService, never()).sendDiscardedApplicationNotification(any());
    }

    @Test
    public void discardExpiredApplicationsHandlesExceptions() {
        given(anomusService.findExpiredApplications(TEST_PERIOD)).willReturn(List.of(APPLICATION));
        doThrow(new RuntimeException("Miserable failure")).when(anomusService).discardApplication(APPLICATION);

        taskExecutorService.discardExpiredApplications(TEST_PERIOD);

        verify(anomusService, times(1)).discardApplication(APPLICATION);
        verify(emailService, never()).sendDiscardedApplicationNotification(any());
    }
}
