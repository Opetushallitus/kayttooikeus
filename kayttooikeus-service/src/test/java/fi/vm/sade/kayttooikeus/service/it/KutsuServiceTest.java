package fi.vm.sade.kayttooikeus.service.it;

import com.google.common.collect.Sets;
import fi.vm.sade.kayttooikeus.aspects.HenkiloHelper;
import fi.vm.sade.kayttooikeus.controller.KutsuPopulator;
import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.dto.enumeration.KutsuView;
import fi.vm.sade.kayttooikeus.dto.enumeration.OrganisaatioStatus;
import fi.vm.sade.kayttooikeus.enumeration.KutsuOrganisaatioOrder;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.IdentificationRepository;
import fi.vm.sade.kayttooikeus.repositories.OrganisaatioHenkiloRepository;
import fi.vm.sade.kayttooikeus.repositories.criteria.KutsuCriteria;
import fi.vm.sade.kayttooikeus.repositories.dto.HenkiloCreateByKutsuDto;
import fi.vm.sade.kayttooikeus.repositories.populate.*;
import fi.vm.sade.kayttooikeus.service.KayttooikeusAnomusService;
import fi.vm.sade.kayttooikeus.service.KutsuService;
import fi.vm.sade.kayttooikeus.service.OrganisaatioService;
import fi.vm.sade.kayttooikeus.service.exception.ForbiddenException;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import fi.vm.sade.kayttooikeus.service.external.RyhmasahkopostiClient;
import fi.vm.sade.kayttooikeus.util.YhteystietoUtil;
import fi.vm.sade.oppijanumerorekisteri.dto.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fi.vm.sade.kayttooikeus.controller.KutsuPopulator.kutsu;
import static fi.vm.sade.kayttooikeus.model.Identification.HAKA_AUTHENTICATION_IDP;
import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.palvelukayttaja;
import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.virkailija;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusPopulator.oikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaMyontoViitePopulator.kayttoOikeusRyhmaMyontoViite;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma;
import static fi.vm.sade.kayttooikeus.repositories.populate.KutsuOrganisaatioPopulator.kutsuOrganisaatio;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloKayttoOikeusPopulator.myonnettyKayttoOikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloPopulator.organisaatioHenkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.TextGroupPopulator.text;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@WithMockUser
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class KutsuServiceTest extends AbstractServiceIntegrationTest {

    private static final String TEST_PASWORD = "This_is_example_of_strong_password";

    @Autowired
    private KutsuService kutsuService;

    @Autowired
    private IdentificationRepository identificationRepository;

    @Autowired
    private OrganisaatioHenkiloRepository organisaatioHenkiloRepository;

    @SpyBean
    private KayttooikeusAnomusService kayttooikeusAnomusService;
    
    @MockBean
    private OrganisaatioClient organisaatioClient;

    @MockBean
    private RyhmasahkopostiClient ryhmasahkopostiClient;

    @MockBean
    private OppijanumerorekisteriClient oppijanumerorekisteriClient;

    @MockBean
    private HenkiloHelper henkiloHelper;

    @MockBean
    private OrganisaatioService organisaatioService;

    @Captor
    private ArgumentCaptor<Collection<KayttoOikeusRyhma>> kayttooikeusRyhmaCollectionCaptor;

    @Before
    public void setup() {
        doNothing().when(this.oppijanumerorekisteriClient).updateHenkilo(any(HenkiloUpdateDto.class));
    }

    @Test
    @WithMockUser(username = "1.2.4", authorities = "ROLE_APP_KAYTTOOIKEUS_CRUD")
    public void listAvoinKutsus() {
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.3", "1.2.3.4.5"),
                kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_CRUD))));
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.4", "1.2.3.4.5"),
                kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_CRUD))));
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.4", "1.2.3.4.6"),
                kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_CRUD))));
        given(this.organisaatioClient.listWithChildOids(eq("1.2.3.4.5"), any())).willReturn(singleton("1.2.3.4.5"));
        given(this.organisaatioClient.listWithChildOids(eq("1.2.3.4.6"), any())).willReturn(singleton("1.2.3.4.6"));
        populate(kutsu("Essi", "Esimerkki", "a@eaxmple.com")
                .kutsuja("1.2.3").aikaleima(LocalDateTime.of(2016,1,1,0,0,0, 0))
                .organisaatio(kutsuOrganisaatio("1.2.3.4.5")
                        .ryhma(kayttoOikeusRyhma("RYHMA1"))
                ));
        Kutsu kutsu2 = populate(kutsu("Matti", "Meikäläinen", "b@eaxmple.com")
                .kutsuja("1.2.4").aikaleima(LocalDateTime.of(2016,2,1,0,0,0,0))
                .organisaatio(kutsuOrganisaatio("1.2.3.4.5")
                        .ryhma(kayttoOikeusRyhma("RYHMA2")))
                .organisaatio(kutsuOrganisaatio("1.2.3.4.6")
                        .ryhma(kayttoOikeusRyhma("RYHMA3")))
        );
        populate(kutsu("Eero", "Esimerkki", "c@eaxmple.com")
                .tila(KutsunTila.POISTETTU)
                .kutsuja("1.2.4").aikaleima(LocalDateTime.of(2016,1,1,0,0,0,0))
                .organisaatio(kutsuOrganisaatio("1.2.3.4.5").ryhma(kayttoOikeusRyhma("RYHMA1"))
                ));

        OrganisaatioPerustieto org1 = new OrganisaatioPerustieto();
        org1.setOid("1.2.3.4.5");
        org1.setNimi(new TextGroupMapDto().put("fi", "Nimi2").asMap());
        OrganisaatioPerustieto org2 = new OrganisaatioPerustieto();
        org2.setOid("1.2.3.4.6");
        org2.setNimi(new TextGroupMapDto().put("fi", "Nimi1").asMap());
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(eq("1.2.3.4.5")))
                .willReturn(Optional.of(org1));
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(eq("1.2.3.4.6")))
                .willReturn(Optional.of(org2));
        
        List<KutsuReadDto> kutsus = kutsuService.listKutsus(KutsuOrganisaatioOrder.AIKALEIMA, Sort.Direction.ASC, KutsuCriteria.builder().searchTerm("matti meikäläinen").build(), null, null);
        assertEquals(1, kutsus.size());
        assertEquals(LocalDateTime.of(2016,2,1,0,0,0,0), kutsus.get(0).getAikaleima());
        assertEquals(kutsu2.getId(), kutsus.get(0).getId());
        assertEquals("b@eaxmple.com", kutsus.get(0).getSahkoposti());
        assertEquals(2, kutsus.get(0).getOrganisaatiot().size());
        assertThat(kutsus).flatExtracting(KutsuReadDto::getOrganisaatiot)
                .extracting(KutsuReadDto.KutsuOrganisaatioReadDto::getOrganisaatioOid)
                .containsExactlyInAnyOrder("1.2.3.4.5", "1.2.3.4.6");
        assertThat(kutsus).extracting(KutsuReadDto::getEtunimi).containsExactlyInAnyOrder("Matti");
        assertThat(kutsus).extracting(KutsuReadDto::getSukunimi).containsExactlyInAnyOrder("Meikäläinen");
        assertThat(kutsus).flatExtracting(KutsuReadDto::getOrganisaatiot)
                .extracting(KutsuReadDto.KutsuOrganisaatioReadDto::getNimi)
                .extracting(TextGroupMapDto::getTexts)
                .extracting(map -> map.get("fi"))
                .containsExactlyInAnyOrder("Nimi1", "Nimi2");
    }

    @Test
    @WithMockUser(username = "1.2.4", authorities = {"ROLE_APP_KAYTTOOIKEUS_CRUD", "ROLE_APP_KAYTTOOIKEUS_CRUD_1.2.246.562.10.00000000001"})
    public void listAvoinKutsusWithMiniAdminAndOrganisationIsForcedWithOphView() {
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.3", "1.2.246.562.10.00000000001"),
                kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_CRUD))));
        populate(kutsu("Essi", "Esimerkki", "a@eaxmple.com")
                .kutsuja("1.2.3").aikaleima(LocalDateTime.of(2016, 1, 1, 0, 0, 0, 0))
                .organisaatio(kutsuOrganisaatio("1.2.246.562.10.00000000001")
                        .ryhma(kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_CRUD)))));
        given(this.organisaatioClient.listWithChildOids(eq("1.2.246.562.10.00000000001"), any())).willReturn(singleton("1.2.246.562.10.00000000001"));
        OrganisaatioPerustieto org1 = new OrganisaatioPerustieto();
        org1.setOid("1.2.246.562.10.00000000001");
        org1.setNimi(new TextGroupMapDto().put("fi", "Nimi2").asMap());
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(eq("1.2.246.562.10.00000000001")))
                .willReturn(Optional.of(org1));

        // Does not allow changing organisaatio with ophView
        List<KutsuReadDto> kutsuList = this.kutsuService.listKutsus(KutsuOrganisaatioOrder.AIKALEIMA,
                Sort.Direction.ASC,
                KutsuCriteria.builder().kutsujaOrganisaatioOid("1.2.3.4.5").view(KutsuView.OPH).build(),
                null,
                null);
        assertThat(kutsuList)
                .flatExtracting(KutsuReadDto::getOrganisaatiot)
                .flatExtracting(KutsuReadDto.KutsuOrganisaatioReadDto::getOrganisaatioOid)
                .containsExactly("1.2.246.562.10.00000000001");
    }

    @Test
    @WithMockUser(username = "1.2.4", authorities = {"ROLE_APP_KAYTTOOIKEUS_CRUD", "ROLE_APP_KAYTTOOIKEUS_CRUD_1.2.3.4.5"})
    public void listAvoinKutsusWithMiniAdminAndKayttooikeusryhmaView() {
        MyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma = populate(
                myonnettyKayttoOikeus(organisaatioHenkilo("1.2.3", "1.2.3.4.5"),
                kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_CRUD))));
        populate(kutsu("Essi", "Esimerkki", "a@eaxmple.com")
                .kutsuja("1.2.3").aikaleima(LocalDateTime.of(2016, 1, 1, 0, 0, 0, 0))
                .organisaatio(kutsuOrganisaatio("1.2.3.4.5")
                        .ryhma(kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_CRUD)))));
        OrganisaatioPerustieto org1 = new OrganisaatioPerustieto();
        org1.setOid("1.2.3.4.5");
        org1.setNimi(new TextGroupMapDto().put("fi", "Nimi2").asMap());
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(eq("1.2.3.4.5")))
                .willReturn(Optional.of(org1));
        given(this.organisaatioClient.listWithChildOids(eq("1.2.3.4.5"), any())).willReturn(singleton("1.2.3.4.5"));

        List<KutsuReadDto> kutsuList = this.kutsuService.listKutsus(KutsuOrganisaatioOrder.AIKALEIMA,
                Sort.Direction.ASC,
                KutsuCriteria.builder().kutsujaKayttooikeusryhmaIds(Sets.newHashSet(999L)).view(KutsuView.KAYTTOOIKEUSRYHMA).build(),
                null,
                null);
        assertThat(kutsuList)
                .flatExtracting(KutsuReadDto::getOrganisaatiot)
                .flatExtracting(KutsuReadDto.KutsuOrganisaatioReadDto::getKayttoOikeusRyhmat)
                .extracting(KutsuReadDto.KutsuKayttoOikeusRyhmaReadDto::getId)
                .containsExactly(myonnettyKayttoOikeusRyhmaTapahtuma.getKayttoOikeusRyhma().getId());
    }

    @Test
    @WithMockUser(username = "1.2.4", authorities = {"ROLE_APP_KAYTTOOIKEUS_CRUD", "ROLE_APP_KAYTTOOIKEUS_CRUD_1.2.3.4.5"})
    public void listAvoinKutsusWithNormalUserAndOrganisationIsForced() {
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.3", "1.2.3.4.5"),
                kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_CRUD))));
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.4", "1.2.3.4.5"),
                kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_CRUD))));
        populate(kutsu("Essi", "Esimerkki", "a@eaxmple.com")
                .kutsuja("1.2.3").aikaleima(LocalDateTime.of(2016, 1, 1, 0, 0, 0, 0))
                .organisaatio(kutsuOrganisaatio("1.2.3.4.5")
                        .ryhma(kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_CRUD)))));
        populate(kutsu("Essi", "Esimerkki", "a@eaxmple.com")
                .kutsuja("1.2.3").aikaleima(LocalDateTime.of(2016, 1, 1, 0, 0, 0, 0))
                .organisaatio(kutsuOrganisaatio("1.2.246.562.10.00000000001")
                        .ryhma(kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_CRUD)))));
        given(this.organisaatioClient.listWithChildOids(eq("1.2.246.562.10.00000000001"), any())).willReturn(singleton("1.2.246.562.10.00000000001"));
        given(this.organisaatioClient.listWithChildOids(eq("1.2.3.4.5"), any())).willReturn(singleton("1.2.3.4.5"));
        OrganisaatioPerustieto org = new OrganisaatioPerustieto();
        org.setOid("1.2.3.4.5");
        org.setNimi(new TextGroupMapDto().put("fi", "Nimi2").asMap());
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(eq("1.2.3.4.5")))
                .willReturn(Optional.of(org));

        List<KutsuReadDto> kutsuList = this.kutsuService.listKutsus(KutsuOrganisaatioOrder.AIKALEIMA,
                Sort.Direction.ASC,
                KutsuCriteria.builder().build(),
                null,
                null);
        assertThat(kutsuList)
                .flatExtracting(KutsuReadDto::getOrganisaatiot)
                .flatExtracting(KutsuReadDto.KutsuOrganisaatioReadDto::getOrganisaatioOid)
                .containsExactly("1.2.3.4.5");
    }

    @Test
    @WithMockUser(username = "1.2.4", authorities = {"ROLE_APP_KAYTTOOIKEUS_CRUD", "ROLE_APP_KAYTTOOIKEUS_CRUD_1.2.3.4.5"})
    public void listAvoinKutsusWithNormalUserByKayttooikeusryhmaId() {
        MyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma
                = populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.4", "1.2.3.4.5"),
                kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_CRUD))));
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("kutsujaOid", "1.2.3.4.5"),
                kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_CRUD))));
        populate(kutsu("Essi", "Esimerkki", "a@eaxmple.com")
                .kutsuja("kutsujaOid").aikaleima(LocalDateTime.of(2016, 1, 1, 0, 0, 0, 0))
                .organisaatio(kutsuOrganisaatio("1.2.3.4.5")
                        .ryhma(kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_CRUD)))));
        populate(kutsu("Essi", "Esimerkki", "a@eaxmple.com")
                .kutsuja("kutsujaOid").aikaleima(LocalDateTime.of(2016, 1, 1, 0, 0, 0, 0))
                .organisaatio(kutsuOrganisaatio("1.2.246.562.10.00000000001")
                        .ryhma(kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_CRUD)))));
        given(this.organisaatioClient.listWithChildOids(eq("1.2.3.4.5"), any())).willReturn(singleton("1.2.3.4.5"));
        OrganisaatioPerustieto org = new OrganisaatioPerustieto();
        org.setOid("1.2.3.4.5");
        org.setNimi(new TextGroupMapDto().put("fi", "Nimi2").asMap());
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(eq("1.2.3.4.5")))
                .willReturn(Optional.of(org));

        // Ryhmä user has not rights to will be set to all his ryhmas
        List<KutsuReadDto> kutsuList = this.kutsuService.listKutsus(KutsuOrganisaatioOrder.AIKALEIMA,
                Sort.Direction.ASC,
                KutsuCriteria.builder().kayttooikeusryhmaIds(Sets.newHashSet(myonnettyKayttoOikeusRyhmaTapahtuma.getKayttoOikeusRyhma().getId())).build(),
                null,
                null);
        assertThat(kutsuList)
                .flatExtracting(KutsuReadDto::getOrganisaatiot)
                .flatExtracting(KutsuReadDto.KutsuOrganisaatioReadDto::getOrganisaatioOid)
                .containsExactly("1.2.3.4.5");
        assertThat(kutsuList)
                .flatExtracting(KutsuReadDto::getOrganisaatiot)
                .flatExtracting(KutsuReadDto.KutsuOrganisaatioReadDto::getKayttoOikeusRyhmat)
                .extracting(KutsuReadDto.KutsuKayttoOikeusRyhmaReadDto::getId)
                .containsExactly(myonnettyKayttoOikeusRyhmaTapahtuma.getKayttoOikeusRyhma().getId());
    }

    @Test
    @WithMockUser(username = "1.2.4", authorities = {"ROLE_APP_KAYTTOOIKEUS_CRUD", "ROLE_APP_KAYTTOOIKEUS_CRUD_1.2.246.562.10.2"})
    public void listKutsusPassivoituOrganisaatioNormaaliVirkailija() {
        HenkiloPopulator kutsuja = HenkiloPopulator.henkilo("kutsujaOid");
        populate(myonnettyKayttoOikeus(organisaatioHenkilo(kutsuja, "1.2.246.562.10.1").passivoitu(), kayttoOikeusRyhma("käyttöoikeusryhmä1")));
        populate(myonnettyKayttoOikeus(organisaatioHenkilo(kutsuja, "1.2.246.562.10.2"), kayttoOikeusRyhma("käyttöoikeusryhmä2")));
        Kutsu kutsu = populate(kutsu("Essi", "Esimerkki", "a@example.com").kutsuja("kutsujaOid").organisaatio(kutsuOrganisaatio("1.2.246.562.10.1")));

        KutsuOrganisaatioOrder order = KutsuOrganisaatioOrder.AIKALEIMA;
        Sort.Direction direction = Sort.Direction.ASC;
        KutsuCriteria criteria = new KutsuCriteria();

        List<KutsuReadDto> kutsuList = kutsuService.listKutsus(order, direction, criteria, null, null);

        assertThat(kutsuList).isEmpty();
    }

    @Test
    @WithMockUser(username = "1.2.4", authorities = {"ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA", "ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA_1.2.246.562.10.00000000001"})
    public void listKutsusPassivoituOrganisaatioRekisterinpitaja() {
        HenkiloPopulator kutsuja = HenkiloPopulator.henkilo("kutsujaOid");
        populate(myonnettyKayttoOikeus(organisaatioHenkilo(kutsuja, "1.2.246.562.10.1").passivoitu(), kayttoOikeusRyhma("käyttöoikeusryhmä1")));
        Kutsu kutsu = populate(kutsu("Essi", "Esimerkki", "a@example.com").kutsuja("kutsujaOid").organisaatio(kutsuOrganisaatio("1.2.246.562.10.1")));

        KutsuOrganisaatioOrder order = KutsuOrganisaatioOrder.AIKALEIMA;
        Sort.Direction direction = Sort.Direction.ASC;
        KutsuCriteria criteria = new KutsuCriteria();

        List<KutsuReadDto> kutsuList = kutsuService.listKutsus(order, direction, criteria, null, null);

        assertThat(kutsuList).extracting(KutsuReadDto::getId).containsExactly(kutsu.getId());
    }

    @Test
    @WithMockUser(username = "1.2.4", authorities = {"ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA", "ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA_1.2.246.562.10.00000000001"})
    public void createKutsuAsAdmin() {
        given(ryhmasahkopostiClient.sendRyhmasahkoposti(any())).willReturn("12345");
        doReturn(HenkiloDto.builder()
                .kutsumanimi("kutsun")
                .sukunimi("kutsuja")
                .yksiloityVTJ(true)
                .hetu("valid hetu")
                .build())
                .when(this.oppijanumerorekisteriClient).getHenkiloByOid(anyString());

        OrganisaatioPerustieto org1 = new OrganisaatioPerustieto();
        org1.setOid("1.2.246.562.10.00000000001");
        org1.setNimi(new TextGroupMapDto().put("FI", "Opetushallitus").asMap());
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(eq("1.2.246.562.10.00000000001")))
                .willReturn(Optional.of(org1));
        
        MyonnettyKayttoOikeusRyhmaTapahtuma tapahtuma = populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(virkailija("1.2.4"), "1.2.246.562.10.00000000001"),
                kayttoOikeusRyhma("kayttoOikeusRyhma")
                        .withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_CRUD))
                        .withNimi(text("fi", "Käyttöoikeusryhmä"))));
        Long kayttoOikeusRyhmaId = tapahtuma.getKayttoOikeusRyhma().getId();
        KutsuCreateDto.KutsuKayttoOikeusRyhmaCreateDto kutsuKayttoOikeusRyhma = new KutsuCreateDto.KutsuKayttoOikeusRyhmaCreateDto();
        kutsuKayttoOikeusRyhma.setId(kayttoOikeusRyhmaId);

        KutsuCreateDto kutsu = new KutsuCreateDto();
        kutsu.setEtunimi("Etu");
        kutsu.setSukunimi("Suku");
        kutsu.setSahkoposti("example@example.com");
        kutsu.setAsiointikieli(Asiointikieli.fi);
        kutsu.setOrganisaatiot(new LinkedHashSet<>());
        KutsuCreateDto.KutsuOrganisaatioCreateDto kutsuOrganisaatio = new KutsuCreateDto.KutsuOrganisaatioCreateDto();
        kutsuOrganisaatio.setOrganisaatioOid("1.2.246.562.10.00000000001");
        kutsuOrganisaatio.setKayttoOikeusRyhmat(Stream.of(kutsuKayttoOikeusRyhma).collect(toSet()));
        kutsu.getOrganisaatiot().add(kutsuOrganisaatio);

        long id = kutsuService.createKutsu(kutsu);
        KutsuReadDto tallennettu = kutsuService.getKutsu(id);

        assertThat(tallennettu.getAsiointikieli()).isEqualByComparingTo(Asiointikieli.fi);
        assertThat(tallennettu.getOrganisaatiot())
                .hasSize(1)
                .flatExtracting(KutsuReadDto.KutsuOrganisaatioReadDto::getKayttoOikeusRyhmat)
                .hasSize(1)
                .extracting(KutsuReadDto.KutsuKayttoOikeusRyhmaReadDto::getNimi)
                .flatExtracting(TextGroupMapDto::getTexts)
                .extracting("fi")
                .containsExactly("Käyttöoikeusryhmä");

        Kutsu entity = em.find(Kutsu.class, id);
        assertThat(entity.getSalaisuus()).isNotEmpty();
    }

    @Test
    @WithMockUser(username = "1.2.4", authorities = {"ROLE_APP_KAYTTOOIKEUS_CRUD", "ROLE_APP_KAYTTOOIKEUS_CRUD_1.2.3.4.5"})
    public void createKutsuAsNormalUser() {
        given(this.ryhmasahkopostiClient.sendRyhmasahkoposti(any())).willReturn("12345");
        doReturn(HenkiloDto.builder()
                .kutsumanimi("kutsun")
                .sukunimi("kutsuja")
                .yksiloityVTJ(true)
                .hetu("valid hetu")
                .build())
                .when(this.oppijanumerorekisteriClient).getHenkiloByOid(anyString());

        OrganisaatioPerustieto org1 = new OrganisaatioPerustieto();
        org1.setOid("1.2.3.4.1");
        org1.setNimi(new TextGroupMapDto().put("FI", "Kutsuttu organisaatio").asMap());
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(eq("1.2.3.4.1")))
                .willReturn(Optional.of(org1));
        given(this.organisaatioClient.listWithParentsAndChildren(eq("1.2.3.4.1"), any()))
                .willReturn(asList(org1));
        OrganisaatioPerustieto org2 = new OrganisaatioPerustieto();
        org2.setOid("1.2.3.4.5");
        org2.setNimi(new TextGroupMapDto().put("FI", "Käyttäjän organisaatio").asMap());
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(eq("1.2.3.4.5")))
                .willReturn(Optional.of(org2));
        given(this.organisaatioClient.listWithParentsAndChildren(eq("1.2.3.4.5"), any()))
                .willReturn(asList(org2));

        MyonnettyKayttoOikeusRyhmaTapahtuma myonnetty = populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(virkailija("1.2.4"), "1.2.3.4.5"),
                kayttoOikeusRyhma("kayttoOikeusRyhma")
                        .withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_CRUD))));
        KayttoOikeusRyhma myonnettava = populate(kayttoOikeusRyhma("RYHMA2")
                .withOrganisaatiorajoite("1.2.3.4.1")
                .withNimi(text("fi", "Käyttöoikeusryhmä")));
        populate(kayttoOikeusRyhmaMyontoViite(myonnetty.getKayttoOikeusRyhma().getId(),
                myonnettava.getId()));

        KutsuCreateDto.KutsuKayttoOikeusRyhmaCreateDto kutsuKayttoOikeusRyhma = new KutsuCreateDto.KutsuKayttoOikeusRyhmaCreateDto();
        kutsuKayttoOikeusRyhma.setId(myonnettava.getId());
        given(this.organisaatioClient.listWithChildOids(eq("1.2.3.4.5"), any()))
                .willReturn(Stream.of("1.2.3.4.5", "1.2.3.4.1").collect(toSet()));

        KutsuCreateDto kutsu = new KutsuCreateDto();
        kutsu.setEtunimi("Etu");
        kutsu.setSukunimi("Suku");
        kutsu.setSahkoposti("example@example.com");
        kutsu.setAsiointikieli(Asiointikieli.fi);
        kutsu.setOrganisaatiot(new LinkedHashSet<>());
        KutsuCreateDto.KutsuOrganisaatioCreateDto kutsuOrganisaatio = new KutsuCreateDto.KutsuOrganisaatioCreateDto();
        kutsuOrganisaatio.setOrganisaatioOid("1.2.3.4.1");
        kutsuOrganisaatio.setKayttoOikeusRyhmat(Stream.of(kutsuKayttoOikeusRyhma).collect(toSet()));
        kutsu.getOrganisaatiot().add(kutsuOrganisaatio);

        long id = kutsuService.createKutsu(kutsu);
        KutsuReadDto tallennettu = kutsuService.getKutsu(id);

        assertThat(tallennettu.getAsiointikieli()).isEqualByComparingTo(Asiointikieli.fi);
        assertThat(tallennettu.getOrganisaatiot())
                .hasSize(1)
                .flatExtracting(KutsuReadDto.KutsuOrganisaatioReadDto::getKayttoOikeusRyhmat)
                .hasSize(1)
                .extracting(KutsuReadDto.KutsuKayttoOikeusRyhmaReadDto::getNimi)
                .flatExtracting(TextGroupMapDto::getTexts)
                .extracting("fi")
                .containsExactly("Käyttöoikeusryhmä");

        Kutsu entity = this.em.find(Kutsu.class, id);
        assertThat(entity.getSalaisuus()).isNotEmpty();
    }

    @Test
    @WithMockUser(username = "1.2.4", authorities = {"ROLE_APP_KAYTTOOIKEUS_KUTSU_CRUD", "ROLE_APP_KAYTTOOIKEUS_KUTSU_CRUD_1.2.3.4.5"})
    public void createKutsuAsNormalUserWithKutsuCrud() {
        given(this.ryhmasahkopostiClient.sendRyhmasahkoposti(any())).willReturn("12345");
        doReturn(HenkiloDto.builder()
                .kutsumanimi("kutsun")
                .sukunimi("kutsuja")
                .yksiloityVTJ(true)
                .hetu("valid hetu")
                .build())
                .when(this.oppijanumerorekisteriClient).getHenkiloByOid(anyString());

        OrganisaatioPerustieto org1 = new OrganisaatioPerustieto();
        org1.setOid("1.2.3.4.1");
        org1.setNimi(new TextGroupMapDto().put("FI", "Kutsuttu organisaatio").asMap());
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(eq("1.2.3.4.1")))
                .willReturn(Optional.of(org1));
        given(this.organisaatioClient.listWithParentsAndChildren(eq("1.2.3.4.1"), any()))
                .willReturn(asList(org1));
        OrganisaatioPerustieto org2 = new OrganisaatioPerustieto();
        org2.setOid("1.2.3.4.5");
        org2.setNimi(new TextGroupMapDto().put("FI", "Käyttäjän organisaatio").asMap());
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(eq("1.2.3.4.5")))
                .willReturn(Optional.of(org2));
        given(this.organisaatioClient.listWithParentsAndChildren(eq("1.2.3.4.5"), any()))
                .willReturn(asList(org2));

        MyonnettyKayttoOikeusRyhmaTapahtuma myonnetty = populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(virkailija("1.2.4"), "1.2.3.4.5"),
                kayttoOikeusRyhma("kayttoOikeusRyhma")
                        .withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_KUTSU_CRUD))));
        KayttoOikeusRyhma myonnettava = populate(kayttoOikeusRyhma("RYHMA2")
                .withOrganisaatiorajoite("1.2.3.4.1")
                .withNimi(text("fi", "Käyttöoikeusryhmä")));
        populate(kayttoOikeusRyhmaMyontoViite(myonnetty.getKayttoOikeusRyhma().getId(),
                myonnettava.getId()));

        KutsuCreateDto.KutsuKayttoOikeusRyhmaCreateDto kutsuKayttoOikeusRyhma = new KutsuCreateDto.KutsuKayttoOikeusRyhmaCreateDto();
        kutsuKayttoOikeusRyhma.setId(myonnettava.getId());
        given(this.organisaatioClient.listWithChildOids(eq("1.2.3.4.5"), any()))
                .willReturn(Stream.of("1.2.3.4.5", "1.2.3.4.1").collect(toSet()));

        KutsuCreateDto kutsu = new KutsuCreateDto();
        kutsu.setEtunimi("Etu");
        kutsu.setSukunimi("Suku");
        kutsu.setSahkoposti("example@example.com");
        kutsu.setAsiointikieli(Asiointikieli.fi);
        kutsu.setOrganisaatiot(new LinkedHashSet<>());
        KutsuCreateDto.KutsuOrganisaatioCreateDto kutsuOrganisaatio = new KutsuCreateDto.KutsuOrganisaatioCreateDto();
        kutsuOrganisaatio.setOrganisaatioOid("1.2.3.4.1");
        kutsuOrganisaatio.setKayttoOikeusRyhmat(Stream.of(kutsuKayttoOikeusRyhma).collect(toSet()));
        kutsu.getOrganisaatiot().add(kutsuOrganisaatio);

        long id = kutsuService.createKutsu(kutsu);
        KutsuReadDto tallennettu = kutsuService.getKutsu(id);

        assertThat(tallennettu.getAsiointikieli()).isEqualByComparingTo(Asiointikieli.fi);
        assertThat(tallennettu.getOrganisaatiot())
                .hasSize(1)
                .flatExtracting(KutsuReadDto.KutsuOrganisaatioReadDto::getKayttoOikeusRyhmat)
                .hasSize(1)
                .extracting(KutsuReadDto.KutsuKayttoOikeusRyhmaReadDto::getNimi)
                .flatExtracting(TextGroupMapDto::getTexts)
                .extracting("fi")
                .containsExactly("Käyttöoikeusryhmä");

        Kutsu entity = this.em.find(Kutsu.class, id);
        assertThat(entity.getSalaisuus()).isNotEmpty();
    }

    @Test
    @WithMockUser(username = "1.2.3", authorities = {"ROLE_APP_KAYTTOOIKEUS_KUTSU_CRUD", "ROLE_APP_KAYTTOOIKEUS_KUTSU_CRUD_1.2.3.4.5"})
    public void createKutsuAsPalvelukayttaja() {
        given(this.ryhmasahkopostiClient.sendRyhmasahkoposti(any())).willReturn("12345");
        doReturn(HenkiloDto.builder()
                .kutsumanimi("kutsun")
                .sukunimi("kutsuja")
                .yksiloityVTJ(true)
                .hetu("valid hetu")
                .build())
                .when(this.oppijanumerorekisteriClient).getHenkiloByOid(anyString());

        OrganisaatioPerustieto org1 = new OrganisaatioPerustieto();
        org1.setOid("1.2.3.4.1");
        org1.setNimi(new TextGroupMapDto().put("FI", "Kutsuttu organisaatio").asMap());
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(eq("1.2.3.4.1")))
                .willReturn(Optional.of(org1));
        given(this.organisaatioClient.listWithParentsAndChildren(eq("1.2.3.4.1"), any()))
                .willReturn(asList(org1));
        OrganisaatioPerustieto org2 = new OrganisaatioPerustieto();
        org2.setOid("1.2.3.4.5");
        org2.setNimi(new TextGroupMapDto().put("FI", "Käyttäjän organisaatio").asMap());
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(eq("1.2.3.4.5")))
                .willReturn(Optional.of(org2));
        given(this.organisaatioClient.listWithParentsAndChildren(eq("1.2.3.4.5"), any()))
                .willReturn(asList(org2));

        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(palvelukayttaja("1.2.3"), "1.2.3.4.5"),
                kayttoOikeusRyhma("RYHMA3")
                        .withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_KUTSU_CRUD))));
        MyonnettyKayttoOikeusRyhmaTapahtuma myonnetty = populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(virkailija("1.2.4"), "1.2.3.4.5"),
                kayttoOikeusRyhma("kayttoOikeusRyhma")
                        .withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_CRUD))));
        KayttoOikeusRyhma myonnettava = populate(kayttoOikeusRyhma("RYHMA2")
                .withOrganisaatiorajoite("1.2.3.4.1")
                .withNimi(text("fi", "Käyttöoikeusryhmä")));
        populate(kayttoOikeusRyhmaMyontoViite(myonnetty.getKayttoOikeusRyhma().getId(),
                myonnettava.getId()));

        KutsuCreateDto.KutsuKayttoOikeusRyhmaCreateDto kutsuKayttoOikeusRyhma = new KutsuCreateDto.KutsuKayttoOikeusRyhmaCreateDto();
        kutsuKayttoOikeusRyhma.setId(myonnettava.getId());
        given(this.organisaatioClient.getActiveParentOids(eq("1.2.3.4.1")))
                .willReturn(asList("1.2.3.4.1", "1.2.3.4.5"));
        given(this.organisaatioClient.listWithChildOids(eq("1.2.3.4.5"), any()))
                .willReturn(Stream.of("1.2.3.4.5", "1.2.3.4.1").collect(toSet()));

        KutsuCreateDto kutsu = new KutsuCreateDto();
        kutsu.setKutsujaOid("1.2.4");
        kutsu.setEtunimi("Etu");
        kutsu.setSukunimi("Suku");
        kutsu.setSahkoposti("example@example.com");
        kutsu.setAsiointikieli(Asiointikieli.fi);
        kutsu.setOrganisaatiot(new LinkedHashSet<>());
        KutsuCreateDto.KutsuOrganisaatioCreateDto kutsuOrganisaatio = new KutsuCreateDto.KutsuOrganisaatioCreateDto();
        kutsuOrganisaatio.setOrganisaatioOid("1.2.3.4.1");
        kutsuOrganisaatio.setKayttoOikeusRyhmat(Stream.of(kutsuKayttoOikeusRyhma).collect(toSet()));
        kutsu.getOrganisaatiot().add(kutsuOrganisaatio);

        long id = kutsuService.createKutsu(kutsu);
        KutsuReadDto tallennettu = kutsuService.getKutsu(id);

        assertThat(tallennettu)
                .returns(Asiointikieli.fi, KutsuReadDto::getAsiointikieli)
                .returns("1.2.4", KutsuReadDto::getKutsujaOid);
        assertThat(tallennettu.getOrganisaatiot())
                .hasSize(1)
                .flatExtracting(KutsuReadDto.KutsuOrganisaatioReadDto::getKayttoOikeusRyhmat)
                .hasSize(1)
                .extracting(KutsuReadDto.KutsuKayttoOikeusRyhmaReadDto::getNimi)
                .flatExtracting(TextGroupMapDto::getTexts)
                .extracting("fi")
                .containsExactly("Käyttöoikeusryhmä");

        Kutsu entity = this.em.find(Kutsu.class, id);
        assertThat(entity.getSalaisuus()).isNotEmpty();
    }

    @Test(expected = ForbiddenException.class)
    @WithMockUser(username = "1.2.4", authorities = {"ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA", "ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA_1.2.246.562.10.00000000001"})
    public void createKutsuAsAdminWithNoHetuOrVtjYksiloity() {
        doReturn(HenkiloDto.builder()
                .kutsumanimi("kutsun")
                .sukunimi("kutsuja")
                .build())
                .when(this.oppijanumerorekisteriClient).getHenkiloByOid(anyString());
        populate(virkailija("1.2.4"));
        // This kind of kutsu is not actually allowed on api.
        this.kutsuService.createKutsu(KutsuCreateDto.builder().organisaatiot(new HashSet<>()).build());
    }

    // Assert that existing yhteystiedot won't be overrun
    @Test
    public void addEmailToNewHenkiloUpdateDto() {
        HenkiloUpdateDto henkiloUpdateDto = new HenkiloUpdateDto();
        String kutsuEmail = "kutsumail@domain.com";

        ReflectionTestUtils.invokeMethod(this.kutsuService, "addEmailToNewHenkiloUpdateDto", henkiloUpdateDto, kutsuEmail);
        assertThat(henkiloUpdateDto.getYhteystiedotRyhma().size()).isEqualTo(1);

        HashSet<YhteystietoDto> allYhteystiedot = new HashSet<>();
        henkiloUpdateDto.getYhteystiedotRyhma().forEach(yr -> allYhteystiedot.addAll(yr.getYhteystieto()));
        List<String> yhteystietoArvot = allYhteystiedot.stream().map(y -> y.getYhteystietoArvo()).collect(Collectors.toList());
        assertTrue(yhteystietoArvot.contains("kutsumail@domain.com"));
    }

    //Assert that duplicate email addresses won't be created
    @Test
    public void addEmailToExistingHenkiloNoDuplicateEmailTest() {
        HenkiloUpdateDto henkiloUpdateDto = new HenkiloUpdateDto();
        String henkiloOid = "1.2.3.4.5";
        String kutsuEmail = "teppo.testi@domain.com";

        HenkiloDto existingHenkiloDto = new HenkiloDto();
        Set<YhteystiedotRyhmaDto> existingYhteystiedotRyhmaDtos = new HashSet<>();
        Set<YhteystietoDto> existingYhteystietoDtos = new HashSet<>();
        YhteystietoDto existingYhteystietoDto = new YhteystietoDto(YhteystietoTyyppi.YHTEYSTIETO_SAHKOPOSTI, kutsuEmail);
        existingYhteystietoDtos.add(existingYhteystietoDto);
        YhteystiedotRyhmaDto existingYhteystiedotRyhmaDto = new YhteystiedotRyhmaDto(null, YhteystietoUtil.TYOOSOITE, "alkupera6", true, existingYhteystietoDtos);
        existingYhteystiedotRyhmaDtos.add(existingYhteystiedotRyhmaDto);
        existingHenkiloDto.setYhteystiedotRyhma(existingYhteystiedotRyhmaDtos);
        given(this.oppijanumerorekisteriClient.getHenkiloByOid(eq(henkiloOid))).willReturn(existingHenkiloDto);

        this.kutsuService.addEmailToExistingHenkiloUpdateDto(henkiloOid, kutsuEmail, henkiloUpdateDto);

        assertThat(henkiloUpdateDto.getYhteystiedotRyhma().size()).isEqualTo(1);

        HashSet<YhteystietoDto> allYhteystiedot = new HashSet<>();
        henkiloUpdateDto.getYhteystiedotRyhma().forEach(yr -> allYhteystiedot.addAll(yr.getYhteystieto()));
        List<String> yhteystietoArvot = allYhteystiedot.stream().map(y -> y.getYhteystietoArvo()).collect(Collectors.toList());
        assertTrue(yhteystietoArvot.contains("teppo.testi@domain.com"));
    }
    
    //Assert that new email addresses are added and existing remains
    @Test
    public void addEmailToExistingHenkiloTest() {
        HenkiloUpdateDto henkiloUpdateDto = new HenkiloUpdateDto();
        String henkiloOid = "1.2.3.4.5";
        String kutsuEmail = "teppo.testi@domain.com";

        HenkiloDto existingHenkiloDto = new HenkiloDto();
        Set<YhteystiedotRyhmaDto> existingYhteystiedotRyhmaDtos = new HashSet<>();
        Set<YhteystietoDto> existingYhteystietoDtos = new HashSet<>();
        YhteystietoDto existingYhteystietoDto = new YhteystietoDto(YhteystietoTyyppi.YHTEYSTIETO_SAHKOPOSTI, "teppo.toinenposti@domain.com");
        existingYhteystietoDtos.add(existingYhteystietoDto);
        YhteystiedotRyhmaDto existingYhteystiedotRyhmaDto = new YhteystiedotRyhmaDto(null, YhteystietoUtil.TYOOSOITE, "alkupera6", true, existingYhteystietoDtos);
        existingYhteystiedotRyhmaDtos.add(existingYhteystiedotRyhmaDto);
        existingHenkiloDto.setYhteystiedotRyhma(existingYhteystiedotRyhmaDtos);
        given(this.oppijanumerorekisteriClient.getHenkiloByOid(eq(henkiloOid))).willReturn(existingHenkiloDto);

        this.kutsuService.addEmailToExistingHenkiloUpdateDto(henkiloOid, kutsuEmail, henkiloUpdateDto);
        assertThat(henkiloUpdateDto.getYhteystiedotRyhma().size()).isEqualTo(2);

        HashSet<YhteystietoDto> allYhteystiedot = new HashSet<>();
        henkiloUpdateDto.getYhteystiedotRyhma().forEach(yr -> allYhteystiedot.addAll(yr.getYhteystieto()));
        List<String> yhteystietoArvot = allYhteystiedot.stream().map(y -> y.getYhteystietoArvo()).collect(Collectors.toList());
        assertTrue(yhteystietoArvot.contains("teppo.testi@domain.com"));
        assertTrue(yhteystietoArvot.contains("teppo.toinenposti@domain.com"));

    }

    @Test
    @WithMockUser(username = "1.2.4", authorities = {"ROLE_APP_KAYTTOOIKEUS_CRUD", "ROLE_APP_KAYTTOOIKEUS_CRUD_1.2.3.4.5"})
    public void deleteKutsuTest() {
        Kutsu kutsu = populate(kutsu("Matti", "Mehiläinen", "b@eaxmple.com")
                .kutsuja("1.2.4")
                .organisaatio(kutsuOrganisaatio("1.2.3.4.5")
                        .ryhma(kayttoOikeusRyhma("RYHMA2")))
        );
        populate(organisaatioHenkilo("1.2.4", "1.2.3.4.5"));
        given(this.organisaatioClient.listWithChildOids(eq("1.2.3.4.5"), any())).willReturn(singleton("1.2.3.4.5"));
        this.kutsuService.deleteKutsu(kutsu.getId());
        this.em.flush();
        assertEquals(KutsunTila.POISTETTU, kutsu.getTila());
        assertEquals("1.2.4", kutsu.getPoistaja());
        assertNotNull(kutsu.getPoistettu());
    }
    
    @Test(expected = ForbiddenException.class)
    @WithMockUser(username = "1.2.4", authorities = "ROLE_APP_KAYTTOOIKEUS_CRUD")
    public void deleteKutsuOtherKutsujaWithoutProperAuthorityFails() {
        Kutsu kutsu = populate(kutsu("Matti", "Mehiläinen", "b@eaxmple.com")
                .kutsuja("1.2.5")
                .organisaatio(kutsuOrganisaatio("1.2.3.4.5")
                        .ryhma(kayttoOikeusRyhma("RYHMA2")))
        );
        this.kutsuService.deleteKutsu(kutsu.getId());
    }

    @Test
    public void getByTemporaryToken() {
        populate(KutsuPopulator.kutsu("arpa", "kuutio", "arpa@kuutio.fi")
                .temporaryToken("123")
                .hakaIdentifier("hakaidentifier"));
        KutsuReadDto kutsu = this.kutsuService.getByTemporaryToken("123");
        assertThat(kutsu.getAsiointikieli()).isEqualTo(Asiointikieli.fi);
        assertThat(kutsu.getEtunimi()).isEqualTo("arpa");
        assertThat(kutsu.getSukunimi()).isEqualTo("kuutio");
        assertThat(kutsu.getSahkoposti()).isEqualTo("arpa@kuutio.fi");
        assertThat(kutsu.getHakaIdentifier()).isTrue();
    }

    @Test
    public void createHenkilo() {
        given(this.oppijanumerorekisteriClient.getHenkilonPerustiedot(eq("1.2.3.4.1")))
                .willReturn(Optional.of(HenkiloPerustietoDto.builder().hetu("valid hetu").build()));
        Kutsu kutsu = populate(KutsuPopulator.kutsu("arpa", "kuutio", "arpa@kuutio.fi")
                .temporaryToken("123")
                .hetu("hetu")
                .kutsuja("1.2.3.4.1")
                .organisaatio(KutsuOrganisaatioPopulator.kutsuOrganisaatio("1.2.0.0.1")
                        .ryhma(KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma("ryhma").withNimi(text("FI", "Kuvaus")))));
        Henkilo henkilo = populate(HenkiloPopulator.henkilo("1.2.3.4.5"));
        populate(HenkiloPopulator.henkilo("1.2.3.4.1"));
        doReturn("1.2.3.4.5").when(this.oppijanumerorekisteriClient).createHenkilo(any(HenkiloCreateDto.class));
        doReturn(Optional.empty()).when(this.oppijanumerorekisteriClient).getHenkiloByHetu(any());
        doReturn("1.2.3.4.5").when(this.oppijanumerorekisteriClient).getOidByHetu("hetu");
        HenkiloCreateByKutsuDto henkiloCreateByKutsuDto = new HenkiloCreateByKutsuDto("arpa",
                new KielisyysDto("fi", null), "arpauser", TEST_PASWORD);

        OrganisaatioPerustieto organisaatio = OrganisaatioPerustieto.builder().status(OrganisaatioStatus.AKTIIVINEN).build();
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(any())).willReturn(Optional.of(organisaatio));
        this.kutsuService.createHenkilo("123", henkiloCreateByKutsuDto);
        assertThat(henkilo.getOidHenkilo()).isEqualTo("1.2.3.4.5");
        assertThat(henkilo.getKayttajatiedot().getUsername()).isEqualTo("arpauser");
        assertThat(henkilo.getKayttajatiedot().getPassword()).isNotEmpty();
        assertThat(organisaatioHenkiloRepository.findByHenkilo(henkilo))
                .flatExtracting(OrganisaatioHenkilo::getMyonnettyKayttoOikeusRyhmas)
                .extracting(MyonnettyKayttoOikeusRyhmaTapahtuma::getKayttoOikeusRyhma)
                .extracting(KayttoOikeusRyhma::getTunniste)
                .containsExactly("ryhma");
        assertThat(organisaatioHenkiloRepository.findByHenkilo(henkilo))
                .flatExtracting(OrganisaatioHenkilo::getMyonnettyKayttoOikeusRyhmas)
                .extracting(MyonnettyKayttoOikeusRyhmaTapahtuma::getKasittelija)
                .extracting(Henkilo::getOidHenkilo)
                .containsExactly("1.2.3.4.1");
        assertThat(organisaatioHenkiloRepository.findByHenkilo(henkilo))
                .flatExtracting(OrganisaatioHenkilo::getOrganisaatioOid)
                .containsExactly("1.2.0.0.1");

        assertThat(kutsu.getLuotuHenkiloOid()).isEqualTo(henkilo.getOidHenkilo());
        assertThat(kutsu.getTemporaryToken()).isNull();
        assertThat(kutsu.getTila()).isEqualTo(KutsunTila.KAYTETTY);
    }

    @Test
    public void vainPalvelulleSallittuJaPassivoituKayttooikeusRyhmaOhitetaan() {
        given(this.oppijanumerorekisteriClient.getHenkilonPerustiedot(eq("1.2.3.4.1")))
                .willReturn(Optional.of(HenkiloPerustietoDto.builder().hetu("valid hetu").build()));
        populate(KutsuPopulator.kutsu("arpa", "kuutio", "arpa@kuutio.fi")
                .temporaryToken("123")
                .hetu("hetu")
                .kutsuja("1.2.3.4.1")
                .organisaatio(KutsuOrganisaatioPopulator.kutsuOrganisaatio("1.2.0.0.1")
                        .ryhma(KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma("ryhma")
                                .withNimi(text("FI", "Kuvaus"))
                                .withSallittu(KayttajaTyyppi.PALVELU))
                        .ryhma(KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma("passivoituryhma")
                                .withNimi(text("FI", "Kuvaus"))
                                .asPassivoitu())
                ));
        populate(HenkiloPopulator.henkilo("1.2.3.4.5"));
        populate(HenkiloPopulator.henkilo("1.2.3.4.1"));
        doReturn("1.2.3.4.5").when(this.oppijanumerorekisteriClient).createHenkilo(any(HenkiloCreateDto.class));
        doReturn(Optional.empty()).when(this.oppijanumerorekisteriClient).getHenkiloByHetu(any());
        doReturn("1.2.3.4.5").when(this.oppijanumerorekisteriClient).getOidByHetu("hetu");
        HenkiloCreateByKutsuDto henkiloCreateByKutsuDto = new HenkiloCreateByKutsuDto("arpa",
                new KielisyysDto("fi", null), "arpauser", TEST_PASWORD);

        OrganisaatioPerustieto organisaatio = OrganisaatioPerustieto.builder().status(OrganisaatioStatus.AKTIIVINEN).build();
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(any())).willReturn(Optional.of(organisaatio));
        this.kutsuService.createHenkilo("123", henkiloCreateByKutsuDto);
        verify(this.kayttooikeusAnomusService).grantPreValidatedKayttooikeusryhma(anyString(), anyString(), any(), this.kayttooikeusRyhmaCollectionCaptor.capture(), anyString());
        assertThat(this.kayttooikeusRyhmaCollectionCaptor.getValue()).isEmpty();
    }

    @Test
    @WithMockUser("1.2.3.4.5")
    public void createHenkiloWhenKutsuCreatorHetuMatches() {
        given(this.oppijanumerorekisteriClient.getHenkilonPerustiedot(eq("1.2.3.4.1")))
                .willReturn(Optional.of(HenkiloPerustietoDto.builder().hetu("valid hetu").build()));
        Kutsu kutsu = populate(KutsuPopulator.kutsu("arpa", "kuutio", "arpa@kuutio.fi")
                .temporaryToken("123")
                .hetu("valid hetu")
                .kutsuja("1.2.3.4.1")
                .organisaatio(KutsuOrganisaatioPopulator.kutsuOrganisaatio("1.2.0.0.1")
                        .ryhma(KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma("ryhma").withNimi(text("FI", "Kuvaus")))));
        Henkilo henkilo = populate(HenkiloPopulator.henkilo("1.2.3.4.5"));
        populate(HenkiloPopulator.henkilo("1.2.3.4.1"));
        doReturn("1.2.3.4.5").when(this.oppijanumerorekisteriClient).createHenkilo(any(HenkiloCreateDto.class));
        doReturn("1.2.3.4.5").when(this.oppijanumerorekisteriClient).getOidByHetu("valid hetu");
        HenkiloCreateByKutsuDto henkiloCreateByKutsuDto = new HenkiloCreateByKutsuDto("arpa",
                new KielisyysDto("fi", null), "arpauser", TEST_PASWORD);
        HenkiloDto henkiloDto = new HenkiloDto();
        henkiloDto.setOidHenkilo("123");

        given(this.oppijanumerorekisteriClient.getHenkiloByOid(any())).willReturn(henkiloDto);
        this.kutsuService.createHenkilo("123", henkiloCreateByKutsuDto);
        assertThat(henkilo.getOidHenkilo()).isEqualTo("1.2.3.4.5");
        // Username/password won't change
        assertThat(henkilo.getKayttajatiedot()).isNull();
        // No organisation or kayttooikeusryhma are granted
        assertThat(henkilo.getOrganisaatioHenkilos()).isEmpty();

        assertThat(kutsu.getLuotuHenkiloOid()).isEqualTo(henkilo.getOidHenkilo());
        assertThat(kutsu.getTemporaryToken()).isNull();
        assertThat(kutsu.getTila()).isEqualTo(KutsunTila.KAYTETTY);
    }

    // Existing henkilo username changes to the new one
    @Test
    public void createHenkiloHetuExistsKayttajatiedotExists() {
        given(this.oppijanumerorekisteriClient.getHenkilonPerustiedot(eq("1.2.3.4.1")))
                .willReturn(Optional.of(HenkiloPerustietoDto.builder().hetu("valid hetu").build()));
        Kutsu kutsu = populate(KutsuPopulator.kutsu("arpa", "kuutio", "arpa@kuutio.fi")
                .temporaryToken("123")
                .hetu("hetu")
                .kutsuja("1.2.3.4.1")
                .organisaatio(KutsuOrganisaatioPopulator.kutsuOrganisaatio("1.2.0.0.1")
                        .ryhma(KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma("ryhma").withNimi(text("FI", "Kuvaus")))));
        populate(HenkiloPopulator.henkilo("1.2.3.4.5"));
        Henkilo henkilo = populate(OrganisaatioHenkiloPopulator.organisaatioHenkilo(
                HenkiloPopulator.henkilo("1.2.0.0.2").withUsername("old_username"),
                "2.1.0.1"))
                .getHenkilo();
        populate(HenkiloPopulator.henkilo("1.2.3.4.1"));
        doReturn(Optional.of(new HenkiloDto().builder().oidHenkilo("1.2.0.0.2").build())).when(this.oppijanumerorekisteriClient).getHenkiloByHetu("hetu");

        HenkiloCreateByKutsuDto henkiloCreateByKutsuDto = new HenkiloCreateByKutsuDto("arpa",
                new KielisyysDto("fi", null), "arpauser", TEST_PASWORD);
        given(this.oppijanumerorekisteriClient.getHenkiloByOid(any())).willReturn(new HenkiloDto());
        OrganisaatioPerustieto organisaatio = OrganisaatioPerustieto.builder().status(OrganisaatioStatus.AKTIIVINEN).build();
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(any())).willReturn(Optional.of(organisaatio));

        this.kutsuService.createHenkilo("123", henkiloCreateByKutsuDto);
        assertThat(henkilo.getOidHenkilo()).isEqualTo("1.2.0.0.2");
        assertThat(henkilo.getKayttajatiedot().getUsername()).isEqualTo("arpauser");
        assertThat(henkilo.getKayttajatiedot().getPassword()).isNotEmpty();
        assertThat(organisaatioHenkiloRepository.findByHenkilo(henkilo))
                .flatExtracting(OrganisaatioHenkilo::getMyonnettyKayttoOikeusRyhmas)
                .extracting(MyonnettyKayttoOikeusRyhmaTapahtuma::getKayttoOikeusRyhma)
                .extracting(KayttoOikeusRyhma::getTunniste)
                .containsExactly("ryhma");
        assertThat(organisaatioHenkiloRepository.findByHenkilo(henkilo))
                .flatExtracting(OrganisaatioHenkilo::getMyonnettyKayttoOikeusRyhmas)
                .extracting(MyonnettyKayttoOikeusRyhmaTapahtuma::getKasittelija)
                .extracting(Henkilo::getOidHenkilo)
                .containsExactly("1.2.3.4.1");
        assertThat(organisaatioHenkiloRepository.findByHenkilo(henkilo))
                .flatExtracting(OrganisaatioHenkilo::getOrganisaatioOid)
                .containsExactlyInAnyOrder("1.2.0.0.1", "2.1.0.1");

        assertThat(kutsu.getLuotuHenkiloOid()).isEqualTo(henkilo.getOidHenkilo());
        assertThat(kutsu.getTemporaryToken()).isNull();
        assertThat(kutsu.getTila()).isEqualTo(KutsunTila.KAYTETTY);
    }

    @Test
    public void createHenkiloHetuExistsKayttajatiedotSame() {
        given(this.oppijanumerorekisteriClient.getHenkilonPerustiedot(eq("1.2.3.4.1")))
                .willReturn(Optional.of(HenkiloPerustietoDto.builder().hetu("valid hetu").build()));
        Kutsu kutsu = populate(KutsuPopulator.kutsu("arpa", "kuutio", "arpa@kuutio.fi")
                .temporaryToken("123")
                .hetu("hetu")
                .kutsuja("1.2.3.4.1")
                .organisaatio(KutsuOrganisaatioPopulator.kutsuOrganisaatio("1.2.0.0.1")
                        .ryhma(KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma("ryhma").withNimi(text("FI", "Kuvaus")))));
        populate(HenkiloPopulator.henkilo("1.2.3.4.5"));
        Henkilo henkilo = populate(OrganisaatioHenkiloPopulator.organisaatioHenkilo(
                HenkiloPopulator.henkilo("1.2.0.0.2").withUsername("arpauser"),
                "2.1.0.1"))
                .getHenkilo();
        populate(HenkiloPopulator.henkilo("1.2.3.4.1"));
        doReturn(Optional.of(new HenkiloDto().builder().oidHenkilo("1.2.0.0.2").build())).when(this.oppijanumerorekisteriClient).getHenkiloByHetu("hetu");

        HenkiloCreateByKutsuDto henkiloCreateByKutsuDto = new HenkiloCreateByKutsuDto("arpa",
                new KielisyysDto("fi", null), "arpauser", TEST_PASWORD);
        given(this.oppijanumerorekisteriClient.getHenkiloByOid(any())).willReturn(new HenkiloDto());
        OrganisaatioPerustieto organisaatio = OrganisaatioPerustieto.builder().status(OrganisaatioStatus.AKTIIVINEN).build();
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(any())).willReturn(Optional.of(organisaatio));

        this.kutsuService.createHenkilo("123", henkiloCreateByKutsuDto);
        assertThat(henkilo.getOidHenkilo()).isEqualTo("1.2.0.0.2");
        assertThat(henkilo.getKayttajatiedot().getUsername()).isEqualTo("arpauser");
        assertThat(henkilo.getKayttajatiedot().getPassword()).isNotEmpty();
        assertThat(organisaatioHenkiloRepository.findByHenkilo(henkilo))
                .flatExtracting(OrganisaatioHenkilo::getMyonnettyKayttoOikeusRyhmas)
                .extracting(MyonnettyKayttoOikeusRyhmaTapahtuma::getKayttoOikeusRyhma)
                .extracting(KayttoOikeusRyhma::getTunniste)
                .containsExactly("ryhma");
        assertThat(organisaatioHenkiloRepository.findByHenkilo(henkilo))
                .flatExtracting(OrganisaatioHenkilo::getMyonnettyKayttoOikeusRyhmas)
                .extracting(MyonnettyKayttoOikeusRyhmaTapahtuma::getKasittelija)
                .extracting(Henkilo::getOidHenkilo)
                .containsExactly("1.2.3.4.1");
        assertThat(organisaatioHenkiloRepository.findByHenkilo(henkilo))
                .flatExtracting(OrganisaatioHenkilo::getOrganisaatioOid)
                .containsExactlyInAnyOrder("1.2.0.0.1", "2.1.0.1");

        assertThat(kutsu.getLuotuHenkiloOid()).isEqualTo(henkilo.getOidHenkilo());
        assertThat(kutsu.getTemporaryToken()).isNull();
        assertThat(kutsu.getTila()).isEqualTo(KutsunTila.KAYTETTY);
    }

    @Test
    public void createHenkiloWithHakaIdentifier() {
        given(this.oppijanumerorekisteriClient.getHenkilonPerustiedot(eq("1.2.3.4.1")))
                .willReturn(Optional.of(HenkiloPerustietoDto.builder().hetu("valid hetu").build()));
        Kutsu kutsu = populate(KutsuPopulator.kutsu("arpa", "kuutio", "arpa@kuutio.fi")
                .hakaIdentifier("!haka%Identifier1/")
                .temporaryToken("123")
                .hetu("hetu")
                .kutsuja("1.2.3.4.1")
                .organisaatio(KutsuOrganisaatioPopulator.kutsuOrganisaatio("1.2.0.0.1")
                        .ryhma(KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma("ryhma").withNimi(text("FI", "Kuvaus")))));
        Henkilo henkilo = populate(HenkiloPopulator.henkilo("1.2.3.4.5"));
        populate(HenkiloPopulator.henkilo("1.2.3.4.1"));
        doReturn("1.2.3.4.5").when(this.oppijanumerorekisteriClient).createHenkilo(any(HenkiloCreateDto.class));
        doReturn("1.2.3.4.5").when(this.oppijanumerorekisteriClient).getOidByHetu("hetu");
        HenkiloCreateByKutsuDto henkiloCreateByKutsuDto = new HenkiloCreateByKutsuDto("arpa",
                new KielisyysDto("fi", null), null, null);

        OrganisaatioPerustieto organisaatio = OrganisaatioPerustieto.builder().status(OrganisaatioStatus.AKTIIVINEN).build();
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(any())).willReturn(Optional.of(organisaatio));
        this.kutsuService.createHenkilo("123", henkiloCreateByKutsuDto);
        assertThat(henkilo.getOidHenkilo()).isEqualTo("1.2.3.4.5");
        assertThat(henkilo.getKayttajatiedot().getUsername()).matches("hakaIdentifier1[\\d]{3,3}");
        assertThat(henkilo.getKayttajatiedot().getPassword()).isNull();
        assertThat(organisaatioHenkiloRepository.findByHenkilo(henkilo))
                .flatExtracting(OrganisaatioHenkilo::getMyonnettyKayttoOikeusRyhmas)
                .extracting(MyonnettyKayttoOikeusRyhmaTapahtuma::getKayttoOikeusRyhma)
                .extracting(KayttoOikeusRyhma::getTunniste)
                .containsExactly("ryhma");
        assertThat(organisaatioHenkiloRepository.findByHenkilo(henkilo))
                .flatExtracting(OrganisaatioHenkilo::getMyonnettyKayttoOikeusRyhmas)
                .extracting(MyonnettyKayttoOikeusRyhmaTapahtuma::getKasittelija)
                .extracting(Henkilo::getOidHenkilo)
                .containsExactly("1.2.3.4.1");
        assertThat(organisaatioHenkiloRepository.findByHenkilo(henkilo))
                .flatExtracting(OrganisaatioHenkilo::getOrganisaatioOid)
                .containsExactly("1.2.0.0.1");

        assertThat(identificationRepository.findByHenkilo(henkilo))
                .filteredOn(identification -> identification.getIdpEntityId().equals(HAKA_AUTHENTICATION_IDP))
                .extracting(Identification::getIdentifier)
                .containsExactlyInAnyOrder("!haka%Identifier1/");

        assertThat(kutsu.getLuotuHenkiloOid()).isEqualTo(henkilo.getOidHenkilo());
        assertThat(kutsu.getTemporaryToken()).isNull();
        assertThat(kutsu.getTila()).isEqualTo(KutsunTila.KAYTETTY);
    }

    @Test
    public void createHenkiloWithDuplicateHakaIdentifier() {
        given(this.oppijanumerorekisteriClient.getHenkilonPerustiedot(eq("1.2.3.4.1")))
                .willReturn(Optional.of(HenkiloPerustietoDto.builder().hetu("valid hetu").build()));
        Kutsu kutsu = populate(KutsuPopulator.kutsu("arpa", "kuutio", "arpa@kuutio.fi")
                .hakaIdentifier("!haka%Identifier1/")
                .temporaryToken("123")
                .hetu("hetu")
                .kutsuja("1.2.3.4.1")
                .organisaatio(KutsuOrganisaatioPopulator.kutsuOrganisaatio("1.2.0.0.1")
                        .ryhma(KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma("ryhma").withNimi(text("FI", "Kuvaus")))));
        Henkilo henkilo = populate(HenkiloPopulator.henkilo("1.2.3.4.5"));
        Henkilo henkilo2 = populate(IdentificationPopulator.identification(HAKA_AUTHENTICATION_IDP,
                "!haka%Identifier1/",
                HenkiloPopulator.henkilo("1.2.3.4.1")))
                .getHenkilo();
        doReturn("1.2.3.4.5").when(this.oppijanumerorekisteriClient).createHenkilo(any(HenkiloCreateDto.class));
        doReturn("1.2.3.4.5").when(this.oppijanumerorekisteriClient).getOidByHetu("hetu");
        HenkiloCreateByKutsuDto henkiloCreateByKutsuDto = new HenkiloCreateByKutsuDto("arpa",
                new KielisyysDto("fi", null), null, null);

        OrganisaatioPerustieto organisaatio = OrganisaatioPerustieto.builder().status(OrganisaatioStatus.AKTIIVINEN).build();
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(any())).willReturn(Optional.of(organisaatio));
        this.kutsuService.createHenkilo("123", henkiloCreateByKutsuDto);
        assertThat(henkilo.getOidHenkilo()).isEqualTo("1.2.3.4.5");
        assertThat(henkilo.getKayttajatiedot().getUsername()).matches("hakaIdentifier1[\\d]{3,3}");
        assertThat(henkilo.getKayttajatiedot().getPassword()).isNull();
        assertThat(organisaatioHenkiloRepository.findByHenkilo(henkilo))
                .flatExtracting(OrganisaatioHenkilo::getMyonnettyKayttoOikeusRyhmas)
                .extracting(MyonnettyKayttoOikeusRyhmaTapahtuma::getKayttoOikeusRyhma)
                .extracting(KayttoOikeusRyhma::getTunniste)
                .containsExactly("ryhma");
        assertThat(organisaatioHenkiloRepository.findByHenkilo(henkilo))
                .flatExtracting(OrganisaatioHenkilo::getMyonnettyKayttoOikeusRyhmas)
                .extracting(MyonnettyKayttoOikeusRyhmaTapahtuma::getKasittelija)
                .extracting(Henkilo::getOidHenkilo)
                .containsExactly("1.2.3.4.1");
        assertThat(organisaatioHenkiloRepository.findByHenkilo(henkilo))
                .flatExtracting(OrganisaatioHenkilo::getOrganisaatioOid)
                .containsExactly("1.2.0.0.1");

        assertThat(identificationRepository.findByHenkilo(henkilo))
                .filteredOn(identification -> identification.getIdpEntityId().equals(HAKA_AUTHENTICATION_IDP))
                .extracting(Identification::getIdentifier)
                .containsExactlyInAnyOrder("!haka%Identifier1/");
        assertThat(identificationRepository.findByHenkilo(henkilo2))
                .filteredOn(identification -> identification.getIdpEntityId().equals(HAKA_AUTHENTICATION_IDP))
                .isEmpty();

        assertThat(kutsu.getLuotuHenkiloOid()).isEqualTo(henkilo.getOidHenkilo());
        assertThat(kutsu.getTemporaryToken()).isNull();
        assertThat(kutsu.getTila()).isEqualTo(KutsunTila.KAYTETTY);
    }

    // Another haka identifier will be added to an existing user credentials
    @Test
    public void createHenkiloWithHakaIdentifierHetuExists() {
        given(this.oppijanumerorekisteriClient.getHenkilonPerustiedot(eq("1.2.3.4.1")))
                .willReturn(Optional.of(HenkiloPerustietoDto.builder().hetu("valid hetu").build()));
        Kutsu kutsu = populate(KutsuPopulator.kutsu("arpa", "kuutio", "arpa@kuutio.fi")
                .hakaIdentifier("!haka%Identifier1/")
                .temporaryToken("123")
                .hetu("hetu")
                .kutsuja("1.2.3.4.1")
                .organisaatio(KutsuOrganisaatioPopulator.kutsuOrganisaatio("1.2.0.0.1")
                        .ryhma(KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma("ryhma").withNimi(text("FI", "Kuvaus")))));
        Henkilo henkilo = populate(IdentificationPopulator.identification(HAKA_AUTHENTICATION_IDP,
                "old_identifier",
                HenkiloPopulator.henkilo("1.2.3.4.5")))
                .getHenkilo();
        populate(HenkiloPopulator.henkilo("1.2.3.4.1"));
        doReturn("1.2.3.4.5").when(this.oppijanumerorekisteriClient).createHenkilo(any(HenkiloCreateDto.class));
        doReturn("1.2.3.4.5").when(this.oppijanumerorekisteriClient).getOidByHetu("hetu");
        HenkiloCreateByKutsuDto henkiloCreateByKutsuDto = new HenkiloCreateByKutsuDto("arpa",
                new KielisyysDto("fi", null), null, null);

        OrganisaatioPerustieto organisaatio = OrganisaatioPerustieto.builder().status(OrganisaatioStatus.AKTIIVINEN).build();
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(any())).willReturn(Optional.of(organisaatio));
        this.kutsuService.createHenkilo("123", henkiloCreateByKutsuDto);
        assertThat(henkilo.getOidHenkilo()).isEqualTo("1.2.3.4.5");
        assertThat(henkilo.getKayttajatiedot().getUsername()).matches("hakaIdentifier1[\\d]{3,3}");
        assertThat(henkilo.getKayttajatiedot().getPassword()).isNull();
        assertThat(organisaatioHenkiloRepository.findByHenkilo(henkilo))
                .flatExtracting(OrganisaatioHenkilo::getMyonnettyKayttoOikeusRyhmas)
                .extracting(MyonnettyKayttoOikeusRyhmaTapahtuma::getKayttoOikeusRyhma)
                .extracting(KayttoOikeusRyhma::getTunniste)
                .containsExactly("ryhma");
        assertThat(organisaatioHenkiloRepository.findByHenkilo(henkilo))
                .flatExtracting(OrganisaatioHenkilo::getMyonnettyKayttoOikeusRyhmas)
                .extracting(MyonnettyKayttoOikeusRyhmaTapahtuma::getKasittelija)
                .extracting(Henkilo::getOidHenkilo)
                .containsExactly("1.2.3.4.1");
        assertThat(organisaatioHenkiloRepository.findByHenkilo(henkilo))
                .flatExtracting(OrganisaatioHenkilo::getOrganisaatioOid)
                .containsExactly("1.2.0.0.1");

        assertThat(identificationRepository.findByHenkilo(henkilo))
                .filteredOn(identification -> identification.getIdpEntityId().equals(HAKA_AUTHENTICATION_IDP))
                .extracting(Identification::getIdentifier)
                .containsExactlyInAnyOrder("!haka%Identifier1/", "old_identifier");

        assertThat(kutsu.getLuotuHenkiloOid()).isEqualTo(henkilo.getOidHenkilo());
        assertThat(kutsu.getTemporaryToken()).isNull();
        assertThat(kutsu.getTila()).isEqualTo(KutsunTila.KAYTETTY);
    }

    @Test
    public void discardInvitation() {
        Kutsu invitation = Mockito.mock(Kutsu.class);
        kutsuService.discardInvitation(invitation);
        verify(invitation, times(1)).poista("system");
    }
}
