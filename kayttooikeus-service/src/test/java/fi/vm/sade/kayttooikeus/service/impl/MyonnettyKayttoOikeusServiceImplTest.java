package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.HenkiloVarmentaja;
import fi.vm.sade.kayttooikeus.model.MyonnettyKayttoOikeusRyhmaTapahtuma;
import fi.vm.sade.kayttooikeus.model.OrganisaatioHenkilo;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.repositories.KayttoOikeusRyhmaTapahtumaHistoriaDataRepository;
import fi.vm.sade.kayttooikeus.repositories.MyonnettyKayttoOikeusRyhmaTapahtumaRepository;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService;
import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
public class MyonnettyKayttoOikeusServiceImplTest {
    @InjectMocks
    private MyonnettyKayttoOikeusServiceImpl myonnettyKayttoOikeusService;

    @Mock
    private PermissionCheckerService permissionCheckerService;

    @Mock
    private HenkiloDataRepository henkiloDataRepository;

    @Mock
    private MyonnettyKayttoOikeusRyhmaTapahtumaRepository myonnettyKayttoOikeusRyhmaTapahtumaRepository;

    @Mock
    private KayttoOikeusRyhmaTapahtumaHistoriaDataRepository kayttoOikeusRyhmaTapahtumaHistoriaDataRepository;

    @Mock
    private LdapSynchronizationService ldapSynchronizationService;

    @Test
    public void varmentajallaOnYhaOikeuksiaSamaanOrganisaatioon() {
        Henkilo henkilo = Henkilo.builder().oidHenkilo("kasittelija").build();
        given(this.henkiloDataRepository.findByOidHenkilo(eq("kasittelija"))).willReturn(Optional.of(henkilo));

        Henkilo varmennettavaHenkilo = Henkilo.builder()
                .oidHenkilo("varmennettava")
                .organisaatioHenkilos(Collections.singleton(OrganisaatioHenkilo.builder()
                        .passivoitu(false)
                        .organisaatioOid("1.2.0.0.1")
                        .build()))
                .build();
        HenkiloVarmentaja henkiloVarmentaja = new HenkiloVarmentaja();
        henkiloVarmentaja.setTila(true);
        henkiloVarmentaja.setVarmennettavaHenkilo(varmennettavaHenkilo);

        Henkilo varmentavaHenkilo = Henkilo.builder().oidHenkilo("varmentaja")
                .henkiloVarmennettavas(Collections.singleton(henkiloVarmentaja))
                .build();
        henkiloVarmentaja.setVarmentavaHenkilo(varmentavaHenkilo);
        OrganisaatioHenkilo poistuvanOikeudenOrganisaatioHenkilo = OrganisaatioHenkilo.builder()
                .organisaatioOid("1.2.0.0.1")
                .henkilo(varmentavaHenkilo)
                .build();
        MyonnettyKayttoOikeusRyhmaTapahtuma poistuvaKayttooikeus = MyonnettyKayttoOikeusRyhmaTapahtuma.builder()
                .organisaatioHenkilo(poistuvanOikeudenOrganisaatioHenkilo)
                .build();
        poistuvaKayttooikeus.setId(1L);

        List<MyonnettyKayttoOikeusRyhmaTapahtuma> kayttoOikeudet = new ArrayList<>();
        kayttoOikeudet.add(poistuvaKayttooikeus);
        given(this.myonnettyKayttoOikeusRyhmaTapahtumaRepository.findByVoimassaLoppuPvmBefore(any())).willReturn(kayttoOikeudet);

        OrganisaatioHenkilo yhaOlemassaOlevanOikeudenOrganisaatioHenkilo = OrganisaatioHenkilo.builder()
                .organisaatioOid("1.2.0.0.1")
                .build();
        MyonnettyKayttoOikeusRyhmaTapahtuma yhaOlemassaOlevaKayttooikeus = MyonnettyKayttoOikeusRyhmaTapahtuma.builder()
                .organisaatioHenkilo(yhaOlemassaOlevanOikeudenOrganisaatioHenkilo)
                .build();
        yhaOlemassaOlevaKayttooikeus.setId(2L);
        List<MyonnettyKayttoOikeusRyhmaTapahtuma> varmentajanOikeudet = new ArrayList<>();
        varmentajanOikeudet.add(poistuvaKayttooikeus);
        varmentajanOikeudet.add(yhaOlemassaOlevaKayttooikeus);
        given(this.myonnettyKayttoOikeusRyhmaTapahtumaRepository.findByOrganisaatioHenkiloHenkiloOidHenkilo(eq("varmentaja")))
                .willReturn(varmentajanOikeudet);

        this.myonnettyKayttoOikeusService.poistaVanhentuneet("kasittelija");

        assertThat(henkiloVarmentaja.isTila()).isTrue();
    }
}
