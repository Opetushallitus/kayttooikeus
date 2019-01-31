package fi.vm.sade.kayttooikeus.service;

import com.google.common.collect.Lists;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import fi.vm.sade.kayttooikeus.dto.KayttajaTyyppi;
import fi.vm.sade.kayttooikeus.dto.MyonnettyKayttoOikeusDto;
import fi.vm.sade.kayttooikeus.model.QMyonnettyKayttoOikeusRyhmaTapahtuma;
import fi.vm.sade.kayttooikeus.model.QOrganisaatioHenkilo;
import fi.vm.sade.kayttooikeus.repositories.KayttoOikeusRyhmaRepository;
import fi.vm.sade.kayttooikeus.repositories.KayttoOikeusRyhmaRepositoryCustom;
import fi.vm.sade.kayttooikeus.repositories.KayttoOikeusRyhmaTapahtumaHistoriaRepository;
import fi.vm.sade.kayttooikeus.repositories.MyonnettyKayttoOikeusRyhmaTapahtumaRepository;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.kayttooikeus.service.it.AbstractServiceIntegrationTest;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
public class KayttoOikeusServiceUnitTest extends AbstractServiceIntegrationTest {
    @Autowired
    private KayttoOikeusService kayttoOikeusService;

    @MockBean
    private KayttoOikeusRyhmaRepository kayttoOikeusRyhmaRepositoryMock;

    @MockBean
    private MyonnettyKayttoOikeusRyhmaTapahtumaRepository myonnettyKayttoOikeusRyhmaTapahtumaRepository;

    @MockBean
    private KayttoOikeusRyhmaTapahtumaHistoriaRepository kayttoOikeusRyhmaTapahtumaHistoriaRepository;

    @MockBean
    private LocalizationService localizationService;

    @MockBean
    private OppijanumerorekisteriClient oppijanumerorekisteriClient;

    @Test
    public void findKayttooikeusryhmatAndOrganisaatioByHenkiloOidTest() {
        String orgOid = "1.0.0.102.1";
        QOrganisaatioHenkilo organisaatioHenkilo = QOrganisaatioHenkilo.organisaatioHenkilo;
        QMyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma
                = QMyonnettyKayttoOikeusRyhmaTapahtuma.myonnettyKayttoOikeusRyhmaTapahtuma;
        Tuple x = Projections.tuple(organisaatioHenkilo.organisaatioOid, myonnettyKayttoOikeusRyhmaTapahtuma.kayttoOikeusRyhma.id)
                .newInstance(orgOid, 12001L);
        given(this.kayttoOikeusRyhmaRepositoryMock.findOrganisaatioOidAndRyhmaIdByHenkiloOid("1.2.3.4.5"))
                .willReturn(Collections.singletonList(x));
        Map<String, List<Integer>> result = kayttoOikeusService.findKayttooikeusryhmatAndOrganisaatioByHenkiloOid("1.2.3.4.5");
        assertThat(result.containsKey(orgOid)).isTrue();
        assertThat(result.get(orgOid).get(0)).isEqualTo(12001);
    }

    @Test
    public void listMyonnettyKayttoOikeusRyhmasByHenkiloAndOrganisaatio() {
        HenkiloPerustietoDto henkiloPerustietoDto = HenkiloPerustietoDto.builder()
                .kutsumanimi("arpa")
                .sukunimi("kuutio")
                .oidHenkilo("1.2.3.4.1")
                .build();
        MyonnettyKayttoOikeusDto myonnettyKayttoOikeusDto = MyonnettyKayttoOikeusDto.builder()
                .kasittelijaOid("1.2.3.4.1")
                .sallittuKayttajatyyppi(KayttajaTyyppi.PALVELU)
                .build();
        MyonnettyKayttoOikeusDto myonnettyKayttoOikeusDto2 = MyonnettyKayttoOikeusDto.builder()
                .kasittelijaOid("1.2.3.4.2")
                .build();
        given(this.myonnettyKayttoOikeusRyhmaTapahtumaRepository.findByHenkiloInOrganisaatio(eq("1.2.3.4.5"), isNull()))
                .willReturn(Lists.newArrayList(myonnettyKayttoOikeusDto, myonnettyKayttoOikeusDto2));
        given(this.kayttoOikeusRyhmaTapahtumaHistoriaRepository.findByHenkiloInOrganisaatio(eq("1.2.3.4.5"), isNull()))
                .willReturn(Lists.newArrayList());
        given(this.localizationService.localize(anyList())).willAnswer(returnsFirstArg());

        given(this.oppijanumerorekisteriClient.getHenkilonPerustiedot(anyCollection()))
                .willReturn(Lists.newArrayList(henkiloPerustietoDto));
        List<MyonnettyKayttoOikeusDto> myonnettyKayttooikeusList = this.kayttoOikeusService
                .listMyonnettyKayttoOikeusRyhmasByHenkiloAndOrganisaatio("1.2.3.4.5", null);
        assertThat(myonnettyKayttooikeusList)
                .extracting(MyonnettyKayttoOikeusDto::getKasittelijaOid, MyonnettyKayttoOikeusDto::getKasittelijaNimi, MyonnettyKayttoOikeusDto::getSallittuKayttajatyyppi)
                .containsExactlyInAnyOrder(tuple("1.2.3.4.1", "arpa kuutio", KayttajaTyyppi.PALVELU), tuple("1.2.3.4.2", "1.2.3.4.2", null));
    }
}
