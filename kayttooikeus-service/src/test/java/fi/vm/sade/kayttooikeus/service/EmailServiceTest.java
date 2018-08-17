package fi.vm.sade.kayttooikeus.service;

import com.google.common.collect.Sets;
import fi.vm.sade.kayttooikeus.dto.TextGroupDto;
import fi.vm.sade.kayttooikeus.dto.UpdateHaettuKayttooikeusryhmaDto;
import fi.vm.sade.kayttooikeus.dto.YhteystietojenTyypit;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.KayttoOikeusRyhmaRepository;
import fi.vm.sade.kayttooikeus.repositories.dto.ExpiringKayttoOikeusDto;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import fi.vm.sade.kayttooikeus.service.external.RyhmasahkopostiClient;
import fi.vm.sade.kayttooikeus.util.CreateUtil;
import fi.vm.sade.oppijanumerorekisteri.dto.*;
import fi.vm.sade.ryhmasahkoposti.api.dto.EmailData;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
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
import java.util.HashMap;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Optional.of;
import static org.apache.http.HttpVersion.HTTP_1_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

@RunWith(SpringRunner.class)
public class EmailServiceTest extends AbstractServiceTest {
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
        HenkiloPerustietoDto perustiedot = new HenkiloPerustietoDto();
        KielisyysDto kielisyys = new KielisyysDto();
        kielisyys.setKieliKoodi("FI");
        perustiedot.setAsiointiKieli(kielisyys);
        given(oppijanumerorekisteriClient.getHenkilonPerustiedot("1.2.3.4.5")).willReturn(of(perustiedot));
        HenkiloDto henkiloDto = new HenkiloDto();
        henkiloDto.setYhteystiedotRyhma(singleton(YhteystiedotRyhmaDto
                .builder()
                .ryhmaKuvaus(YhteystietojenTyypit.TYOOSOITE)
                .yhteystieto(YhteystietoDto.builder()
                        .yhteystietoTyyppi(YhteystietoTyyppi.YHTEYSTIETO_SAHKOPOSTI)
                        .yhteystietoArvo("testi@example.com")
                        .build())
                .build()));
        given(oppijanumerorekisteriClient.getHenkiloByOid("1.2.3.4.5")).willReturn(henkiloDto);
        given(ryhmasahkopostiClient.sendRyhmasahkoposti(any(EmailData.class)))
                .willReturn(new BasicHttpResponse(new BasicStatusLine(HTTP_1_1, 200, "")));
        emailService.sendExpirationReminder("1.2.3.4.5", asList(
                ExpiringKayttoOikeusDto.builder()
                    .henkiloOid("1.2.3.4.5")
                    .myonnettyTapahtumaId(1L)
                    .ryhmaName("RYHMA")
                    .ryhmaDescription(new TextGroupDto(2L).put("FI", "Kuvaus")
                            .put("EN", "Desc"))
                    .voimassaLoppuPvm(LocalDate.now().plusMonths(3))
                .build(),
                ExpiringKayttoOikeusDto.builder()
                    .henkiloOid("1.2.3.4.5")
                    .myonnettyTapahtumaId(3L)
                    .ryhmaName("RYHMA2")
                    .ryhmaDescription(new TextGroupDto(3L).put("FI", "Kuvaus2")
                            .put("EN", "Desc2"))
                    .voimassaLoppuPvm(LocalDate.now().plusMonths(3))
                .build()
            ));
        verify(ryhmasahkopostiClient, times(1)).sendRyhmasahkoposti(
                argThat(new TypeSafeMatcher<EmailData>() {
                    @Override
                    public void describeTo(Description description) {
                        description.appendText("Not valid email.");
                    }
                    @Override
                    protected boolean matchesSafely(EmailData item) {
                        return item.getRecipient().size() == 1
                                && item.getRecipient().get(0).getEmail().equals("testi@example.com")
                                && !item.getRecipient().get(0).getRecipientReplacements().isEmpty();
                    }
                })
        );
    }

    @Test
    public void sendEmailKayttooikeusAnomusKasitelty() {
        HenkiloDto henkiloDto = new HenkiloDto();
        henkiloDto.setOidHenkilo("1.2.3.4.5");
        henkiloDto.setYhteystiedotRyhma(Sets.newHashSet(CreateUtil.createYhteystietoSahkoposti("arpa@kuutio.fi", YhteystietojenTyypit.MUU_OSOITE),
                CreateUtil.createYhteystietoSahkoposti("arpa2@kuutio.fi", YhteystietojenTyypit.TYOOSOITE),
                CreateUtil.createYhteystietoSahkoposti("arpa3@kuutio.fi", YhteystietojenTyypit.VAPAA_AJAN_OSOITE)));
        henkiloDto.setAsiointiKieli(new KielisyysDto("sv", "svenska"));
        henkiloDto.setEtunimet("arpa noppa");
        henkiloDto.setKutsumanimi("arpa");
        henkiloDto.setSukunimi("kuutio");
        given(this.oppijanumerorekisteriClient.getHenkiloByOid("1.2.3.4.5")).willReturn(henkiloDto);
        given(this.kayttoOikeusRyhmaRepository.findById(10L)).willReturn(of(KayttoOikeusRyhma.builder()
                .tunniste("kayttooikeusryhmatunniste")
                .nimi(new TextGroup())
                .build()));
        LocalDate startDate = LocalDate.of(2017, 10, 10);
        LocalDate endDate = LocalDate.of(2017, 10, 9);
        UpdateHaettuKayttooikeusryhmaDto updateHaettuKayttooikeusryhmaDto
                = new UpdateHaettuKayttooikeusryhmaDto(10L, "MYONNETTY", startDate, endDate, null);

        Henkilo henkilo = new Henkilo();
        henkilo.setOidHenkilo("1.2.3.4.5");
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
        anomus.getHaettuKayttoOikeusRyhmas().stream().forEach( h -> h.getKayttoOikeusRyhma().setId(10L));

        this.emailService.sendEmailAnomusKasitelty(anomus, updateHaettuKayttooikeusryhmaDto, 10L);

        ArgumentCaptor<EmailData> emailDataArgumentCaptor = ArgumentCaptor.forClass(EmailData.class);
        verify(this.ryhmasahkopostiClient).sendRyhmasahkoposti(emailDataArgumentCaptor.capture());
        EmailData emailData = emailDataArgumentCaptor.getValue();
        assertThat(emailData.getRecipient()).hasSize(1);
        assertThat(emailData.getRecipient().get(0).getRecipientReplacements()).hasSize(3)
                .extracting("name").containsExactlyInAnyOrder("vastaanottaja", "rooli", "linkki");
        assertThat(emailData.getRecipient().get(0).getOid()).isEqualTo("1.2.3.4.5");
        assertThat(emailData.getRecipient().get(0).getEmail()).isEqualTo("arpa@kuutio.fi");
        assertThat(emailData.getRecipient().get(0).getName()).isEqualTo("arpa kuutio");
        assertThat(emailData.getRecipient().get(0).getLanguageCode()).isEqualTo("sv");
        assertThat(emailData.getRecipient().get(0).getOidType()).isEqualTo("henkilo");

        assertThat(emailData.getEmail().getLanguageCode()).isEqualTo("sv");
        assertThat(emailData.getEmail().getFrom()).isEqualTo(emailData.getEmail().getReplyTo()).isEqualTo("noreply@oph.fi");
        assertThat(emailData.getEmail().getCallingProcess()).isEqualTo("kayttooikeus");
    }

    @Test
    public void sendInvitationEmail() {
        OrganisaatioPerustieto organisaatioPerustieto = new OrganisaatioPerustieto();
        organisaatioPerustieto.setNimi(new HashMap<String, String>(){{put("fi", "suomenkielinennimi");}});
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
                .containsExactlyInAnyOrder("vastaanottaja", "organisaatiot", "linkki", "kutsuja", "voimassa");
        assertThat(emailData.getRecipient().get(0).getOid()).isEqualTo("");
        assertThat(emailData.getRecipient().get(0).getOidType()).isEqualTo("");
        assertThat(emailData.getRecipient().get(0).getEmail()).isEqualTo("arpa@kuutio.fi");
        assertThat(emailData.getRecipient().get(0).getName()).isEqualTo("arpa kuutio");
        assertThat(emailData.getRecipient().get(0).getLanguageCode()).isEqualTo("fi");

        assertThat(emailData.getEmail().getCallingProcess()).isEqualTo("kayttooikeus");
        assertThat(emailData.getEmail().getLanguageCode()).isEqualTo("fi");
        assertThat(emailData.getEmail().getFrom()).isEqualTo(emailData.getEmail().getReplyTo()).isEqualTo("noreply@oph.fi");
        assertThat(emailData.getEmail().getTemplateName()).isEqualTo("kayttooikeus_kutsu_v2");
    }
}
