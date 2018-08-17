package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.VarmennusPoletti;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.repositories.KayttajatiedotRepository;
import fi.vm.sade.kayttooikeus.repositories.VarmennusPolettiRepository;
import fi.vm.sade.kayttooikeus.service.CryptoService;
import fi.vm.sade.kayttooikeus.service.EmailService;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService;
import fi.vm.sade.kayttooikeus.service.TimeService;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystiedotRyhmaDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoTyyppi;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UnohtunutSalasanaServiceImplTest {

    private UnohtunutSalasanaServiceImpl impl;

    @Mock
    private TimeService timeService;
    @Mock
    private EmailService emailService;
    @Mock
    private HenkiloDataRepository henkiloDataRepository;
    @Mock
    private VarmennusPolettiRepository varmennusPolettiRepository;
    @Mock
    private OppijanumerorekisteriClient oppijanumerorekisteriClient;
    @Mock
    private CryptoService cryptoService;
    @Mock
    private LdapSynchronizationService ldapSynchronizationService;
    @Mock
    private KayttajatiedotRepository kayttajatiedotRepository;

    @Before
    public void setup() {
        impl = new UnohtunutSalasanaServiceImpl(timeService, emailService,
                henkiloDataRepository, varmennusPolettiRepository,
                oppijanumerorekisteriClient, cryptoService, ldapSynchronizationService,
                kayttajatiedotRepository);
    }

    @Test
    public void lahetaPolettiShouldSendEmail() {
        LocalDateTime now = LocalDateTime.now();

        when(henkiloDataRepository.findByKayttajatiedotUsername(any()))
                .thenReturn(Optional.of(Henkilo.builder().oidHenkilo("oid1").build()));
        when(timeService.getDateTimeNow())
                .thenReturn(now);
        when(varmennusPolettiRepository.save(any(VarmennusPoletti.class)))
                .thenReturn(VarmennusPoletti.builder()
                        .poletti("poletti1")
                        .build());
        HenkiloDto henkiloDto = HenkiloDto.builder()
                .yhteystiedotRyhma(singleton(YhteystiedotRyhmaDto.builder()
                        .yhteystieto(YhteystietoDto.builder()
                                .yhteystietoTyyppi(YhteystietoTyyppi.YHTEYSTIETO_SAHKOPOSTI)
                                .yhteystietoArvo("example@example.com")
                                .build())
                        .build()))
                .build();
        when(oppijanumerorekisteriClient.getHenkiloByOid(any()))
                .thenReturn(henkiloDto);
        when(oppijanumerorekisteriClient.listOidByYhteystieto(any()))
                .thenReturn(singleton("oid1"));

        impl.lahetaPoletti("kayttajatunnus1");

        ArgumentCaptor<VarmennusPoletti> varmennusPolettiCaptor = ArgumentCaptor.forClass(VarmennusPoletti.class);
        verify(varmennusPolettiRepository).save(varmennusPolettiCaptor.capture());
        VarmennusPoletti varmennusPoletti = varmennusPolettiCaptor.getValue();
        assertThat(varmennusPoletti.getPoletti()).isNotNull();
        assertThat(varmennusPoletti.getTyyppi()).isEqualByComparingTo(VarmennusPoletti.VarmennusPolettiTyyppi.HAVINNYT_SALASANA);
        assertThat(varmennusPoletti.getVoimassa()).isAfter(now);
        verify(oppijanumerorekisteriClient).listOidByYhteystieto(eq("example@example.com"));
        verify(emailService).sendEmailReset(eq(henkiloDto), eq("example@example.com"), eq("poletti1"), any());
    }

    @Test
    public void lahetaPolettiShouldNotSendEmailWhenSameEmail() {
        LocalDateTime now = LocalDateTime.now();

        when(henkiloDataRepository.findByKayttajatiedotUsername(any()))
                .thenReturn(Optional.of(Henkilo.builder().oidHenkilo("oid1").build()));
        when(timeService.getDateTimeNow())
                .thenReturn(now);
        when(varmennusPolettiRepository.save(any(VarmennusPoletti.class)))
                .thenReturn(VarmennusPoletti.builder()
                        .poletti("poletti1")
                        .build());
        HenkiloDto henkiloDto = HenkiloDto.builder()
                .yhteystiedotRyhma(singleton(YhteystiedotRyhmaDto.builder()
                        .yhteystieto(YhteystietoDto.builder()
                                .yhteystietoTyyppi(YhteystietoTyyppi.YHTEYSTIETO_SAHKOPOSTI)
                                .yhteystietoArvo("example@example.com")
                                .build())
                        .build()))
                .build();
        when(oppijanumerorekisteriClient.getHenkiloByOid(any()))
                .thenReturn(henkiloDto);
        when(oppijanumerorekisteriClient.listOidByYhteystieto(any()))
                .thenReturn(Stream.of("oid1", "oid2").collect(toSet()));

        impl.lahetaPoletti("kayttajatunnus1");

        verify(oppijanumerorekisteriClient).listOidByYhteystieto(eq("example@example.com"));
        verify(emailService, never()).sendEmailReset(any(), any(), any(), any());
    }

    @Test
    public void lahetaPolettiShouldNotSendEmailWhenDifferentEmail() {
        LocalDateTime now = LocalDateTime.now();

        when(henkiloDataRepository.findByKayttajatiedotUsername(any()))
                .thenReturn(Optional.of(Henkilo.builder().oidHenkilo("oid1").build()));
        when(timeService.getDateTimeNow())
                .thenReturn(now);
        when(varmennusPolettiRepository.save(any(VarmennusPoletti.class)))
                .thenReturn(VarmennusPoletti.builder()
                        .poletti("poletti1")
                        .build());
        HenkiloDto henkiloDto = HenkiloDto.builder()
                .yhteystiedotRyhma(singleton(YhteystiedotRyhmaDto.builder()
                        .yhteystieto(YhteystietoDto.builder()
                                .yhteystietoTyyppi(YhteystietoTyyppi.YHTEYSTIETO_SAHKOPOSTI)
                                .yhteystietoArvo("example@example.com")
                                .build())
                        .build()))
                .build();
        when(oppijanumerorekisteriClient.getHenkiloByOid(any()))
                .thenReturn(henkiloDto);
        when(oppijanumerorekisteriClient.listOidByYhteystieto(any()))
                .thenReturn(singleton("oid2"));

        impl.lahetaPoletti("kayttajatunnus1");

        verify(oppijanumerorekisteriClient).listOidByYhteystieto(eq("example@example.com"));
        verify(emailService, never()).sendEmailReset(any(), any(), any(), any());
    }

    @Test
    public void lahetaPolettiShouldNotSendEmailWhenHenkiloNotFound() {
        when(henkiloDataRepository.findByKayttajatiedotUsername(any()))
                .thenReturn(Optional.empty());

        impl.lahetaPoletti("kayttajatunnus1");

        verify(emailService, never()).sendEmailReset(any(), any(), any(), any());
    }

}
