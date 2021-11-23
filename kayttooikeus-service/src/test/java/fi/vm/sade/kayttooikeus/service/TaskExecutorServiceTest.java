package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.TextGroupDto;
import fi.vm.sade.kayttooikeus.repositories.dto.ExpiringKayttoOikeusDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
public class TaskExecutorServiceTest extends AbstractServiceTest {
    @Autowired
    private TaskExecutorService taskExecutorService;

    @MockBean
    private KayttoOikeusService kayttoOikeusService;

    @MockBean
    private KutsuService kutsuService;

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
    public void purgeExpiredInvitations() {
        Period testPeriod = Period.ofMonths(2);
        given(kutsuService.findExpiredInvitations(testPeriod)).willReturn(List.of(1L, 2L));

        taskExecutorService.purgeExpiredInvitations(testPeriod);

        verify(kutsuService, times(2)).deleteKutsu(anyLong());
        verify(emailService, times(2)).sendPurgeNotification(any());
    }
}
