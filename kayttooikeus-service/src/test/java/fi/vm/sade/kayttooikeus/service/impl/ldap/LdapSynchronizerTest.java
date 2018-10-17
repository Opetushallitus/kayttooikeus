package fi.vm.sade.kayttooikeus.service.impl.ldap;

import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.LdapPriorityType;
import fi.vm.sade.kayttooikeus.model.LdapSynchronizationData;
import fi.vm.sade.kayttooikeus.model.LdapUpdateData;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.repositories.HenkiloHibernateRepository;
import fi.vm.sade.kayttooikeus.repositories.LdapUpdateDataCriteria;
import fi.vm.sade.kayttooikeus.repositories.LdapUpdateDataRepository;
import fi.vm.sade.kayttooikeus.service.TimeService;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

@RunWith(MockitoJUnitRunner.class)
public class LdapSynchronizerTest {

    private LdapSynchronizer ldapSynchronizer;

    @Mock
    private TimeService timeService;
    @Mock
    private LdapService ldapServiceMock;
    @Mock
    private LdapUpdateDataRepository ldapUpdateDataRepositoryMock;
    @Mock
    private HenkiloDataRepository henkiloRepositoryMock;
    @Mock
    private HenkiloHibernateRepository henkiloHibernateRepository;
    @Mock
    private OppijanumerorekisteriClient oppijanumerorekisteriClientMock;

    @Before
    public void setup() {
        when(oppijanumerorekisteriClientMock.findHenkiloByOid(any())).thenReturn(Optional.of(new HenkiloDto()));
        when(henkiloRepositoryMock.findByOidHenkilo(any())).thenReturn(Optional.of(new Henkilo()));
        ldapSynchronizer = new LdapSynchronizer(timeService, ldapServiceMock,
                ldapUpdateDataRepositoryMock, henkiloRepositoryMock,
                henkiloHibernateRepository,
                oppijanumerorekisteriClientMock);
    }

    @Test
    public void runShouldWorkAtNightTime() {
        Optional<LdapSynchronizationData> previous = Optional.empty();

        Optional<LdapSynchronizationData> next = ldapSynchronizer.run(previous, true, 10, 15);

        assertThat(next).isEmpty();
        ArgumentCaptor<LdapUpdateDataCriteria> criteriaCaptor = ArgumentCaptor.forClass(LdapUpdateDataCriteria.class);
        verify(ldapUpdateDataRepositoryMock, times(3)).findBy(criteriaCaptor.capture(), anyLong());
        List<LdapUpdateDataCriteria> criteria = criteriaCaptor.getAllValues();
        assertThat(criteria).extracting("priorities").containsExactly(
                singletonList(LdapPriorityType.ASAP),
                singletonList(LdapPriorityType.NIGHT),
                singletonList(LdapPriorityType.NORMAL));
    }

    @Test
    public void runShouldWorkAtDayTime() {
        Optional<LdapSynchronizationData> previous = Optional.empty();

        Optional<LdapSynchronizationData> next = ldapSynchronizer.run(previous, false, 10, 15);

        assertThat(next).isEmpty();
        ArgumentCaptor<LdapUpdateDataCriteria> criteriaCaptor = ArgumentCaptor.forClass(LdapUpdateDataCriteria.class);
        verify(ldapUpdateDataRepositoryMock, times(3)).findBy(criteriaCaptor.capture(), anyLong());
        List<LdapUpdateDataCriteria> criteria = criteriaCaptor.getAllValues();
        assertThat(criteria).extracting("priorities").containsExactly(
                singletonList(LdapPriorityType.ASAP),
                singletonList(LdapPriorityType.BATCH),
                singletonList(LdapPriorityType.NORMAL));
    }

    @Test
    public void runShouldHandleAsapOnly() {
        Optional<LdapSynchronizationData> previous = Optional.empty();
        when(ldapUpdateDataRepositoryMock.findBy(any(), any())).thenReturn(Arrays.asList(LdapUpdateData.builder().build()));

        Optional<LdapSynchronizationData> next = ldapSynchronizer.run(previous, true, 10, 15);

        assertThat(next).isNotEmpty();
        ArgumentCaptor<LdapUpdateDataCriteria> criteriaCaptor = ArgumentCaptor.forClass(LdapUpdateDataCriteria.class);
        verify(ldapUpdateDataRepositoryMock).findBy(criteriaCaptor.capture(), eq(10L));
        LdapUpdateDataCriteria criteria = criteriaCaptor.getValue();
        assertThat(criteria.getPriorities()).containsExactly(LdapPriorityType.ASAP);
    }

    @Test
    public void runShouldFallbackToNightPriorityAtNightTime() {
        Optional<LdapSynchronizationData> previous = Optional.empty();
        when(ldapUpdateDataRepositoryMock.findBy(argThat(isPriority(LdapPriorityType.NIGHT)), anyLong())).thenReturn(Arrays.asList(LdapUpdateData.builder().build()));

        Optional<LdapSynchronizationData> next = ldapSynchronizer.run(previous, true, 10, 15);

        assertThat(next).isNotEmpty();
        ArgumentCaptor<LdapUpdateDataCriteria> criteriaCaptor = ArgumentCaptor.forClass(LdapUpdateDataCriteria.class);
        verify(ldapUpdateDataRepositoryMock, times(2)).findBy(criteriaCaptor.capture(), anyLong());
        List<LdapUpdateDataCriteria> criteria = criteriaCaptor.getAllValues();
        assertThat(criteria).extracting("priorities").containsExactly(
                singletonList(LdapPriorityType.ASAP),
                singletonList(LdapPriorityType.NIGHT));
    }

    @Test
    public void runShouldFallbackToNormalPriorityAtNightTime() {
        Optional<LdapSynchronizationData> previous = Optional.empty();
        when(ldapUpdateDataRepositoryMock.findBy(argThat(isPriority(LdapPriorityType.NORMAL)), anyLong())).thenReturn(Arrays.asList(LdapUpdateData.builder().build()));

        Optional<LdapSynchronizationData> next = ldapSynchronizer.run(previous, true, 10, 15);

        assertThat(next).isNotEmpty();
        ArgumentCaptor<LdapUpdateDataCriteria> criteriaCaptor = ArgumentCaptor.forClass(LdapUpdateDataCriteria.class);
        verify(ldapUpdateDataRepositoryMock, times(3)).findBy(criteriaCaptor.capture(), anyLong());
        List<LdapUpdateDataCriteria> criteria = criteriaCaptor.getAllValues();
        assertThat(criteria).extracting("priorities").containsExactly(
                singletonList(LdapPriorityType.ASAP),
                singletonList(LdapPriorityType.NIGHT),
                singletonList(LdapPriorityType.NORMAL));
    }

    @Test
    public void runShouldFallbackToBatchPriorityAtDayTime() {
        Optional<LdapSynchronizationData> previous = Optional.empty();
        when(ldapUpdateDataRepositoryMock.findBy(argThat(isPriority(LdapPriorityType.BATCH)), anyLong())).thenReturn(Arrays.asList(LdapUpdateData.builder().build()));

        Optional<LdapSynchronizationData> next = ldapSynchronizer.run(previous, false, 10, 15);

        assertThat(next).isNotEmpty();
        ArgumentCaptor<LdapUpdateDataCriteria> criteriaCaptor = ArgumentCaptor.forClass(LdapUpdateDataCriteria.class);
        verify(ldapUpdateDataRepositoryMock, times(2)).findBy(criteriaCaptor.capture(), anyLong());
        List<LdapUpdateDataCriteria> criteria = criteriaCaptor.getAllValues();
        assertThat(criteria).extracting("priorities").containsExactly(
                singletonList(LdapPriorityType.ASAP),
                singletonList(LdapPriorityType.BATCH));
    }

    @Test
    public void runShouldFallbackToNormalPriorityAtDayTime() {
        Optional<LdapSynchronizationData> previous = Optional.empty();
        when(ldapUpdateDataRepositoryMock.findBy(argThat(isPriority(LdapPriorityType.NORMAL)), anyLong())).thenReturn(Arrays.asList(LdapUpdateData.builder().build()));

        Optional<LdapSynchronizationData> next = ldapSynchronizer.run(previous, false, 10, 15);

        assertThat(next).isNotEmpty();
        ArgumentCaptor<LdapUpdateDataCriteria> criteriaCaptor = ArgumentCaptor.forClass(LdapUpdateDataCriteria.class);
        verify(ldapUpdateDataRepositoryMock, times(3)).findBy(criteriaCaptor.capture(), anyLong());
        List<LdapUpdateDataCriteria> criteria = criteriaCaptor.getAllValues();
        assertThat(criteria).extracting("priorities").containsExactly(
                singletonList(LdapPriorityType.ASAP),
                singletonList(LdapPriorityType.BATCH),
                singletonList(LdapPriorityType.NORMAL));
    }

    private Matcher<LdapUpdateDataCriteria> isPriority(LdapPriorityType priority) {
        return new TypeSafeMatcher<LdapUpdateDataCriteria>() {
            @Override
            protected boolean matchesSafely(LdapUpdateDataCriteria item) {
                return item.getPriorities().contains(priority);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Criteria doesn not contain priority " + priority);
            }
        };
    }

}
