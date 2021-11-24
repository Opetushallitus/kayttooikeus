package fi.vm.sade.kayttooikeus.config.scheduling;

import fi.vm.sade.kayttooikeus.config.properties.KayttooikeusProperties;
import fi.vm.sade.kayttooikeus.service.TaskExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DiscardExpiredInvitationsTaskTest {

    private TaskExecutorService taskExecutorService;

    private KayttooikeusProperties kayttooikeusProperties;

    private DiscardExpiredInvitationsTask discardExpiredInvitationsTask;

    @Before
    public void setUp() {
        taskExecutorService = mock(TaskExecutorService.class);
        kayttooikeusProperties = mock(KayttooikeusProperties.class, Answers.RETURNS_DEEP_STUBS);
        discardExpiredInvitationsTask = new DiscardExpiredInvitationsTask(kayttooikeusProperties, taskExecutorService);
    }

    @Test
    public void execute() {
        discardExpiredInvitationsTask.execute(null, null);
        verify(taskExecutorService, times(1)).discardExpiredInvitations(any());
    }
}
