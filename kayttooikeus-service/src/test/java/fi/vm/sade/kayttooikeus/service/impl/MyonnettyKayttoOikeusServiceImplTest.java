package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.HenkiloVarmentaja;
import fi.vm.sade.kayttooikeus.model.MyonnettyKayttoOikeusRyhmaTapahtuma;
import fi.vm.sade.kayttooikeus.model.OrganisaatioHenkilo;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.repositories.KayttoOikeusRyhmaTapahtumaHistoriaDataRepository;
import fi.vm.sade.kayttooikeus.repositories.MyonnettyKayttoOikeusRyhmaTapahtumaRepository;
import fi.vm.sade.kayttooikeus.repositories.OrganisaatioHenkiloRepository;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService;
import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import org.assertj.core.util.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static fi.vm.sade.kayttooikeus.util.CreateUtil.createHenkilo;
import static fi.vm.sade.kayttooikeus.util.CreateUtil.createMyonnettyKayttoOikeusRyhmaTapahtumaWithOrganisation;
import static fi.vm.sade.kayttooikeus.util.CreateUtil.createOrganisaatioHenkilo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
    private OrganisaatioHenkiloRepository organisaatioHenkiloRepository;

    @Mock
    private LdapSynchronizationService ldapSynchronizationService;

    @Test
    public void varmentajallaOnYhaOikeuksiaSamaanOrganisaatioon() {
        Henkilo henkilo = Henkilo.builder().oidHenkilo("kasittelija").build();
        given(this.henkiloDataRepository.findByOidHenkilo(eq("kasittelija"))).willReturn(Optional.of(henkilo));

        Henkilo varmennettavaHenkilo = Henkilo.builder()
                .oidHenkilo("varmennettava")
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
        poistuvanOikeudenOrganisaatioHenkilo.setMyonnettyKayttoOikeusRyhmas(new HashSet<>(kayttoOikeudet));
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
        verify(kayttoOikeusRyhmaTapahtumaHistoriaDataRepository, times(1)).save(any());
        verify(myonnettyKayttoOikeusRyhmaTapahtumaRepository, times(1)).delete(any());
        verify(ldapSynchronizationService, times(1)).updateHenkilo(any());
    }

    @Test
    public void varmentajallaEiOleEnaaOikeuksiaSamaanOrganisaatioon() {
        Henkilo henkilo = Henkilo.builder().oidHenkilo("kasittelija").build();
        given(this.henkiloDataRepository.findByOidHenkilo(eq("kasittelija"))).willReturn(Optional.of(henkilo));

        Henkilo varmennettavaHenkilo = Henkilo.builder()
                .oidHenkilo("varmennettava")
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
        poistuvanOikeudenOrganisaatioHenkilo.setMyonnettyKayttoOikeusRyhmas(new HashSet<>(kayttoOikeudet));
        given(this.myonnettyKayttoOikeusRyhmaTapahtumaRepository.findByVoimassaLoppuPvmBefore(any())).willReturn(kayttoOikeudet);

        List<MyonnettyKayttoOikeusRyhmaTapahtuma> varmentajanOikeudet = new ArrayList<>();
        varmentajanOikeudet.add(poistuvaKayttooikeus);
        given(this.myonnettyKayttoOikeusRyhmaTapahtumaRepository.findByOrganisaatioHenkiloHenkiloOidHenkilo(eq("varmentaja")))
                .willReturn(varmentajanOikeudet);

        this.myonnettyKayttoOikeusService.poistaVanhentuneet("kasittelija");

        assertThat(henkiloVarmentaja.isTila()).isFalse();
        verify(kayttoOikeusRyhmaTapahtumaHistoriaDataRepository, times(1)).save(any());
        verify(myonnettyKayttoOikeusRyhmaTapahtumaRepository, times(1)).delete(any());
        verify(ldapSynchronizationService, times(1)).updateHenkilo(any());
    }

    @Test
    public void poistaVanhentuneetTest() {
        given(henkiloDataRepository.findByOidHenkilo(any())).willReturn(Optional.of(createHenkilo("1.2.3.4.5")));

        // Set up myonnettykayttooikeusryhmatapahtumas
        OrganisaatioHenkilo oh1 = createOrganisaatioHenkilo("org1_oid", false);
        OrganisaatioHenkilo oh2 = createOrganisaatioHenkilo("org2_oid", false);

        oh1.setHenkilo(createHenkilo("h1_oid"));
        oh2.setHenkilo(createHenkilo("h2_oid"));

        MyonnettyKayttoOikeusRyhmaTapahtuma oh1ko1 = createMyonnettyKayttoOikeusRyhmaTapahtumaWithOrganisation(10L, 20L, "org1_oid");
        oh1ko1.setOrganisaatioHenkilo(oh1);
        MyonnettyKayttoOikeusRyhmaTapahtuma oh1ko2 = createMyonnettyKayttoOikeusRyhmaTapahtumaWithOrganisation(11L, 21L, "org2_oid");
        oh1ko2.setOrganisaatioHenkilo(oh2);
        MyonnettyKayttoOikeusRyhmaTapahtuma oh2ko1 = createMyonnettyKayttoOikeusRyhmaTapahtumaWithOrganisation(12L, 22L, "org3_oid");
        oh2ko1.setOrganisaatioHenkilo(oh2);

        // Organisaatiohenkilo1 kayttooikeusryhmas
        HashSet<MyonnettyKayttoOikeusRyhmaTapahtuma> oh1Kayttooikeusryhmas  = Sets.newHashSet();
        oh1Kayttooikeusryhmas.add(oh1ko1);
        oh1Kayttooikeusryhmas.add(oh1ko2);
        oh1.setMyonnettyKayttoOikeusRyhmas(oh1Kayttooikeusryhmas);

        // Organisaatiohenkilo2 kayttooikeusryhmas
        HashSet<MyonnettyKayttoOikeusRyhmaTapahtuma> oh2Kayttooikeusryhmas = Sets.newHashSet();
        oh2Kayttooikeusryhmas.add(oh2ko1);
        oh2.setMyonnettyKayttoOikeusRyhmas(oh2Kayttooikeusryhmas);

        // oh1ko2 ei ole vanhentumassa
        given(myonnettyKayttoOikeusRyhmaTapahtumaRepository.findByVoimassaLoppuPvmBefore(any())).willReturn(Arrays.asList(oh1ko1,oh2ko1));

        // Run
        myonnettyKayttoOikeusService.poistaVanhentuneet("1.2.3.4.5");

        // Assert
        ArgumentCaptor<OrganisaatioHenkilo> organisaatioHenkiloArgumentCaptor = ArgumentCaptor.forClass(OrganisaatioHenkilo.class);
        verify(this.organisaatioHenkiloRepository, times(2)).save(organisaatioHenkiloArgumentCaptor.capture());

        List<OrganisaatioHenkilo> organisaatioHenkiloList = organisaatioHenkiloArgumentCaptor.getAllValues();

        OrganisaatioHenkilo result1 = organisaatioHenkiloList.get(0);
        OrganisaatioHenkilo result2 = organisaatioHenkiloList.get(1);

        assertThat(result1.isPassivoitu()).isFalse();
        assertThat(result2.isPassivoitu()).isTrue();
        assertThat(result1.getMyonnettyKayttoOikeusRyhmas().size()).isEqualTo(1);
        assertThat(result2.getMyonnettyKayttoOikeusRyhmas().size()).isEqualTo(0);

    }
}
