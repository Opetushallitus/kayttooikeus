package fi.vm.sade.kayttooikeus.service.it;

import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.dto.ExpiringKayttoOikeusDto;
import fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaPopulator;
import fi.vm.sade.kayttooikeus.service.KayttoOikeusService;
import fi.vm.sade.kayttooikeus.service.TimeService;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import org.assertj.core.groups.Tuple;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.henkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusPopulator.oikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaMyontoViitePopulator.kayttoOikeusRyhmaMyontoViite;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloKayttoOikeusPopulator.myonnettyKayttoOikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloPopulator.organisaatioHenkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.PalveluPopulator.palvelu;
import static fi.vm.sade.kayttooikeus.repositories.populate.TextGroupPopulator.text;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

@RunWith(SpringRunner.class)
@Sql("/truncate_tables.sql")
public class KayttoOikeusServiceTest extends AbstractServiceIntegrationTest {
    @Autowired
    private KayttoOikeusService kayttoOikeusService;

    @MockBean
    private OrganisaatioClient organisaatioClient;

    @SpyBean
    private TimeService timeService;

    @SpyBean
    private CommonProperties commonProperties;

    @Test
    public void listAllKayttoOikeusRyhmasTest() {
        populate(kayttoOikeusRyhma("RYHMA1").withNimi(text("FI", "Käyttäjähallinta")
                                .put("EN", "User management"))
                .withOrganisaatiorajoite("TYYPPI")
                .withOikeus(oikeus("HENKILOHALLINTA", "CRUD"))
                .withOikeus(oikeus("KOODISTO", "READ")));
        populate(kayttoOikeusRyhma("RYHMA2").withNimi(text("FI", "Koodistonhallinta")
                        .put("EN", "Code management"))
                .withOrganisaatiorajoite("TYYPPI")
                .withOikeus(oikeus("KOODISTO", "CRUD")));
        
        List<KayttoOikeusRyhmaDto> ryhmas = kayttoOikeusService.listAllKayttoOikeusRyhmas(false);
        assertEquals(2, ryhmas.size());

    }

    @Test
    public void listKayttoOikeusByPalveluTest() {
        populate(oikeus("HENKILOHALLINTA", "CRUD").kuvaus(text("FI", "Käsittelyoikeus")
                                                        .put("EN", "Admin")));
        populate(oikeus("HENKILOHALLINTA", "READ").kuvaus(text("FI", "Lukuoikeus")));
        populate(oikeus("KOODISTO", "CRUD"));

        List<PalveluKayttoOikeusDto> results = kayttoOikeusService.listKayttoOikeusByPalvelu("HENKILOHALLINTA");
        assertEquals(2, results.size());
        assertEquals("CRUD", results.get(0).getRooli());
        assertEquals("Käsittelyoikeus", results.get(0).getOikeusLangs().get("FI"));
        assertEquals("Admin", results.get(0).getOikeusLangs().get("EN"));
        assertEquals("Lukuoikeus", results.get(1).getOikeusLangs().get("FI"));
    }
    
    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void listMyonnettyKayttoOikeusHistoriaForCurrentUser() {
        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA")
                        .withOikeus(oikeus("HENKILOHALLINTA", "CRUD"))
                        .withOikeus(oikeus(palvelu("KOODISTO").kuvaus(text("FI", "Palvelukuvaus")), "READ"))
        ));
        MyonnettyKayttoOikeusRyhmaTapahtuma tapahtuma = populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "4.5.6.7.8"),
                kayttoOikeusRyhma("RYHMA2").withNimi(text("FI", "Koodistonhallinta"))
                        .withOikeus(oikeus("KOODISTO", "CRUD")
                                .kuvaus(text("FI", "Kirjoitusoikeus")))
        ));
        
        List<KayttoOikeusHistoriaDto> list = kayttoOikeusService.listMyonnettyKayttoOikeusHistoriaForCurrentUser();
        assertEquals(3, list.size());
        KayttoOikeusHistoriaDto kayttoOikeusHistoriaResult = list.stream().filter(kayttoOikeusHistoriaDto -> kayttoOikeusHistoriaDto.getOrganisaatioOid().equals(tapahtuma.getOrganisaatioHenkilo().getOrganisaatioOid())).findFirst().orElseThrow(RuntimeException::new);
        assertEquals(tapahtuma.getAikaleima(), kayttoOikeusHistoriaResult.getAikaleima());
        assertEquals(tapahtuma.getKasittelija().getOidHenkilo(), kayttoOikeusHistoriaResult.getKasittelija());
        assertEquals(tapahtuma.getOrganisaatioHenkilo().getOrganisaatioOid(), kayttoOikeusHistoriaResult.getOrganisaatioOid());
        assertEquals(tapahtuma.getOrganisaatioHenkilo().getTehtavanimike(), kayttoOikeusHistoriaResult.getTehtavanimike());
        assertEquals(KayttoOikeudenTila.MYONNETTY, kayttoOikeusHistoriaResult.getTila());
        assertEquals(KayttoOikeusTyyppi.KOOSTEROOLI, kayttoOikeusHistoriaResult.getTyyppi());
        assertEquals(tapahtuma.getVoimassaAlkuPvm(), kayttoOikeusHistoriaResult.getVoimassaAlkuPvm());
        assertEquals(tapahtuma.getVoimassaLoppuPvm(), kayttoOikeusHistoriaResult.getVoimassaLoppuPvm());
        assertEquals(tapahtuma.getKayttoOikeusRyhma().getKayttoOikeus().iterator().next().getId().longValue(),
                kayttoOikeusHistoriaResult.getKayttoOikeusId());
        assertEquals("CRUD", kayttoOikeusHistoriaResult.getRooli());
        assertEquals("KOODISTO", kayttoOikeusHistoriaResult.getPalvelu());
        assertEquals("Koodistonhallinta", kayttoOikeusHistoriaResult.getKuvaus().get("FI"));
        assertEquals("Kirjoitusoikeus", kayttoOikeusHistoriaResult.getKayttoOikeusKuvaus().get("FI"));
        assertEquals("Palvelukuvaus", kayttoOikeusHistoriaResult.getPalveluKuvaus().get("FI"));
    }

    @Test
    public void findToBeExpiringMyonnettyKayttoOikeusTest() {
        MyonnettyKayttoOikeusRyhmaTapahtuma tapahtuma = populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA").withNimi(text("FI", "kuvaus"))
                        .withOikeus(oikeus("HENKILOHALLINTA", "CRUD"))
                        .withOikeus(oikeus("KOODISTO", "READ"))
            ).voimassaPaattyen(LocalDate.now().plusMonths(1)));
        List<ExpiringKayttoOikeusDto> oikeus = kayttoOikeusService.findToBeExpiringMyonnettyKayttoOikeus(LocalDate.now(), Period.ofMonths(1));
        assertEquals(1, oikeus.size());
        assertEquals(tapahtuma.getId(), oikeus.get(0).getMyonnettyTapahtumaId());
        assertEquals("1.2.3.4.5", oikeus.get(0).getHenkiloOid());
        assertEquals(LocalDate.now().plusMonths(1), oikeus.get(0).getVoimassaLoppuPvm());
        assertEquals("kuvaus", oikeus.get(0).getRyhmaDescription().get("FI"));
        assertEquals("RYHMA", oikeus.get(0).getRyhmaName());
    }

    @Test
    @WithMockUser(username = "1.2.3.4.6")
    public void listPossibleRyhmasByOrganizationTest(){
        OrganisaatioPerustieto oppilaitos11 = oppilaitos("1.2.246.562.10.12345678911", "oppilaitostyyppi_11");
        OrganisaatioPerustieto oppilaitos12 = oppilaitos("1.2.246.562.10.12345678912", "oppilaitostyyppi_12");
        OrganisaatioPerustieto koulutustoimija1 = new OrganisaatioPerustieto();
        koulutustoimija1.setOid("1.2.246.562.10.12345678910");
        koulutustoimija1.setChildren(asList(oppilaitos11, oppilaitos12));
        given(this.organisaatioClient.listWithParentsAndChildren(argThat(isOneOf(
                "1.2.246.562.10.12345678910", "1.2.246.562.10.12345678911", "1.2.246.562.10.12345678912")), any()))
                .willReturn(asList(koulutustoimija1, oppilaitos11, oppilaitos12));
        given(this.organisaatioClient.listWithParentsAndChildren(argThat(isOneOf(
                "1.2.246.562.10.12345678911")), any()))
                .willReturn(asList(koulutustoimija1, oppilaitos11));
        given(this.organisaatioClient.listWithParentsAndChildren(argThat(isOneOf(
                "1.2.246.562.10.12345678912")), any()))
                .willReturn(asList(koulutustoimija1, oppilaitos12));

        OrganisaatioPerustieto toimipiste211 = OrganisaatioPerustieto.builder()
                .oid("1.2.246.562.10.123456789211")
                .organisaatiotyypit(singletonList("organisaatiotyyppi_03"))
                .build();
        OrganisaatioPerustieto oppilaitos21 = OrganisaatioPerustieto.builder()
                .oid("1.2.246.562.10.12345678921")
                .organisaatiotyypit(singletonList("organisaatiotyyppi_02"))
                .children(singletonList(toimipiste211))
                .build();
        OrganisaatioPerustieto koulutustoimija2 = OrganisaatioPerustieto.builder()
                .oid("1.2.246.562.10.12345678920")
                .organisaatiotyypit(singletonList("organisaatiotyyppi_01"))
                .children(singletonList(oppilaitos21))
                .build();
        given(this.organisaatioClient.listWithParentsAndChildren(argThat(isOneOf(
                "1.2.246.562.10.12345678920", "1.2.246.562.10.12345678921", "1.2.246.562.10.123456789211")), any()))
                .willReturn(asList(koulutustoimija2, oppilaitos21, toimipiste211));

        populate(kayttoOikeusRyhma("RYHMA-ORGANISAATIOLLE").withOrganisaatiorajoite("1.2.246.562.10.12345678901"));
        populate(kayttoOikeusRyhma("RYHMA-OPPILAITOKSEN_PERUSTEELLA").withOrganisaatiorajoite("oppilaitostyyppi_12"));
        populate(kayttoOikeusRyhma("RYHMA-ORGANISAATIONTYYPIN_PERUSTEELLA").withOrganisaatiorajoite("organisaatiotyyppi_02"));
        populate(kayttoOikeusRyhma("RYHMA-ORGANISAATIORYHMILLE1").withOrganisaatiorajoite("1.2.246.562.28"));
        populate(kayttoOikeusRyhma("RYHMA-ORGANISAATIORYHMILLE2").asRyhmaRestriction());
        populate(kayttoOikeusRyhma("RYHMA-VAIN_OPH"));

        List<KayttoOikeusRyhmaDto> ryhmat;
        ryhmat = kayttoOikeusService.listPossibleRyhmasByOrganization("1.2.246.562.10.12345678901");
        assertThat(ryhmat).extracting(KayttoOikeusRyhmaDto::getTunniste)
                .containsExactlyInAnyOrder("RYHMA-ORGANISAATIOLLE");

        ryhmat = kayttoOikeusService.listPossibleRyhmasByOrganization("1.2.246.562.10.12345678902");
        assertThat(ryhmat).isEmpty();

        ryhmat = kayttoOikeusService.listPossibleRyhmasByOrganization("1.2.246.562.10.12345678910");
        assertThat(ryhmat).extracting(KayttoOikeusRyhmaDto::getTunniste)
                .containsExactlyInAnyOrder("RYHMA-OPPILAITOKSEN_PERUSTEELLA");
        ryhmat = kayttoOikeusService.listPossibleRyhmasByOrganization("1.2.246.562.10.12345678911");
        assertThat(ryhmat).extracting(KayttoOikeusRyhmaDto::getTunniste).isEmpty();

        ryhmat = kayttoOikeusService.listPossibleRyhmasByOrganization("1.2.246.562.10.12345678921");
        assertThat(ryhmat).extracting(KayttoOikeusRyhmaDto::getTunniste)
                .containsExactlyInAnyOrder("RYHMA-ORGANISAATIONTYYPIN_PERUSTEELLA");
        ryhmat = kayttoOikeusService.listPossibleRyhmasByOrganization("1.2.246.562.10.12345678920");
        assertThat(ryhmat).extracting(KayttoOikeusRyhmaDto::getTunniste).isEmpty();
        ryhmat = kayttoOikeusService.listPossibleRyhmasByOrganization("1.2.246.562.10.123456789211");
        assertThat(ryhmat).extracting(KayttoOikeusRyhmaDto::getTunniste).isEmpty();

        ryhmat = kayttoOikeusService.listPossibleRyhmasByOrganization("1.2.246.562.28.12345678901");
        assertThat(ryhmat).extracting(KayttoOikeusRyhmaDto::getTunniste)
                .containsExactlyInAnyOrder("RYHMA-ORGANISAATIORYHMILLE1", "RYHMA-ORGANISAATIORYHMILLE2");

        ryhmat = kayttoOikeusService.listPossibleRyhmasByOrganization("1.2.246.562.10.00000000001");
        assertThat(ryhmat).hasSize(kayttoOikeusService.listAllKayttoOikeusRyhmas(false).size());
    }

    private static OrganisaatioPerustieto oppilaitos(String oid, String oppilaitostyyppi) {
        OrganisaatioPerustieto organisaatio = new OrganisaatioPerustieto();
        organisaatio.setOid(oid);
        organisaatio.setOppilaitostyyppi(String.format("%s#1", oppilaitostyyppi));
        return organisaatio;
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void findKayttoOikeusRyhmaTest(){
        populate(kayttoOikeusRyhma("RYHMA1").withNimi(text("FI", "Käyttäjähallinta")
                .put("EN", "User management"))
                .withOrganisaatiorajoite("TYYPPI")
                .withOikeus(oikeus("HENKILOHALLINTA", "CRUD"))
                .withOikeus(oikeus("KOODISTO", "READ")));
        Long id = populate(kayttoOikeusRyhma("RYHMA2").withNimi(text("FI", "Koodistonhallinta")
                .put("EN", "Code management"))
                .withOrganisaatiorajoite("TYYPPI")
                .withOikeus(oikeus("KOODISTO", "CRUD"))
                .withSallittu(KayttajaTyyppi.PALVELU)).getId();

        KayttoOikeusRyhmaDto ryhma = kayttoOikeusService.findKayttoOikeusRyhma(id, false);
        assertNotNull(ryhma);
        assertEquals("RYHMA2", ryhma.getName());
        assertEquals(KayttajaTyyppi.PALVELU, ryhma.getSallittuKayttajatyyppi());
    }


    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void findPalveluRoolisByKayttoOikeusRyhmaTest(){
        populate(palvelu("HENKILOPALVELU").kuvaus(text("FI", "Henkilöpalvelu").put("EN", "Person service")));
        Palvelu palvelu = populate(palvelu("KOODISTO").kuvaus(text("FI", "palvelun kuvaus")
                        .put("EN", "kuv en")
                        .put("SV", "kuvaus på sv")));
        palvelu.getKayttoOikeus().add(populate(oikeus("HENKILOHALLINTA", "CRUD")));

        populate(kayttoOikeusRyhma("RYHMA1").withNimi(text("FI", "Käyttäjähallinta")
                .put("EN", "User management"))
                .withOrganisaatiorajoite("TYYPPI")
                .withOikeus(oikeus("HENKILOHALLINTA", "CRUD"))
                .withOikeus(oikeus("KOODISTO", "READ")));
        Long id = populate(kayttoOikeusRyhma("RYHMA2").withNimi(text("FI", "Koodistonhallinta")
                .put("EN", "Code management"))
                .withOrganisaatiorajoite("TYYPPI")
                .withOikeus(oikeus("KOODISTO", "CRUD"))).getId();

        List<PalveluRooliDto> roolis = kayttoOikeusService.findPalveluRoolisByKayttoOikeusRyhma(id);
        assertEquals(1L, roolis.size());
        assertEquals("KOODISTO", roolis.get(0).getPalveluName());
        assertEquals("CRUD", roolis.get(0).getRooli());
        assertEquals(3, roolis.get(0).getPalveluTexts().getTexts().size());
        assertEquals("palvelun kuvaus", roolis.get(0).getPalveluTexts().get("FI"));
        assertEquals("kuv en", roolis.get(0).getPalveluTexts().get("EN"));
        assertEquals("kuvaus på sv", roolis.get(0).getPalveluTexts().get("SV"));
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void findHenkilotByKayttoOikeusRyhma(){

        KayttoOikeusRyhmaPopulator pop = kayttoOikeusRyhma("RYHMA1").withNimi(text("FI", "Koodistonhallinta")
                .put("EN", "Code management"))
                .withOikeus(oikeus("KOODISTO", "CRUD"));

        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"), pop).voimassaPaattyen(LocalDate.now().plusMonths(3)).voimassaAlkaen(LocalDate.now().minusMonths(1)));

        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("5.6.7.8.9"), "3.4.5.6.7"), pop).voimassaPaattyen(LocalDate.now().plusMonths(2)).voimassaAlkaen(LocalDate.now().minusMonths(1)));

        KayttoOikeusRyhmaPopulator pop2 = kayttoOikeusRyhma("RYHMA2").withNimi(text("FI", "testiryhma")
                .put("EN", "testgroup"))
                .withOikeus(oikeus("HAKUAPP", "CRUD"));

        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"), pop2).voimassaPaattyen(LocalDate.now().minusMonths(1)).voimassaAlkaen(LocalDate.now().minusMonths(2)));

        RyhmanHenkilotDto henkilot = kayttoOikeusService.findHenkilotByKayttoOikeusRyhma(populate(pop).getId());
        assertEquals(2, henkilot.getPersonOids().size());

        RyhmanHenkilotDto henkilot2 = kayttoOikeusService.findHenkilotByKayttoOikeusRyhma(populate(pop2).getId());
        assertEquals(0, henkilot2.getPersonOids().size());

    }

    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void findSubRyhmasByMasterRyhmaTest(){
        Long masterId = populate(kayttoOikeusRyhma("MASTER").withNimi(text("FI", "Master").put("EN", "Master"))).getId();
        Long id = populate(kayttoOikeusRyhma("RYHMA").withNimi(text("FI", "Käyttäjähallinta")
                .put("EN", "User management"))
                .withOikeus(oikeus("HENKILOHALLINTA", "CRUD"))
                .withOikeus(oikeus("KOODISTO", "READ"))).getId();
        populate(kayttoOikeusRyhmaMyontoViite(masterId, id));

        List<KayttoOikeusRyhmaDto> ryhmas = kayttoOikeusService.findSubRyhmasByMasterRyhma(masterId);
        assertEquals(1, ryhmas.size());
        assertEquals("RYHMA", ryhmas.get(0).getName());

        ryhmas = kayttoOikeusService.findSubRyhmasByMasterRyhma(111L);
        assertEquals(0, ryhmas.size());
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void createKayttoOikeusRyhmaAndUpdateTest() {
        KayttoOikeus oikeus = populate(oikeus("HENKILOHALLINTA", "CRUD"));
        Palvelu palvelu = populate(palvelu("JOKUPALVELU").kuvaus(text("FI", "joku palvelu")
                .put("EN", "Person service")));
        palvelu.getKayttoOikeus().add(oikeus);

        KayttoOikeusRyhmaModifyDto ryhma = KayttoOikeusRyhmaModifyDto.builder()
                .nimi(new TextGroupDto()
                        .put("FI", "ryhmäname")
                        .put("SV", "ryhmäname sv")
                        .put("EN", "ryhmäname en"))
                .kuvaus(new TextGroupDto()
                        .put("FI", "ryhmäkuvaus")
                        .put("SV", "ryhmäkuvaus sv")
                        .put("EN", "ryhmäkuvaus en"))
                .rooliRajoite("roolirajoite")
                .palvelutRoolit(singletonList(PalveluRooliModifyDto.builder()
                        .rooli("CRUD")
                        .palveluName("HENKILOHALLINTA")
                        .build()))
                .organisaatioTyypit(singletonList("org tyyppi"))
                .ryhmaRestriction(false)
                .sallittuKayttajatyyppi(KayttajaTyyppi.PALVELU)
                .build();

        OffsetDateTime now1 = OffsetDateTime.of(2019, 10, 29, 13, 54, 45, 0, ZoneOffset.UTC);
        when(timeService.getOffsetDateTimeNow()).thenReturn(now1);

        long createdRyhmaId = kayttoOikeusService.createKayttoOikeusRyhma(ryhma);
        KayttoOikeusRyhmaDto createdRyhma = kayttoOikeusService.findKayttoOikeusRyhma(createdRyhmaId, false);

        assertNotNull(createdRyhma);
        assertTrue(createdRyhma.getName().startsWith("ryhmäname_"));
        assertTrue(createdRyhma.getDescription().get("FI").contentEquals("ryhmäname"));
        assertTrue(createdRyhma.getKuvaus().get("FI").contentEquals("ryhmäkuvaus"));
        assertEquals(1, createdRyhma.getOrganisaatioViite().size());
        assertEquals("org tyyppi", createdRyhma.getOrganisaatioViite().get(0).getOrganisaatioTyyppi());
        assertFalse(createdRyhma.isRyhmaRestriction());
        assertEquals(KayttajaTyyppi.PALVELU, createdRyhma.getSallittuKayttajatyyppi());
        assertThat(createdRyhma)
                .returns(now1, KayttoOikeusRyhmaDto::getMuokattu)
                .returns("1.2.3.4.5", KayttoOikeusRyhmaDto::getMuokkaaja);

        OffsetDateTime now2 = OffsetDateTime.of(2019, 10, 30, 8, 23, 22, 0, ZoneOffset.UTC);
        when(timeService.getOffsetDateTimeNow()).thenReturn(now2);

        ryhma.setNimi(new TextGroupDto().put("FI", "uusi nimi"));
        ryhma.setKuvaus(new TextGroupDto().put("FI", "uusi kuvaus"));
        ryhma.setRooliRajoite("uusi rajoite");
        ryhma.setOrganisaatioTyypit(singletonList("uusi org tyyppi"));
        ryhma.setSallittuKayttajatyyppi(null);
        kayttoOikeusService.updateKayttoOikeusForKayttoOikeusRyhma(createdRyhmaId, ryhma);

        createdRyhma = kayttoOikeusService.findKayttoOikeusRyhma(createdRyhmaId, false);
        assertTrue(createdRyhma.getDescription().get("FI").contentEquals("uusi nimi"));
        assertTrue(createdRyhma.getKuvaus().get("FI").contentEquals("uusi kuvaus"));
        assertTrue(createdRyhma.getOrganisaatioViite().get(0).getOrganisaatioTyyppi().contentEquals("uusi org tyyppi"));
        assertNull(createdRyhma.getSallittuKayttajatyyppi());
        assertThat(createdRyhma)
                .returns(now2, KayttoOikeusRyhmaDto::getMuokattu)
                .returns("1.2.3.4.5", KayttoOikeusRyhmaDto::getMuokkaaja);
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void updatePassivoitu() {
        KayttoOikeusRyhma passivoituKayttoOikeusRyhma = populate(kayttoOikeusRyhma("RYHMA")
                .withNimi(text("FI", "Käyttäjähallinta")
                        .put("EN", "User management"))
                .withOikeus(oikeus("HENKILOHALLINTA", "CRUD"))
                .withOikeus(oikeus("KOODISTO", "READ"))
                .asPassivoitu());
        KayttoOikeusRyhmaModifyDto ryhmaModifyDto = KayttoOikeusRyhmaModifyDto.builder()
                .nimi(new TextGroupDto()
                        .put("FI", "Uusi nimi")
                        .put("SV", "Uusi nimi sv")
                        .put("EN", "Uusi nimi en"))
                .palvelutRoolit(new ArrayList<>())
                .build();
        this.kayttoOikeusService.updateKayttoOikeusForKayttoOikeusRyhma(passivoituKayttoOikeusRyhma.getId(), ryhmaModifyDto);
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void createKayttoOikeusTest(){
        KayttoOikeus oikeus = populate(oikeus("HENKILOHALLINTA", "CRUD"));
        Palvelu palvelu = populate(palvelu("JOKUPALVELU").kuvaus(text("FI", "joku palvelu")
                .put("EN", "Person service").put("SV", "palvelu sv")));
        palvelu.getKayttoOikeus().add(oikeus);

        KayttoOikeusCreateDto ko = KayttoOikeusCreateDto.builder()
                .rooli("rooli")
                .palveluName("HENKILOHALLINTA")
                .textGroup(new TextGroupDto().put("FI", "kuvaus"))
                .build();
        long id = kayttoOikeusService.createKayttoOikeus(ko);
        KayttoOikeusDto dto = kayttoOikeusService.findKayttoOikeusById(id);
        assertEquals("rooli", dto.getRooli());
        assertEquals("HENKILOHALLINTA", dto.getPalvelu().getName());
        assertEquals("kuvaus", dto.getTextGroup().get("FI"));
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void listMyonnettyKayttoOikeusRyhmasMergedWithHenkilos(){
        MyonnettyKayttoOikeusRyhmaTapahtuma mkrt = populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA2").withNimi(text("FI", "Koodistonhallinta")
                        .put("EN", "Code management"))
                        .withOrganisaatiorajoite("123.123.123")
                        .withOrganisaatiorajoite("3.4.5.6.7")
                        .withSallittu(KayttajaTyyppi.PALVELU)
                        .withOikeus(oikeus("KAYTTOOIKEUS", "CRUD"))
        ));
        Optional<Long> id = mkrt.getKayttoOikeusRyhma().getOrganisaatioViite().stream()
                .map(OrganisaatioViite::getKayttoOikeusRyhma)
                .filter(ryhma -> ryhma.getSallittuKayttajatyyppi() != null)
                .map(IdentifiableAndVersionedEntity::getId)
                .findFirst();
        populate(kayttoOikeusRyhma("RYHMA1"));
        populate(kayttoOikeusRyhmaMyontoViite(mkrt.getKayttoOikeusRyhma().getId(), mkrt.getKayttoOikeusRyhma().getId()));

        List<MyonnettyKayttoOikeusDto> list = kayttoOikeusService.listMyonnettyKayttoOikeusRyhmasMergedWithHenkilos("1.2.3.4.5", "3.4.5.6.7", "1.2.3.4.5");
        assertEquals(1, list.size());
        // Ei aseteta sallittuKayttajatyyppi kenttää koska käyttäjällä ei ole voimassa olevia oikeuksia joten käyttöoikeusryhmän
        // tietoja oteta mukaan
        assertThat(list)
                .extracting(myonnettyKayttoOikeus -> Optional.ofNullable(myonnettyKayttoOikeus.getRyhmaId()),
                        MyonnettyKayttoOikeusDto::isSelected,
                        MyonnettyKayttoOikeusDto::getSallittuKayttajatyyppi)
                .containsExactly(Tuple.tuple(id, true, KayttajaTyyppi.PALVELU));

        list = kayttoOikeusService.listMyonnettyKayttoOikeusRyhmasMergedWithHenkilos("1.2.3.4.5", "3.4.5.6.madeup", "1.2.3.4.5");
        assertEquals(0, list.size());
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void listMyonnettyKayttoOikeusRyhmasMergedWithHenkilosWithMyontoViiteToRootOrganisation(){
        given(this.commonProperties.getRootOrganizationOid()).willReturn("3.4.5.6.7");
        MyonnettyKayttoOikeusRyhmaTapahtuma mko = populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA2").withNimi(text("FI", "Koodistonhallinta")
                        .put("EN", "Code management"))
                        .withOrganisaatiorajoite("TYYPPI")
                        .withOikeus(oikeus("KAYTTOOIKEUS", "CRUD"))
        ));

        MyonnettyKayttoOikeusRyhmaTapahtuma mko2 = populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA3").withNimi(text("FI", "Koodistonhallinta")
                        .put("EN", "Code management"))
                        .withOrganisaatiorajoite("3.4.5.6.7")
                        .withOikeus(oikeus("KAYTTOOIKEUS", "CRUD")
        ).withSallittu(KayttajaTyyppi.PALVELU)));

        populate(kayttoOikeusRyhmaMyontoViite(mko.getKayttoOikeusRyhma().getId(), mko2.getKayttoOikeusRyhma().getId()));

        List<MyonnettyKayttoOikeusDto> list = kayttoOikeusService.listMyonnettyKayttoOikeusRyhmasMergedWithHenkilos("1.2.3.4.5", "3.4.5.6.7", "1.2.3.4.5");
        assertThat(list)
                .hasSize(1)
                .extracting(MyonnettyKayttoOikeusDto::getRyhmaTunniste, MyonnettyKayttoOikeusDto::getTyyppi, MyonnettyKayttoOikeusDto::getSallittuKayttajatyyppi)
                .containsExactly(tuple("RYHMA3", "KORyhma", KayttajaTyyppi.PALVELU));
        assertThat(list)
                .extracting(MyonnettyKayttoOikeusDto::getRyhmaNames)
                .flatExtracting(TextGroupDto::getTexts)
                .extracting(TextDto::getText)
                .containsExactlyInAnyOrder("Code management", "Koodistonhallinta");
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void findKayttoOikeusRyhmasByKayttoOikeusIdsTest() {
        populate(kayttoOikeusRyhma("RYHMA")
                .withNimi(text("FI", "Käyttäjähallinta")
                        .put("EN", "User management")
                        .put("SV", "på svenska"))
                .withRooliRajoite("roolirajoite")
                .withOikeus(oikeus("HENKILOHALLINTA", "CRUD"))
                .withOikeus(oikeus("KOODISTO", "READ")).withOrganisaatiorajoite("123.123.123"));
        populate(kayttoOikeusRyhma("RYHMA1"));

        List<KayttoOikeusRyhmaDto> ryhmas = kayttoOikeusService.findKayttoOikeusRyhmasByKayttoOikeusList(singletonMap("HENKILOHALLINTA", "CRUD"));
        assertEquals(1, ryhmas.size());
        assertEquals("RYHMA", ryhmas.get(0).getName());
        assertEquals("roolirajoite", ryhmas.get(0).getRooliRajoite());
        assertEquals("Käyttäjähallinta", ryhmas.get(0).getDescription().get("FI"));
        assertEquals("User management", ryhmas.get(0).getDescription().get("EN"));
        assertEquals("på svenska", ryhmas.get(0).getDescription().get("SV"));
        assertEquals("123.123.123", ryhmas.get(0).getOrganisaatioViite().get(0).getOrganisaatioTyyppi());

        ryhmas = kayttoOikeusService.findKayttoOikeusRyhmasByKayttoOikeusList(emptyMap());
        assertEquals(0, ryhmas.size());
    }
}
