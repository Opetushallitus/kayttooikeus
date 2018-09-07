package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.VahvaTunnistusRequestDto;
import fi.vm.sade.kayttooikeus.dto.VahvaTunnistusResponseDto;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.Identification;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;
import fi.vm.sade.kayttooikeus.model.TunnistusToken;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.repositories.IdentificationRepository;
import fi.vm.sade.kayttooikeus.repositories.TunnistusTokenDataRepository;
import fi.vm.sade.kayttooikeus.service.dto.HenkiloVahvaTunnistusDto;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.kayttooikeus.service.it.AbstractServiceIntegrationTest;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class VahvaTunnistusServiceTest extends AbstractServiceIntegrationTest {

    @Autowired
    private VahvaTunnistusService vahvaTunnistusService;
    @Autowired
    private IdentificationService identificationService;
    @Autowired
    private HenkiloDataRepository henkiloRepository;
    @Autowired
    private TunnistusTokenDataRepository tunnistusTokenRepository;
    @Autowired
    private IdentificationRepository identificationRepository;

    @MockBean
    private OppijanumerorekisteriClient oppijanumerorekisteriClient;

    @Before
    public void setup() {
        when(oppijanumerorekisteriClient.getHenkiloByOid(any()))
                .thenAnswer(invocation -> newHenkiloDto(invocation.getArgument(0)));
    }

    private HenkiloDto newHenkiloDto(String oid) {
        return HenkiloDto.builder()
                .oidHenkilo(oid)
                .kutsumanimi("kutsumanimi")
                .etunimet("etunimet")
                .sukunimi("suknimi")
                .build();
    }

    private Henkilo saveHenkilo(String oid, String kayttajatunnus) {
        Henkilo henkilo = new Henkilo(oid);
        Kayttajatiedot kayttajatiedot = new Kayttajatiedot();
        kayttajatiedot.setUsername(kayttajatunnus);
        kayttajatiedot.setHenkilo(henkilo);
        henkilo.setKayttajatiedot(kayttajatiedot);
        return henkiloRepository.save(henkilo);
    }

    @Test
    public void tunnistaudu() {
        String oid = "oid123";
        String kayttajatunnus = "kayttajatunnus123";
        Henkilo henkilo = saveHenkilo(oid, kayttajatunnus);
        String hetu = "hetu123";
        String tyosahkopostiosoite = "etu.suku@example.com";
        String loginToken = identificationService.createLoginToken(oid, false, null);
        identificationService.updateLoginToken(loginToken, hetu);

        VahvaTunnistusRequestDto requestDto = new VahvaTunnistusRequestDto();
        requestDto.setSalasana("Salasana123!");
        requestDto.setTyosahkopostiosoite(tyosahkopostiosoite);
        VahvaTunnistusResponseDto responseDto = vahvaTunnistusService.tunnistaudu(loginToken, requestDto);

        assertThat(responseDto)
                .extracting(VahvaTunnistusResponseDto::getAuthToken)
                .isNotEmpty();
        assertThat(tunnistusTokenRepository.findByLoginToken(loginToken))
                .map(TunnistusToken::getKaytetty)
                .isNotEmpty();
        assertThat(identificationRepository.findAll())
                .extracting(Identification::getHenkilo, Identification::getAuthtoken)
                .containsExactly(tuple(henkilo, responseDto.getAuthToken()));
        ArgumentCaptor<HenkiloVahvaTunnistusDto> henkiloVahvaTunnistusDtoCaptor = ArgumentCaptor.forClass(HenkiloVahvaTunnistusDto.class);
        verify(oppijanumerorekisteriClient).setStrongIdentifiedHetu(eq(oid), henkiloVahvaTunnistusDtoCaptor.capture());
        HenkiloVahvaTunnistusDto henkiloVahvaTunnistusDto = henkiloVahvaTunnistusDtoCaptor.getValue();
        assertThat(henkiloVahvaTunnistusDto)
                .extracting(HenkiloVahvaTunnistusDto::getHetu, HenkiloVahvaTunnistusDto::getTyosahkopostiosoite)
                .containsExactly(hetu, tyosahkopostiosoite);
    }

    @Test
    public void tunnistauduIlmanTyosahkopostia() {
        String oid = "oid123";
        String kayttajatunnus = "kayttajatunnus123";
        Henkilo henkilo = saveHenkilo(oid, kayttajatunnus);
        String hetu = "hetu123";
        String tyosahkopostiosoite = "";
        String loginToken = identificationService.createLoginToken(oid, false, null);
        identificationService.updateLoginToken(loginToken, hetu);

        VahvaTunnistusRequestDto requestDto = new VahvaTunnistusRequestDto();
        requestDto.setSalasana("Salasana123!");
        requestDto.setTyosahkopostiosoite(tyosahkopostiosoite);
        VahvaTunnistusResponseDto responseDto = vahvaTunnistusService.tunnistaudu(loginToken, requestDto);

        assertThat(responseDto)
                .extracting(VahvaTunnistusResponseDto::getAuthToken)
                .isNotEmpty();
        assertThat(tunnistusTokenRepository.findByLoginToken(loginToken))
                .map(TunnistusToken::getKaytetty)
                .isNotEmpty();
        assertThat(identificationRepository.findAll())
                .extracting(Identification::getHenkilo, Identification::getAuthtoken)
                .containsExactly(tuple(henkilo, responseDto.getAuthToken()));
        ArgumentCaptor<HenkiloVahvaTunnistusDto> henkiloVahvaTunnistusDtoCaptor = ArgumentCaptor.forClass(HenkiloVahvaTunnistusDto.class);
        verify(oppijanumerorekisteriClient).setStrongIdentifiedHetu(eq(oid), henkiloVahvaTunnistusDtoCaptor.capture());
        HenkiloVahvaTunnistusDto henkiloVahvaTunnistusDto = henkiloVahvaTunnistusDtoCaptor.getValue();
        assertThat(henkiloVahvaTunnistusDto)
                .extracting(HenkiloVahvaTunnistusDto::getHetu, HenkiloVahvaTunnistusDto::getTyosahkopostiosoite)
                .containsExactly(hetu, null);
    }

}
