package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.Identification;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.repositories.IdentificationRepository;
import fi.vm.sade.kayttooikeus.repositories.KutsuRepository;
import fi.vm.sade.kayttooikeus.repositories.TunnistusTokenDataRepository;
import fi.vm.sade.kayttooikeus.service.KayttoOikeusService;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService;
import fi.vm.sade.kayttooikeus.service.exception.ValidationException;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IdentificationServiceImplTest {

    private IdentificationServiceImpl identificationServiceImpl;

    @Mock
    private IdentificationRepository identificationRepositoryMock;
    @Mock
    private HenkiloDataRepository henkiloDataRepositoryMock;
    @Mock
    private KutsuRepository kutsuRepositoryMock;
    @Mock
    private TunnistusTokenDataRepository tunnistusTokenDataRepositoryMock;

    @Mock
    private KayttoOikeusService kayttoOikeusServiceMock;
    @Mock
    private LdapSynchronizationService ldapSynchronizationServiceMock;

    @Mock
    private OrikaBeanMapper mapperMock;

    @Mock
    private OppijanumerorekisteriClient oppijanumerorekisteriClientMock;

    @Before
    public void setup() {
        identificationServiceImpl = new IdentificationServiceImpl(identificationRepositoryMock,
                henkiloDataRepositoryMock, kutsuRepositoryMock, tunnistusTokenDataRepositoryMock,
                kayttoOikeusServiceMock, ldapSynchronizationServiceMock, mapperMock, oppijanumerorekisteriClientMock);
    }

    @Test
    public void generateAuthTokenForHenkiloNewIdentification() {
        Henkilo henkilo = Henkilo.builder().build();
        when(identificationRepositoryMock.findByidpEntityIdAndIdentifier(eq("key1"), eq("value1")))
                .thenReturn(Optional.empty());

        String token = identificationServiceImpl
                .generateAuthTokenForHenkilo(henkilo, "key1", "value1");

        ArgumentCaptor<Identification> identificationArgumentCaptor = ArgumentCaptor.forClass(Identification.class);
        verify(identificationRepositoryMock).save(identificationArgumentCaptor.capture());
        Identification identification = identificationArgumentCaptor.getValue();
        assertThat(identification.getAuthtoken()).isEqualTo(token);
    }

    @Test
    public void generateAuthTokenForHenkiloExistingIdentification() {
        Henkilo henkilo = Henkilo.builder().build();
        Identification identification = new Identification(henkilo, "key1", "value1");
        when(identificationRepositoryMock.findByidpEntityIdAndIdentifier(eq("key1"), eq("value1")))
                .thenReturn(Optional.of(identification));

        String token = identificationServiceImpl
                .generateAuthTokenForHenkilo(henkilo, "key1", "value1");

        assertThat(identification.getAuthtoken()).isEqualTo(token);
    }

    @Test
    public void generateAuthTokenForHenkiloExistingIdentificationWithAnotherHenkilo() {
        Henkilo henkilo1 = Henkilo.builder().build();
        Identification identification = new Identification(henkilo1, "key1", "value1");
        when(identificationRepositoryMock.findByidpEntityIdAndIdentifier(eq("key1"), eq("value1")))
                .thenReturn(Optional.of(identification));
        Henkilo henkilo2 = Henkilo.builder().build();

        Throwable throwable = catchThrowable(() -> identificationServiceImpl
                .generateAuthTokenForHenkilo(henkilo2, "key1", "value1"));

        assertThat(throwable).isInstanceOf(ValidationException.class);
    }

    @Test
    public void updateIdentificationAndGenerateTokenForHenkiloByOidNewIdentification() {
        String oid = "oid1";
        Henkilo henkilo = Henkilo.builder().kayttajatiedot(Kayttajatiedot.builder().username("user1").build()).build();
        when(henkiloDataRepositoryMock.findByOidHenkilo(eq(oid))).thenReturn(Optional.of(henkilo));

        String token = identificationServiceImpl.updateIdentificationAndGenerateTokenForHenkiloByOid(oid);

        ArgumentCaptor<Identification> identificationArgumentCaptor = ArgumentCaptor.forClass(Identification.class);
        verify(identificationRepositoryMock).save(identificationArgumentCaptor.capture());
        Identification identification = identificationArgumentCaptor.getValue();
        assertThat(identification.getAuthtoken()).isEqualTo(token);
    }

    @Test
    public void updateIdentificationAndGenerateTokenForHenkiloByOidExistingIdentification() {
        String oid = "oid1";
        Henkilo henkilo = Henkilo.builder().kayttajatiedot(Kayttajatiedot.builder().username("user1").build()).build();
        when(henkiloDataRepositoryMock.findByOidHenkilo(eq(oid))).thenReturn(Optional.of(henkilo));
        Identification identification = new Identification(henkilo, "keymock", "user1");
        when(identificationRepositoryMock.findByidpEntityIdAndIdentifier(any(), any()))
                .thenReturn(Optional.of(identification));

        String token = identificationServiceImpl.updateIdentificationAndGenerateTokenForHenkiloByOid(oid);

        assertThat(identification.getAuthtoken()).isEqualTo(token);
    }

}
