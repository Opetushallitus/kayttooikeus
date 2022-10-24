package fi.vm.sade.kayttooikeus.service;

import com.google.common.collect.Sets;
import fi.vm.sade.kayttooikeus.dto.KutsuCreateDto;
import fi.vm.sade.kayttooikeus.dto.TextGroupDto;
import fi.vm.sade.kayttooikeus.dto.UpdateHaettuKayttooikeusryhmaDto;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.KayttoOikeusRyhmaRepository;
import fi.vm.sade.kayttooikeus.repositories.dto.ExpiringKayttoOikeusDto;
import fi.vm.sade.kayttooikeus.service.external.*;
import fi.vm.sade.kayttooikeus.service.impl.EmailServiceImpl;
import fi.vm.sade.kayttooikeus.service.impl.email.SahkopostiHenkiloDto;
import fi.vm.sade.kayttooikeus.util.CreateUtil;
import fi.vm.sade.kayttooikeus.util.YhteystietoUtil;
import fi.vm.sade.oppijanumerorekisteri.dto.*;
import fi.vm.sade.ryhmasahkoposti.api.dto.EmailData;
import fi.vm.sade.ryhmasahkoposti.api.dto.EmailMessage;
import fi.vm.sade.ryhmasahkoposti.api.dto.EmailRecipient;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.singleton;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

@RunWith(SpringRunner.class)
public class EmailServiceTest extends AbstractServiceTest {

    private static final String HENKILO_OID = "1.2.3.4.5";
    private static final String WORK_EMAIL = "testi@example.com";

    private static final String TEST_LANG = "fi";
    private static final String TEST_EMAIL = "arpa@kuutio.fi";
    private static final String TEST_FIRST_NAME = "arpa";
    private static final String TEST_LAST_NAME = "kuutio";

    @MockBean
    private RyhmasahkopostiClient ryhmasahkopostiClient;

    @MockBean
    private OppijanumerorekisteriClient oppijanumerorekisteriClient;

    @MockBean
    private KayttoOikeusRyhmaRepository kayttoOikeusRyhmaRepository;

    @MockBean
    private OrganisaatioClient organisaatioClient;

    @Autowired
    private EmailService emailService;

    @Test
    @WithMockUser(username = "user1")
    public void sendExpirationReminderTest() {
        given(oppijanumerorekisteriClient.getHenkilonPerustiedot(HENKILO_OID)).willReturn(of(getPerustiedot()));
        given(oppijanumerorekisteriClient.getHenkiloByOid(HENKILO_OID)).willReturn(getHenkilo());
        given(ryhmasahkopostiClient.sendRyhmasahkoposti(any(EmailData.class)))
                .willReturn("");

        emailService.sendExpirationReminder(HENKILO_OID, Collections.singletonList(
                ExpiringKayttoOikeusDto.builder()
                        .henkiloOid(HENKILO_OID)
                        .myonnettyTapahtumaId(1L)
                        .ryhmaName("RYHMA")
                        .ryhmaDescription(new TextGroupDto(2L).put("FI", "Kuvaus")
                                .put("EN", "Desc"))
                        .voimassaLoppuPvm(LocalDate.of(2021, 10, 8))
                        .build())
        );

        verify(ryhmasahkopostiClient, times(1)).sendRyhmasahkoposti(
                argThat(new TypeSafeMatcher<EmailData>() {
                    @Override
                    public void describeTo(Description description) {
                        description.appendText("Not valid email.");
                    }

                    @Override
                    protected boolean matchesSafely(EmailData item) {
                        return item.getRecipient().size() == 1
                                && item.getRecipient().get(0).getEmail().equals(WORK_EMAIL)
                                && !item.getRecipient().get(0).getRecipientReplacements().isEmpty()
                                && item.getRecipient().get(0).getRecipientReplacements().size() == 3
                                && "kayttooikeusryhmat".equals(item.getRecipient().get(0).getRecipientReplacements().get(1).getName())
                                && "Kuvaus (8.10.2021)".equals(item.getRecipient().get(0).getRecipientReplacements().get(1).getValue());
                    }
                })
        );
    }

    private HenkiloPerustietoDto getPerustiedot() {
        HenkiloPerustietoDto perustiedot = new HenkiloPerustietoDto();
        KielisyysDto kielisyys = new KielisyysDto();
        kielisyys.setKieliKoodi("FI");
        perustiedot.setAsiointiKieli(kielisyys);
        return perustiedot;
    }

    private HenkiloDto getHenkilo() {
        HenkiloDto henkiloDto = new HenkiloDto();
        henkiloDto.setYhteystiedotRyhma(singleton(YhteystiedotRyhmaDto
                .builder()
                .ryhmaKuvaus(YhteystietoUtil.TYOOSOITE)
                .yhteystieto(YhteystietoDto.builder()
                        .yhteystietoTyyppi(YhteystietoTyyppi.YHTEYSTIETO_SAHKOPOSTI)
                        .yhteystietoArvo(WORK_EMAIL)
                        .build())
                .build()));
        return henkiloDto;
    }

    @Test
    @WithMockUser(username = "user1")
    public void sendExpirationReminderNoWorkEmailTest() {
        given(oppijanumerorekisteriClient.getHenkilonPerustiedot(HENKILO_OID)).willReturn(of(getPerustiedot()));
        given(oppijanumerorekisteriClient.getHenkiloByOid(HENKILO_OID)).willReturn(new HenkiloDto());

        emailService.sendExpirationReminder(HENKILO_OID, Collections.EMPTY_LIST);

        verify(ryhmasahkopostiClient, never()).sendRyhmasahkoposti(any());
    }

    @Test
    @WithMockUser(username = "user1")
    public void NewRequisitionNotificationTest() {
        given(oppijanumerorekisteriClient.getHenkiloByOid(HENKILO_OID)).willReturn(getHenkilo());

        emailService.sendNewRequisitionNotificationEmails(Collections.singleton(HENKILO_OID));

        verify(ryhmasahkopostiClient, times(1)).sendRyhmasahkoposti(
                argThat(new TypeSafeMatcher<EmailData>() {
                    @Override
                    public void describeTo(Description description) {
                        description.appendText("Not valid email.");
                    }

                    @Override
                    protected boolean matchesSafely(EmailData item) {
                        return item.getRecipient().size() == 1
                                && item.getRecipient().get(0).getEmail().equals(WORK_EMAIL)
                                && !item.getRecipient().get(0).getRecipientReplacements().isEmpty();
                    }
                })
        );
    }

    @Test
    @WithMockUser(username = "user1")
    public void NewRequisitionNotificationNoWorkEmailTest() {
        given(oppijanumerorekisteriClient.getHenkiloByOid(HENKILO_OID)).willReturn(new HenkiloDto());

        emailService.sendNewRequisitionNotificationEmails(Collections.singleton(HENKILO_OID));

        verify(ryhmasahkopostiClient, never()).sendRyhmasahkoposti(any());
    }

    @Test
    public void sendEmailKayttooikeusAnomusKasitelty() {
        HenkiloDto henkiloDto = new HenkiloDto();
        henkiloDto.setOidHenkilo(HENKILO_OID);
        henkiloDto.setYhteystiedotRyhma(Sets.newHashSet(CreateUtil.createYhteystietoSahkoposti("arpa@kuutio.fi", "yhteystietotyyppi7"),
                CreateUtil.createYhteystietoSahkoposti("arpa2@kuutio.fi", YhteystietoUtil.TYOOSOITE),
                CreateUtil.createYhteystietoSahkoposti("arpa3@kuutio.fi", "yhteystietotyyppi3")));
        henkiloDto.setAsiointiKieli(new KielisyysDto("sv", "svenska"));
        henkiloDto.setEtunimet("arpa noppa");
        henkiloDto.setKutsumanimi("arpa");
        henkiloDto.setSukunimi("kuutio");
        given(this.oppijanumerorekisteriClient.getHenkiloByOid(HENKILO_OID)).willReturn(henkiloDto);
        given(this.kayttoOikeusRyhmaRepository.findById(10L)).willReturn(of(KayttoOikeusRyhma.builder()
                .tunniste("kayttooikeusryhmatunniste")
                .nimi(new TextGroup())
                .build()));
        LocalDate startDate = LocalDate.of(2017, 10, 10);
        LocalDate endDate = LocalDate.of(2017, 10, 9);
        UpdateHaettuKayttooikeusryhmaDto updateHaettuKayttooikeusryhmaDto
                = new UpdateHaettuKayttooikeusryhmaDto(10L, "MYONNETTY", startDate, endDate, null);

        Henkilo henkilo = new Henkilo();
        henkilo.setOidHenkilo(HENKILO_OID);
        Anomus anomus = Anomus.builder().sahkopostiosoite("arpa@kuutio.fi")
                .henkilo(henkilo)
                .anomuksenTila(AnomuksenTila.KASITELTY)
                .hylkaamisperuste("Hyvä oli")
                .haettuKayttoOikeusRyhmas(Sets.newHashSet(HaettuKayttoOikeusRyhma.builder()
                        .kayttoOikeusRyhma(KayttoOikeusRyhma.builder()
                                .nimi(new TextGroup())
                                .tunniste("Käyttöoikeusryhma haettu").build())
                        .build()))
                .myonnettyKayttooikeusRyhmas(Sets.newHashSet(MyonnettyKayttoOikeusRyhmaTapahtuma.builder()
                        .kayttoOikeusRyhma(KayttoOikeusRyhma.builder()
                                .nimi(new TextGroup())
                                .tunniste("Käyttöoikeusryhmä").build())
                        .build()))
                .build();
        anomus.getHaettuKayttoOikeusRyhmas().stream().forEach(h -> h.getKayttoOikeusRyhma().setId(10L));

        this.emailService.sendEmailAnomusKasitelty(anomus, updateHaettuKayttooikeusryhmaDto, 10L);

        ArgumentCaptor<EmailData> emailDataArgumentCaptor = ArgumentCaptor.forClass(EmailData.class);
        verify(this.ryhmasahkopostiClient).sendRyhmasahkoposti(emailDataArgumentCaptor.capture());
        EmailData emailData = emailDataArgumentCaptor.getValue();
        assertThat(emailData.getRecipient()).hasSize(1);
        assertThat(emailData.getRecipient().get(0).getRecipientReplacements()).hasSize(3)
                .extracting("name").containsExactlyInAnyOrder("vastaanottaja", "rooli", "linkki");
        assertThat(emailData.getRecipient().get(0).getOid()).isEqualTo(HENKILO_OID);
        assertThat(emailData.getRecipient().get(0).getEmail()).isEqualTo("arpa@kuutio.fi");
        assertThat(emailData.getRecipient().get(0).getName()).isEqualTo("arpa kuutio");
        assertThat(emailData.getRecipient().get(0).getLanguageCode()).isEqualTo("sv");
        assertThat(emailData.getRecipient().get(0).getOidType()).isEqualTo("henkilo");

        assertThat(emailData.getEmail().getLanguageCode()).isEqualTo("sv");
        assertThat(emailData.getEmail().getFrom()).isNull();
        assertThat(emailData.getEmail().getCallingProcess()).isEqualTo("kayttooikeus");
    }

    @Test
    public void sendInvitationEmail() {
        OrganisaatioPerustieto organisaatioPerustieto = new OrganisaatioPerustieto();
        organisaatioPerustieto.setNimi(new HashMap<String, String>() {{
            put("fi", "suomenkielinennimi");
        }});
        SahkopostiHenkiloDto kutsuja = new SahkopostiHenkiloDto();
        kutsuja.setKutsumanimi("kutsun");
        kutsuja.setSukunimi("kutsuja");
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(any()))
                .willReturn(Optional.of(organisaatioPerustieto));
        given(this.oppijanumerorekisteriClient.getHenkiloByOid(any()))
                .willReturn(HenkiloDto.builder().kutsumanimi("kutsun").sukunimi("kutsuja").build());
        Kutsu kutsu = Kutsu.builder()
                .kieliKoodi("fi")
                .sahkoposti("arpa@kuutio.fi")
                .salaisuus("salaisuushash")
                .etunimi("arpa")
                .sukunimi("kuutio")
                .saate(null)
                .organisaatiot(Sets.newHashSet(KutsuOrganisaatio.builder()
                        .organisaatioOid("1.2.3.4.1")
                        .ryhmat(Sets.newHashSet(KayttoOikeusRyhma.builder().nimi(new TextGroup()).build()))
                        .build()))
                .aikaleima(LocalDateTime.now())
                .build();

        this.emailService.sendInvitationEmail(kutsu);
        ArgumentCaptor<EmailData> emailDataArgumentCaptor = ArgumentCaptor.forClass(EmailData.class);
        verify(this.ryhmasahkopostiClient).sendRyhmasahkoposti(emailDataArgumentCaptor.capture());
        EmailData emailData = emailDataArgumentCaptor.getValue();
        assertThat(emailData.getRecipient()).hasSize(1);
        assertThat(emailData.getRecipient().get(0).getRecipientReplacements())
                .extracting("name")
                .containsExactlyInAnyOrder("vastaanottaja", "organisaatiot", "linkki", "kutsuja", "voimassa", "saate");
        assertTrue(emailData.getRecipient().get(0).getRecipientReplacements().stream().anyMatch(r -> Objects.equals(r.getName(), "kutsuja") && Objects.equals(r.getValue(), kutsuja.toString()) ));
        assertThat(emailData.getRecipient().get(0).getOid()).isEmpty();
        assertThat(emailData.getRecipient().get(0).getOidType()).isEmpty();
        assertThat(emailData.getRecipient().get(0).getEmail()).isEqualTo("arpa@kuutio.fi");
        assertThat(emailData.getRecipient().get(0).getName()).isEqualTo("arpa kuutio");
        assertThat(emailData.getRecipient().get(0).getLanguageCode()).isEqualTo("fi");

        assertThat(emailData.getEmail().getCallingProcess()).isEqualTo("kayttooikeus");
        assertThat(emailData.getEmail().getLanguageCode()).isEqualTo("fi");
        assertThat(emailData.getEmail().getFrom()).isNull();
        assertThat(emailData.getEmail().getTemplateName()).isEqualTo("kayttooikeus_kutsu_v2");
    }

    @Test
    public void sendInvitationEmailAsServiceKutsuja() {
        OrganisaatioPerustieto organisaatioPerustieto = new OrganisaatioPerustieto();
        organisaatioPerustieto.setNimi(new HashMap<String, String>() {{
            put("fi", "suomenkielinennimi");
        }});

        String expectedKutsuja =  "Varda Info";

        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(any()))
                .willReturn(Optional.of(organisaatioPerustieto));
        given(this.oppijanumerorekisteriClient.getHenkiloByOid(any()))
                .willReturn(HenkiloDto.builder().kutsumanimi("kutsun").sukunimi("kutsuja").build());
        Kutsu kutsu = Kutsu.builder()
                .kieliKoodi("fi")
                .sahkoposti("arpa@kuutio.fi")
                .salaisuus("salaisuushash")
                .etunimi("arpa")
                .sukunimi("kuutio")
                .saate(null)
                .organisaatiot(Sets.newHashSet(KutsuOrganisaatio.builder()
                        .organisaatioOid("1.2.3.4.1")
                        .ryhmat(Sets.newHashSet(KayttoOikeusRyhma.builder().nimi(new TextGroup()).build()))
                        .build()))
                .aikaleima(LocalDateTime.now())
                .build();

        this.emailService.sendInvitationEmail(kutsu, expectedKutsuja);
        ArgumentCaptor<EmailData> emailDataArgumentCaptor = ArgumentCaptor.forClass(EmailData.class);
        verify(this.ryhmasahkopostiClient).sendRyhmasahkoposti(emailDataArgumentCaptor.capture());
        EmailData emailData = emailDataArgumentCaptor.getValue();
        assertThat(emailData.getRecipient()).hasSize(1);
        assertThat(emailData.getRecipient().get(0).getRecipientReplacements())
                .extracting("name")
                .containsExactlyInAnyOrder("vastaanottaja", "organisaatiot", "linkki", "kutsuja", "voimassa", "saate");

        assertTrue(emailData.getRecipient().get(0).getRecipientReplacements().stream().anyMatch(r -> Objects.equals(r.getName(), "kutsuja") && Objects.equals(r.getValue(), expectedKutsuja) ));
        assertThat(emailData.getRecipient().get(0).getOid()).isEmpty();
        assertThat(emailData.getRecipient().get(0).getOidType()).isEmpty();
        assertThat(emailData.getRecipient().get(0).getEmail()).isEqualTo("arpa@kuutio.fi");
        assertThat(emailData.getRecipient().get(0).getName()).isEqualTo("arpa kuutio");
        assertThat(emailData.getRecipient().get(0).getLanguageCode()).isEqualTo("fi");

        assertThat(emailData.getEmail().getCallingProcess()).isEqualTo("kayttooikeus");
        assertThat(emailData.getEmail().getLanguageCode()).isEqualTo("fi");
        assertThat(emailData.getEmail().getFrom()).isNull();
        assertThat(emailData.getEmail().getTemplateName()).isEqualTo("kayttooikeus_kutsu_v2");
    }

    @Test
    public void sendDiscardedInvitationNotificationSuccess() {
        Kutsu invitation = Kutsu.builder()
                .kieliKoodi(TEST_LANG)
                .sahkoposti(TEST_EMAIL)
                .etunimi(TEST_FIRST_NAME)
                .sukunimi(TEST_LAST_NAME)
                .build();

        emailService.sendDiscardNotification(invitation);
        ArgumentCaptor<EmailData> captor = ArgumentCaptor.forClass(EmailData.class);
        verify(ryhmasahkopostiClient).sendRyhmasahkoposti(captor.capture());

        EmailData emailData = captor.getValue();
        assertThat(emailData.getReplacements().isEmpty()).isTrue();
        assertThat(emailData.getRecipient().size()).isEqualTo(1);

        EmailMessage message = emailData.getEmail();
        assertThat(message.getTemplateName()).isEqualTo(EmailServiceImpl.DISCARDED_INVITATION_EMAIL_TEMPLATE);
        assertThat(message.getLanguageCode()).isEqualTo(TEST_LANG);

        EmailRecipient recipient = emailData.getRecipient().get(0);
        assertThat(recipient.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(recipient.getName()).isEqualTo(String.format("%s %s", TEST_FIRST_NAME, TEST_LAST_NAME));
        assertThat(recipient.getLanguageCode()).isEqualTo(TEST_LANG);
    }

    @Test(expected = RuntimeException.class)
    public void sendDiscardedInvitationNotificationFailure() {
        when(ryhmasahkopostiClient.sendRyhmasahkoposti(any())).thenThrow(new RuntimeException("BOOM!"));
        emailService.sendDiscardNotification(Kutsu.builder().build());
    }

    @Test
    public void sendDiscardedApplicationNotificationSuccess() {

        Henkilo henkilo = mock(Henkilo.class);
        when(henkilo.getKutsumanimiCached()).thenReturn(TEST_FIRST_NAME);
        when(henkilo.getSukunimiCached()).thenReturn(TEST_LAST_NAME);

        Anomus application = mock(Anomus.class);
        when(application.getHenkilo()).thenReturn(henkilo);
        when(application.getSahkopostiosoite()).thenReturn(TEST_EMAIL);

        KielisyysDto kielisyysDto = mock(KielisyysDto.class);
        when(kielisyysDto.getKieliKoodi()).thenReturn(TEST_LANG);

        HenkiloDto henkiloDto = mock(HenkiloDto.class);
        when(henkiloDto.getAsiointiKieli()).thenReturn(kielisyysDto);

        given(oppijanumerorekisteriClient.getHenkiloByOid(any())).willReturn(henkiloDto);

        emailService.sendDiscardNotification(application);
        ArgumentCaptor<EmailData> captor = ArgumentCaptor.forClass(EmailData.class);
        verify(ryhmasahkopostiClient).sendRyhmasahkoposti(captor.capture());

        EmailData emailData = captor.getValue();
        assertThat(emailData.getReplacements().isEmpty()).isTrue();
        assertThat(emailData.getRecipient().size()).isEqualTo(1);

        EmailMessage message = emailData.getEmail();
        assertThat(message.getTemplateName()).isEqualTo(EmailServiceImpl.DISCARDED_APPLICATION_EMAIL_TEMPLATE);
        assertThat(message.getLanguageCode()).isEqualTo(TEST_LANG);

        EmailRecipient recipient = emailData.getRecipient().get(0);
        assertThat(recipient.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(recipient.getName()).isEqualTo(String.format("%s %s", TEST_FIRST_NAME, TEST_LAST_NAME));
        assertThat(recipient.getLanguageCode()).isEqualTo(TEST_LANG);
    }

    @Test
    public void sendDiscardedApplicationNotificationResolveLangFailure() {

        Henkilo henkilo = mock(Henkilo.class);
        when(henkilo.getKutsumanimiCached()).thenReturn(TEST_FIRST_NAME);
        when(henkilo.getSukunimiCached()).thenReturn(TEST_LAST_NAME);

        Anomus application = mock(Anomus.class);
        when(application.getHenkilo()).thenReturn(henkilo);
        when(application.getSahkopostiosoite()).thenReturn(TEST_EMAIL);

        emailService.sendDiscardNotification(application);
        ArgumentCaptor<EmailData> captor = ArgumentCaptor.forClass(EmailData.class);
        verify(ryhmasahkopostiClient).sendRyhmasahkoposti(captor.capture());

        EmailData emailData = captor.getValue();
        assertThat(emailData.getReplacements().isEmpty()).isTrue();
        assertThat(emailData.getRecipient().size()).isEqualTo(1);

        EmailMessage message = emailData.getEmail();
        assertThat(message.getTemplateName()).isEqualTo(EmailServiceImpl.DISCARDED_APPLICATION_EMAIL_TEMPLATE);

        EmailRecipient recipient = emailData.getRecipient().get(0);
        assertThat(recipient.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(recipient.getName()).isEqualTo(String.format("%s %s", TEST_FIRST_NAME, TEST_LAST_NAME));
    }

    @Test(expected = RuntimeException.class)
    public void sendDiscardedApplicationNotificationFailure() {
        emailService.sendDiscardNotification(Anomus.builder().build());
    }
}
