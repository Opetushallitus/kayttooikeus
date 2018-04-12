package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.KayttajaTyyppi;
import fi.vm.sade.kayttooikeus.dto.VirkailijaCreateDto;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.repositories.KayttajatiedotRepository;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.kayttooikeus.service.it.AbstractServiceIntegrationTest;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloCreateDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class VirkailijaServiceTest extends AbstractServiceIntegrationTest {

    @Autowired
    private VirkailijaService virkailijaService;
    @Autowired
    private HenkiloDataRepository henkiloRepository;
    @Autowired
    private KayttajatiedotRepository kayttajatiedotRepository;
    @MockBean
    private OppijanumerorekisteriClient oppijanumerorekisteriClient;

    @Test
    public void create() {
        VirkailijaCreateDto createDto = new VirkailijaCreateDto();
        createDto.setEtunimet("teppo juhani");
        createDto.setKutsumanimi("teppo");
        createDto.setSukunimi("testaaja");
        createDto.setKayttajatunnus("testaaja");
        createDto.setSalasana("Salasana123!");
        createDto.setVahvastiTunnistettu(true);
        when(oppijanumerorekisteriClient.createHenkilo(any())).thenReturn("oid123");

        String oid = virkailijaService.create(createDto);

        assertThat(oid).isEqualTo("oid123");
        ArgumentCaptor<HenkiloCreateDto> henkiloCreateDtoCaptor = ArgumentCaptor.forClass(HenkiloCreateDto.class);
        verify(oppijanumerorekisteriClient).createHenkilo(henkiloCreateDtoCaptor.capture());
        HenkiloCreateDto henkiloCreateDto = henkiloCreateDtoCaptor.getValue();
        assertThat(henkiloCreateDto)
                .extracting(HenkiloCreateDto::getEtunimet, HenkiloCreateDto::getKutsumanimi, HenkiloCreateDto::getSukunimi)
                .containsExactly("teppo juhani", "teppo", "testaaja");
        assertThat(henkiloRepository.findByOidHenkilo("oid123")).hasValueSatisfying(virkailija -> {
            assertThat(virkailija.getKayttajaTyyppi()).isEqualByComparingTo(KayttajaTyyppi.VIRKAILIJA);
            assertThat(virkailija.getVahvastiTunnistettu()).isTrue();
        });
        assertThat(kayttajatiedotRepository.findByHenkiloOidHenkilo("oid123")).hasValueSatisfying(kayttajatiedot -> {
            assertThat(kayttajatiedot.getUsername()).isEqualTo("testaaja");
            assertThat(kayttajatiedot.getPassword()).isNotNull();
            assertThat(kayttajatiedot.getSalt()).isNotNull();
        });
    }

}
