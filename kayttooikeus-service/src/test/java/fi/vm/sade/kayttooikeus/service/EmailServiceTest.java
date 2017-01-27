package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.TextGroupDto;
import fi.vm.sade.kayttooikeus.dto.YhteystietojenTyypit;
import fi.vm.sade.kayttooikeus.repositories.dto.ExpiringKayttoOikeusDto;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.kayttooikeus.service.external.RyhmasahkopostiClient;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkilonYhteystiedotViewDto;
import fi.vm.sade.oppijanumerorekisteri.dto.KielisyysDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystiedotDto;
import fi.vm.sade.ryhmasahkoposti.api.dto.EmailData;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.apache.http.HttpVersion.HTTP_1_1;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
public class EmailServiceTest extends AbstractServiceTest {
    @MockBean
    private RyhmasahkopostiClient ryhmasahkopostiClient;
    @MockBean
    private OppijanumerorekisteriClient oppijanumerorekisteriClient;

    @Autowired
    private EmailService emailService;

    @Test
    public void sendExpirationReminderTest() {
        HenkiloPerustietoDto perustiedot = new HenkiloPerustietoDto();
        KielisyysDto kielisyys = new KielisyysDto();
        kielisyys.setKieliKoodi("FI");
        perustiedot.setAsiointiKieli(kielisyys);
        given(oppijanumerorekisteriClient.getHenkilonPerustiedot("1.2.3.4.5")).willReturn(of(perustiedot));
        given(oppijanumerorekisteriClient.getHenkilonYhteystiedot("1.2.3.4.5")).willReturn(new HenkilonYhteystiedotViewDto()
            .put(YhteystietojenTyypit.TYOOSOITE, YhteystiedotDto.builder().sahkoposti("testi@example.com").build()));
        given(ryhmasahkopostiClient.sendRyhmasahkoposti(any(EmailData.class)))
                .willReturn(new BasicHttpResponse(new BasicStatusLine(HTTP_1_1, 200, "")));
        emailService.sendExpirationReminder("1.2.3.4.5", asList(
                ExpiringKayttoOikeusDto.builder()
                    .henkiloOid("1.2.3.4.5")
                    .myonnettyTapahtumaId(1L)
                    .ryhmaName("RYHMA")
                    .ryhmaDescription(new TextGroupDto(2L).put("FI", "Kuvaus")
                            .put("EN", "Desc"))
                    .voimassaLoppuPvm(new LocalDate().plusMonths(3))
                .build(),
                ExpiringKayttoOikeusDto.builder()
                    .henkiloOid("1.2.3.4.5")
                    .myonnettyTapahtumaId(3L)
                    .ryhmaName("RYHMA2")
                    .ryhmaDescription(new TextGroupDto(3L).put("FI", "Kuvaus2")
                            .put("EN", "Desc2"))
                    .voimassaLoppuPvm(new LocalDate().plusMonths(3))
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
}
