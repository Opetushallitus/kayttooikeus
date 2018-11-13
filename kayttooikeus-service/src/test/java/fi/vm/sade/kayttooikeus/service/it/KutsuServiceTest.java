package fi.vm.sade.kayttooikeus.service.it;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fi.vm.sade.kayttooikeus.aspects.HenkiloHelper;
import fi.vm.sade.kayttooikeus.controller.KutsuPopulator;
import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.dto.enumeration.KutsuView;
import fi.vm.sade.kayttooikeus.enumeration.KutsuOrganisaatioOrder;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.IdentificationRepository;
import fi.vm.sade.kayttooikeus.repositories.OrganisaatioHenkiloRepository;
import fi.vm.sade.kayttooikeus.repositories.criteria.KutsuCriteria;
import fi.vm.sade.kayttooikeus.repositories.dto.HenkiloCreateByKutsuDto;
import fi.vm.sade.kayttooikeus.repositories.populate.*;
import fi.vm.sade.kayttooikeus.service.KutsuService;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService;
import fi.vm.sade.kayttooikeus.service.OrganisaatioService;
import fi.vm.sade.kayttooikeus.service.exception.ForbiddenException;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import fi.vm.sade.kayttooikeus.service.external.RyhmasahkopostiClient;
import fi.vm.sade.oppijanumerorekisteri.dto.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fi.vm.sade.kayttooikeus.controller.KutsuPopulator.kutsu;
import static fi.vm.sade.kayttooikeus.model.Identification.HAKA_AUTHENTICATION_IDP;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusPopulator.oikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaMyontoViitePopulator.kayttoOikeusRyhmaMyontoViite;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaPopulator.viite;
import static fi.vm.sade.kayttooikeus.repositories.populate.KutsuOrganisaatioPopulator.kutsuOrganisaatio;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloKayttoOikeusPopulator.myonnettyKayttoOikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloPopulator.organisaatioHenkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.TextGroupPopulator.text;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.PALVELU_HENKILONHALLINTA;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.ROLE_CRUD;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class KutsuServiceTest extends AbstractServiceIntegrationTest {
    @Autowired
    private KutsuService kutsuService;

    @Autowired
    private IdentificationRepository identificationRepository;

    @Autowired
    private OrganisaatioHenkiloRepository organisaatioHenkiloRepository;
    
    @MockBean
    private OrganisaatioClient organisaatioClient;

    @MockBean
    private RyhmasahkopostiClient ryhmasahkopostiClient;

    @MockBean
    private OppijanumerorekisteriClient oppijanumerorekisteriClient;

    @MockBean
    private HenkiloHelper henkiloHelper;

    @MockBean
    private LdapSynchronizationService ldapSynchronizationService;

    @MockBean
    private OrganisaatioService organisaatioService;

    @Before
    public void setup() {
        doNothing().when(this.oppijanumerorekisteriClient).updateHenkilo(any(HenkiloUpdateDto.class));
    }

    @Test
    @WithMockUser(username = "1.2.4", authorities = "ROLE_APP_HENKILONHALLINTA_CRUD")
    public void listAvoinKutsus() {
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.3", "1.2.3.4.5"),
                kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))));
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.4", "1.2.3.4.5"),
                kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))));
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.4", "1.2.3.4.6"),
                kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))));
        given(this.organisaatioClient.getActiveChildOids("1.2.3.4.5")).willReturn(Lists.newArrayList("1.2.3.4.5"));
        given(this.organisaatioClient.getActiveChildOids("1.2.3.4.6")).willReturn(Lists.newArrayList("1.2.3.4.6"));
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
                .extracting(KutsuReadDto.KutsuOrganisaatioDto::getOrganisaatioOid)
                .containsExactlyInAnyOrder("1.2.3.4.5", "1.2.3.4.6");
        assertThat(kutsus).extracting(KutsuReadDto::getEtunimi).containsExactlyInAnyOrder("Matti");
        assertThat(kutsus).extracting(KutsuReadDto::getSukunimi).containsExactlyInAnyOrder("Meikäläinen");
        assertThat(kutsus).flatExtracting(KutsuReadDto::getOrganisaatiot)
                .extracting(KutsuReadDto.KutsuOrganisaatioDto::getNimi)
                .extracting(TextGroupMapDto::getTexts)
                .extracting(map -> map.get("fi"))
                .containsExactlyInAnyOrder("Nimi1", "Nimi2");
    }

    @Test
    @WithMockUser(username = "1.2.4", authorities = {"ROLE_APP_HENKILONHALLINTA_CRUD", "ROLE_APP_HENKILONHALLINTA_CRUD_1.2.246.562.10.00000000001"})
    public void listAvoinKutsusWithMiniAdminAndOrganisationIsForcedWithOphView() {
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.3", "1.2.246.562.10.00000000001"),
                kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))));
        populate(kutsu("Essi", "Esimerkki", "a@eaxmple.com")
                .kutsuja("1.2.3").aikaleima(LocalDateTime.of(2016, 1, 1, 0, 0, 0, 0))
                .organisaatio(kutsuOrganisaatio("1.2.246.562.10.00000000001")
                        .ryhma(kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD)))));
        given(this.organisaatioClient.getActiveChildOids("1.2.246.562.10.00000000001")).willReturn(Lists.newArrayList("1.2.246.562.10.00000000001"));
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
                .flatExtracting(KutsuReadDto.KutsuOrganisaatioDto::getOrganisaatioOid)
                .containsExactly("1.2.246.562.10.00000000001");
    }

    @Test
    @WithMockUser(username = "1.2.4", authorities = {"ROLE_APP_HENKILONHALLINTA_CRUD", "ROLE_APP_HENKILONHALLINTA_CRUD_1.2.3.4.5"})
    public void listAvoinKutsusWithMiniAdminAndKayttooikeusryhmaView() {
        MyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma = populate(
                myonnettyKayttoOikeus(organisaatioHenkilo("1.2.3", "1.2.3.4.5"),
                kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))));
        populate(kutsu("Essi", "Esimerkki", "a@eaxmple.com")
                .kutsuja("1.2.3").aikaleima(LocalDateTime.of(2016, 1, 1, 0, 0, 0, 0))
                .organisaatio(kutsuOrganisaatio("1.2.3.4.5")
                        .ryhma(kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD)))));
        OrganisaatioPerustieto org1 = new OrganisaatioPerustieto();
        org1.setOid("1.2.3.4.5");
        org1.setNimi(new TextGroupMapDto().put("fi", "Nimi2").asMap());
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(eq("1.2.3.4.5")))
                .willReturn(Optional.of(org1));
        given(this.organisaatioClient.getActiveChildOids("1.2.3.4.5")).willReturn(Lists.newArrayList("1.2.3.4.5"));

        List<KutsuReadDto> kutsuList = this.kutsuService.listKutsus(KutsuOrganisaatioOrder.AIKALEIMA,
                Sort.Direction.ASC,
                KutsuCriteria.builder().kutsujaKayttooikeusryhmaIds(Sets.newHashSet(999L)).view(KutsuView.KAYTTOOIKEUSRYHMA).build(),
                null,
                null);
        assertThat(kutsuList)
                .flatExtracting(KutsuReadDto::getOrganisaatiot)
                .flatExtracting(KutsuReadDto.KutsuOrganisaatioDto::getKayttoOikeusRyhmat)
                .extracting(KutsuReadDto.KayttoOikeusRyhmaDto::getId)
                .containsExactly(myonnettyKayttoOikeusRyhmaTapahtuma.getKayttoOikeusRyhma().getId());
    }

    @Test
    @WithMockUser(username = "1.2.4", authorities = {"ROLE_APP_HENKILONHALLINTA_CRUD", "ROLE_APP_HENKILONHALLINTA_CRUD_1.2.3.4.5"})
    public void listAvoinKutsusWithNormalUserAndOrganisationIsForced() {
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.3", "1.2.3.4.5"),
                kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))));
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.4", "1.2.3.4.5"),
                kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))));
        populate(kutsu("Essi", "Esimerkki", "a@eaxmple.com")
                .kutsuja("1.2.3").aikaleima(LocalDateTime.of(2016, 1, 1, 0, 0, 0, 0))
                .organisaatio(kutsuOrganisaatio("1.2.3.4.5")
                        .ryhma(kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD)))));
        populate(kutsu("Essi", "Esimerkki", "a@eaxmple.com")
                .kutsuja("1.2.3").aikaleima(LocalDateTime.of(2016, 1, 1, 0, 0, 0, 0))
                .organisaatio(kutsuOrganisaatio("1.2.246.562.10.00000000001")
                        .ryhma(kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD)))));
        given(this.organisaatioClient.getActiveChildOids("1.2.246.562.10.00000000001")).willReturn(Lists.newArrayList("1.2.246.562.10.00000000001"));
        given(this.organisaatioClient.getActiveChildOids("1.2.3.4.5")).willReturn(Lists.newArrayList("1.2.3.4.5"));
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
                .flatExtracting(KutsuReadDto.KutsuOrganisaatioDto::getOrganisaatioOid)
                .containsExactly("1.2.3.4.5");
    }

    @Test
    @WithMockUser(username = "1.2.4", authorities = {"ROLE_APP_HENKILONHALLINTA_CRUD", "ROLE_APP_HENKILONHALLINTA_CRUD_1.2.3.4.5"})
    public void listAvoinKutsusWithNormalUserByKayttooikeusryhmaId() {
        MyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma
                = populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.4", "1.2.3.4.5"),
                kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))));
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("kutsujaOid", "1.2.3.4.5"),
                kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))));
        populate(kutsu("Essi", "Esimerkki", "a@eaxmple.com")
                .kutsuja("kutsujaOid").aikaleima(LocalDateTime.of(2016, 1, 1, 0, 0, 0, 0))
                .organisaatio(kutsuOrganisaatio("1.2.3.4.5")
                        .ryhma(kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD)))));
        populate(kutsu("Essi", "Esimerkki", "a@eaxmple.com")
                .kutsuja("kutsujaOid").aikaleima(LocalDateTime.of(2016, 1, 1, 0, 0, 0, 0))
                .organisaatio(kutsuOrganisaatio("1.2.246.562.10.00000000001")
                        .ryhma(kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD)))));
        given(this.organisaatioClient.getActiveChildOids("1.2.3.4.5")).willReturn(Lists.newArrayList("1.2.3.4.5"));
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
                .flatExtracting(KutsuReadDto.KutsuOrganisaatioDto::getOrganisaatioOid)
                .containsExactly("1.2.3.4.5");
        assertThat(kutsuList)
                .flatExtracting(KutsuReadDto::getOrganisaatiot)
                .flatExtracting(KutsuReadDto.KutsuOrganisaatioDto::getKayttoOikeusRyhmat)
                .extracting(KutsuReadDto.KayttoOikeusRyhmaDto::getId)
                .containsExactly(myonnettyKayttoOikeusRyhmaTapahtuma.getKayttoOikeusRyhma().getId());
    }

    @Test
    @WithMockUser(username = "1.2.4", authorities = {"ROLE_APP_HENKILONHALLINTA_OPHREKISTERI", "ROLE_APP_HENKILONHALLINTA_OPHREKISTERI_1.2.246.562.10.00000000001"})
    public void createKutsuAsAdmin() {
        HttpEntity emailResponseEntity = new StringEntity("12345", Charset.forName("UTF-8"));
        BasicHttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "Ok"));
        response.setEntity(emailResponseEntity);
        given(ryhmasahkopostiClient.sendRyhmasahkoposti(any())).willReturn(response);
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
                organisaatioHenkilo("1.2.4", "1.2.246.562.10.00000000001"),
                kayttoOikeusRyhma("kayttoOikeusRyhma")
                        .withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))
                        .withNimi(text("fi", "Käyttöoikeusryhmä"))));
        Long kayttoOikeusRyhmaId = tapahtuma.getKayttoOikeusRyhma().getId();
        KutsuCreateDto.KayttoOikeusRyhmaDto kutsuKayttoOikeusRyhma = new KutsuCreateDto.KayttoOikeusRyhmaDto();
        kutsuKayttoOikeusRyhma.setId(kayttoOikeusRyhmaId);

        KutsuCreateDto kutsu = new KutsuCreateDto();
        kutsu.setEtunimi("Etu");
        kutsu.setSukunimi("Suku");
        kutsu.setSahkoposti("example@example.com");
        kutsu.setAsiointikieli(Asiointikieli.fi);
        kutsu.setOrganisaatiot(new LinkedHashSet<>());
        KutsuCreateDto.KutsuOrganisaatioDto kutsuOrganisaatio = new KutsuCreateDto.KutsuOrganisaatioDto();
        kutsuOrganisaatio.setOrganisaatioOid("1.2.246.562.10.00000000001");
        kutsuOrganisaatio.setKayttoOikeusRyhmat(Stream.of(kutsuKayttoOikeusRyhma).collect(toSet()));
        kutsu.getOrganisaatiot().add(kutsuOrganisaatio);

        long id = kutsuService.createKutsu(kutsu);
        KutsuReadDto tallennettu = kutsuService.getKutsu(id);

        assertThat(tallennettu.getAsiointikieli()).isEqualByComparingTo(Asiointikieli.fi);
        assertThat(tallennettu.getOrganisaatiot())
                .hasSize(1)
                .flatExtracting(KutsuReadDto.KutsuOrganisaatioDto::getKayttoOikeusRyhmat)
                .hasSize(1)
                .extracting(KutsuReadDto.KayttoOikeusRyhmaDto::getNimi)
                .flatExtracting(TextGroupMapDto::getTexts)
                .extracting("fi")
                .containsExactly("Käyttöoikeusryhmä");

        Kutsu entity = em.find(Kutsu.class, id);
        assertThat(entity.getSalaisuus()).isNotEmpty();
    }

    @Test
    @WithMockUser(username = "1.2.4", authorities = {"ROLE_APP_HENKILONHALLINTA_CRUD", "ROLE_APP_HENKILONHALLINTA_CRUD_1.2.3.4.5"})
    public void createKutsuAsNormalUser() {
        HttpEntity emailResponseEntity = new StringEntity("12345", Charset.forName("UTF-8"));
        BasicHttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "Ok"));
        response.setEntity(emailResponseEntity);
        given(this.ryhmasahkopostiClient.sendRyhmasahkoposti(any())).willReturn(response);
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
        org1.setOid("1.2.3.4.5");
        org1.setNimi(new TextGroupMapDto().put("FI", "Käyttäjän organisaatio").asMap());
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(eq("1.2.3.4.5")))
                .willReturn(Optional.of(org2));
        given(this.organisaatioClient.listWithParentsAndChildren(eq("1.2.3.4.5"), any()))
                .willReturn(asList(org2));

        MyonnettyKayttoOikeusRyhmaTapahtuma tapahtuma = populate(myonnettyKayttoOikeus(
                organisaatioHenkilo("1.2.4", "1.2.3.4.5"),
                kayttoOikeusRyhma("kayttoOikeusRyhma")
                        .withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))));
        OrganisaatioViite organisaatioViite = populate(viite(kayttoOikeusRyhma("RYHMA2")
                        .withNimi(text("fi", "Käyttöoikeusryhmä")),
                "1.2.3.4.5"));
        populate(kayttoOikeusRyhmaMyontoViite(tapahtuma.getKayttoOikeusRyhma().getId(),
                organisaatioViite.getKayttoOikeusRyhma().getId()));

        KutsuCreateDto.KayttoOikeusRyhmaDto kutsuKayttoOikeusRyhma = new KutsuCreateDto.KayttoOikeusRyhmaDto();
        kutsuKayttoOikeusRyhma.setId(organisaatioViite.getKayttoOikeusRyhma().getId());
        given(this.organisaatioClient.listWithChildOids(eq("1.2.3.4.5"), any()))
                .willReturn(Stream.of("1.2.3.4.5", "1.2.3.4.1").collect(toSet()));

        KutsuCreateDto kutsu = new KutsuCreateDto();
        kutsu.setEtunimi("Etu");
        kutsu.setSukunimi("Suku");
        kutsu.setSahkoposti("example@example.com");
        kutsu.setAsiointikieli(Asiointikieli.fi);
        kutsu.setOrganisaatiot(new LinkedHashSet<>());
        KutsuCreateDto.KutsuOrganisaatioDto kutsuOrganisaatio = new KutsuCreateDto.KutsuOrganisaatioDto();
        kutsuOrganisaatio.setOrganisaatioOid("1.2.3.4.1");
        kutsuOrganisaatio.setKayttoOikeusRyhmat(Stream.of(kutsuKayttoOikeusRyhma).collect(toSet()));
        kutsu.getOrganisaatiot().add(kutsuOrganisaatio);

        long id = kutsuService.createKutsu(kutsu);
        KutsuReadDto tallennettu = kutsuService.getKutsu(id);

        assertThat(tallennettu.getAsiointikieli()).isEqualByComparingTo(Asiointikieli.fi);
        assertThat(tallennettu.getOrganisaatiot())
                .hasSize(1)
                .flatExtracting(KutsuReadDto.KutsuOrganisaatioDto::getKayttoOikeusRyhmat)
                .hasSize(1)
                .extracting(KutsuReadDto.KayttoOikeusRyhmaDto::getNimi)
                .flatExtracting(TextGroupMapDto::getTexts)
                .extracting("fi")
                .containsExactly("Käyttöoikeusryhmä");

        Kutsu entity = this.em.find(Kutsu.class, id);
        assertThat(entity.getSalaisuus()).isNotEmpty();
    }

    @Test(expected = ForbiddenException.class)
    @WithMockUser(username = "1.2.4", authorities = {"ROLE_APP_HENKILONHALLINTA_OPHREKISTERI", "ROLE_APP_HENKILONHALLINTA_OPHREKISTERI_1.2.246.562.10.00000000001"})
    public void createKutsuAsAdminWithNoHetuOrVtjYksiloity() {
        doReturn(HenkiloDto.builder()
                .kutsumanimi("kutsun")
                .sukunimi("kutsuja")
                .build())
                .when(this.oppijanumerorekisteriClient).getHenkiloByOid(anyString());
        this.kutsuService.createKutsu(new KutsuCreateDto());
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
        YhteystiedotRyhmaDto existingYhteystiedotRyhmaDto = new YhteystiedotRyhmaDto(null, YhteystietojenTyypit.TYOOSOITE, "alkupera6", true, existingYhteystietoDtos);
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
        YhteystiedotRyhmaDto existingYhteystiedotRyhmaDto = new YhteystiedotRyhmaDto(null, YhteystietojenTyypit.TYOOSOITE, "alkupera6", true, existingYhteystietoDtos);
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
    @WithMockUser(username = "1.2.4", authorities = {"ROLE_APP_HENKILONHALLINTA_CRUD", "ROLE_APP_HENKILONHALLINTA_CRUD_1.2.3.4.5"})
    public void deleteKutsuTest() {
        Kutsu kutsu = populate(kutsu("Matti", "Mehiläinen", "b@eaxmple.com")
                .kutsuja("1.2.4")
                .organisaatio(kutsuOrganisaatio("1.2.3.4.5")
                        .ryhma(kayttoOikeusRyhma("RYHMA2")))
        );
        populate(organisaatioHenkilo("1.2.4", "1.2.3.4.5"));
        given(this.organisaatioClient.getActiveChildOids("1.2.3.4.5")).willReturn(Lists.newArrayList("1.2.3.4.5"));
        this.kutsuService.deleteKutsu(kutsu.getId());
        this.em.flush();
        assertEquals(KutsunTila.POISTETTU, kutsu.getTila());
        assertEquals("1.2.4", kutsu.getPoistaja());
        assertNotNull(kutsu.getPoistettu());
    }
    
    @Test(expected = ForbiddenException.class)
    @WithMockUser(username = "1.2.4", authorities = "ROLE_APP_HENKILONHALLINTA_CRUD")
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
        doReturn(Optional.of("1.2.3.4.5")).when(this.oppijanumerorekisteriClient).createHenkiloForKutsu(any(HenkiloCreateDto.class));
        doReturn(Optional.ofNullable(null)).when(this.oppijanumerorekisteriClient).getHenkiloByHetu(any());
        doReturn("1.2.3.4.5").when(this.oppijanumerorekisteriClient).getOidByHetu("hetu");
        HenkiloCreateByKutsuDto henkiloCreateByKutsuDto = new HenkiloCreateByKutsuDto("arpa",
                new KielisyysDto("fi", null), "arpauser", "stronkPassword1!");

        given(this.organisaatioClient.existsByOidAndStatus(any(), any())).willReturn(true);
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
        doReturn(Optional.of("1.2.3.4.5")).when(this.oppijanumerorekisteriClient).createHenkiloForKutsu(any(HenkiloCreateDto.class));
        doReturn("1.2.3.4.5").when(this.oppijanumerorekisteriClient).getOidByHetu("valid hetu");
        HenkiloCreateByKutsuDto henkiloCreateByKutsuDto = new HenkiloCreateByKutsuDto("arpa",
                new KielisyysDto("fi", null), "arpauser", "stronkPassword1!");
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
                new KielisyysDto("fi", null), "arpauser", "stronkPassword1!");
        given(this.oppijanumerorekisteriClient.getHenkiloByOid(any())).willReturn(new HenkiloDto());
        given(this.organisaatioClient.existsByOidAndStatus(any(), any())).willReturn(true);

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
        doReturn(Optional.of("1.2.3.4.5")).when(this.oppijanumerorekisteriClient).createHenkiloForKutsu(any(HenkiloCreateDto.class));
        doReturn("1.2.3.4.5").when(this.oppijanumerorekisteriClient).getOidByHetu("hetu");
        HenkiloCreateByKutsuDto henkiloCreateByKutsuDto = new HenkiloCreateByKutsuDto("arpa",
                new KielisyysDto("fi", null), null, null);

        given(this.organisaatioClient.existsByOidAndStatus(any(), any())).willReturn(true);
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
        doReturn(Optional.of("1.2.3.4.5")).when(this.oppijanumerorekisteriClient).createHenkiloForKutsu(any(HenkiloCreateDto.class));
        doReturn("1.2.3.4.5").when(this.oppijanumerorekisteriClient).getOidByHetu("hetu");
        HenkiloCreateByKutsuDto henkiloCreateByKutsuDto = new HenkiloCreateByKutsuDto("arpa",
                new KielisyysDto("fi", null), null, null);

        given(this.organisaatioClient.existsByOidAndStatus(any(), any())).willReturn(true);
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
        doReturn(Optional.of("1.2.3.4.5")).when(this.oppijanumerorekisteriClient).createHenkiloForKutsu(any(HenkiloCreateDto.class));
        doReturn("1.2.3.4.5").when(this.oppijanumerorekisteriClient).getOidByHetu("hetu");
        HenkiloCreateByKutsuDto henkiloCreateByKutsuDto = new HenkiloCreateByKutsuDto("arpa",
                new KielisyysDto("fi", null), null, null);

        given(this.organisaatioClient.existsByOidAndStatus(any(), any())).willReturn(true);
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
}
