package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioWithChildrenDto;
import fi.vm.sade.kayttooikeus.dto.enumeration.OrganisaatioStatus;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.AnomusRepository;
import fi.vm.sade.kayttooikeus.repositories.KayttoOikeusRepository;
import fi.vm.sade.kayttooikeus.repositories.OrganisaatioHenkiloRepository;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import fi.vm.sade.kayttooikeus.service.it.AbstractServiceIntegrationTest;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static fi.vm.sade.kayttooikeus.dto.KayttoOikeudenTila.SULJETTU;
import static fi.vm.sade.kayttooikeus.repositories.populate.AnomusPopulator.anomus;
import static fi.vm.sade.kayttooikeus.repositories.populate.HaettuKayttoOikeusRyhmaPopulator.haettuKayttooikeusryhma;
import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.henkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusPopulator.oikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma;
import static fi.vm.sade.kayttooikeus.repositories.populate.MyonnettyKayttooikeusRyhmaTapahtumaPopulator.kayttooikeusTapahtuma;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloKayttoOikeusPopulator.myonnettyKayttoOikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloPopulator.organisaatioHenkilo;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class OrganisaatioHenkiloServiceTest extends AbstractServiceIntegrationTest {
    @MockBean
    private OrganisaatioClient organisaatioClient;

    @Autowired
    private OrganisaatioHenkiloRepository organisaatioHenkiloRepository;

    @Autowired
    private KayttoOikeusRepository kayttoOikeusRepository;

    @Autowired
    private AnomusRepository anomusRepository;

    @Autowired
    private OrganisaatioHenkiloService organisaatioHenkiloService;

    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void listOrganisaatioHenkilosTest() {
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(eq("1.2.3.4.1"))).willAnswer(invocation -> {
            OrganisaatioPerustieto orgDto = new OrganisaatioPerustieto();
            orgDto.setOid("1.2.3.4.1");
            orgDto.setNimi(new TextGroupMapDto().put("fi", "Suomeksi").put("en", "In English").asMap());
            orgDto.setOrganisaatiotyypit(asList("organisaatiotyyppi_01", "organisaatiotyyppi_02", "tuntematon_koodi"));
            orgDto.setStatus(OrganisaatioStatus.AKTIIVINEN);
            return Optional.of(orgDto);
        });
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(eq("1.2.3.4.2"))).willAnswer(invocation -> {
            OrganisaatioPerustieto orgDto = new OrganisaatioPerustieto();
            orgDto.setOid("1.2.3.4.2");
            orgDto.setNimi(new TextGroupMapDto().put("en", "Only in English").asMap());
            orgDto.setOrganisaatiotyypit(singletonList("organisaatiotyyppi_01"));
            orgDto.setStatus(OrganisaatioStatus.AKTIIVINEN);
            orgDto.setChildren(Lists.newArrayList(
                    OrganisaatioPerustieto.builder()
                            .oid("1.2.3.4.2.1")
                            .status(OrganisaatioStatus.PASSIIVINEN)
                            .children(Lists.newArrayList())
                            .build(),
                    OrganisaatioPerustieto.builder()
                            .oid("1.2.3.4.2.2")
                            .status(OrganisaatioStatus.AKTIIVINEN)
                            .children(Lists.newArrayList())
                            .build()));
            return Optional.of(orgDto);
        });
        populate(organisaatioHenkilo("1.2.3.4.5", "1.2.3.4.1")
                .voimassaAlkaen(LocalDate.now()).voimassaAsti(LocalDate.now().plusYears(1))
                .tehtavanimike("Devaaja"));
        populate(organisaatioHenkilo("1.2.3.4.5", "1.2.3.4.2")
                .voimassaAlkaen(LocalDate.now().minusYears(1))
                .tehtavanimike("Opettaja"));
        populate(organisaatioHenkilo("1.2.3.4.5", "1.2.3.4.3")
                .voimassaAlkaen(LocalDate.now().minusYears(1))
                .tehtavanimike("Testaaja"));

        List<OrganisaatioHenkiloWithOrganisaatioDto> result = organisaatioHenkiloService.listOrganisaatioHenkilos("1.2.3.4.5", "fi", null);
        assertThat(result)
                .extracting(OrganisaatioHenkiloWithOrganisaatioDto::getOrganisaatio)
                .extracting(OrganisaatioWithChildrenDto::getOid)
                .containsExactlyInAnyOrder("1.2.3.4.1", "1.2.3.4.2", "1.2.3.4.3");
        assertThat(result)
                .extracting(OrganisaatioHenkiloWithOrganisaatioDto::getOrganisaatio)
                .flatExtracting(OrganisaatioWithChildrenDto::getChildren)
                .extracting(OrganisaatioWithChildrenDto::getOid)
                .containsExactlyInAnyOrder("1.2.3.4.2.2");
        assertThat(result)
                .extracting(OrganisaatioHenkiloWithOrganisaatioDto::getOrganisaatio)
                .extracting(OrganisaatioWithChildrenDto::getNimi)
                .extracting(TextGroupMapDto::getTexts)
                .flatExtracting(Map::values)
                .containsExactlyInAnyOrder("Only in English", "Suomeksi", "In English", "Okänd organisation", "Tuntematon organisaatio", "Unknown organisation");
        assertThat(result)
                .extracting(OrganisaatioHenkiloWithOrganisaatioDto::getOrganisaatio)
                .flatExtracting(OrganisaatioWithChildrenDto::getTyypit)
                .containsExactlyInAnyOrder("KOULUTUSTOIMIJA", "OPPILAITOS", "KOULUTUSTOIMIJA");
        assertThat(result)
                .extracting(OrganisaatioHenkiloWithOrganisaatioDto::getTehtavanimike)
                .containsExactlyInAnyOrder("Devaaja", "Opettaja", "Testaaja");
        assertThat(result)
                .extracting(OrganisaatioHenkiloWithOrganisaatioDto::getVoimassaAlkuPvm)
                .allSatisfy(alkupvm -> assertThat(alkupvm).isLessThanOrEqualTo(LocalDate.now()));
        assertThat(result)
                .extracting(OrganisaatioHenkiloWithOrganisaatioDto::getVoimassaLoppuPvm)
                .filteredOn(Objects::nonNull)
                .hasSize(1)
                .allSatisfy(loppupvm -> assertThat(loppupvm).isGreaterThanOrEqualTo(LocalDate.now()));
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void listPossibleHenkiloTypesAccessibleForCurrentUserRekisterinpitajaTest() {
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.3.4.5", "6.7.8.9.0"),
                kayttoOikeusRyhma("kayttooikeusryhma1").withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_REKISTERINPITAJA))));

        List<KayttajaTyyppi> list = organisaatioHenkiloService.listPossibleHenkiloTypesAccessibleForCurrentUser();
        assertEquals(new HashSet<>(asList(KayttajaTyyppi.VIRKAILIJA, KayttajaTyyppi.PALVELU)), new HashSet<>(list));
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void listPossibleHenkiloTypesAccessibleForCurrentUserCrudTest() {
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.3.4.5", "6.7.8.9.0"),
                kayttoOikeusRyhma("kayttooikeusryhma1").withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, ROLE_CRUD))));

        List<KayttajaTyyppi> list = organisaatioHenkiloService.listPossibleHenkiloTypesAccessibleForCurrentUser();
        assertEquals(new HashSet<>(asList(KayttajaTyyppi.VIRKAILIJA)), new HashSet<>(list));
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void findOrganisaatioHenkiloByHenkiloAndOrganisaatioTest() {
        populate(organisaatioHenkilo("1.2.3.4.5", "5.6.7.8.9"));

        OrganisaatioHenkiloDto organisaatioHenkilo = organisaatioHenkiloService.findOrganisaatioHenkiloByHenkiloAndOrganisaatio("1.2.3.4.5", "5.6.7.8.9");
        assertThat(organisaatioHenkilo).returns("5.6.7.8.9", OrganisaatioHenkiloDto::getOrganisaatioOid);
    }

    @Test(expected = NotFoundException.class)
    @WithMockUser(username = "1.2.3.4.5")
    public void findOrganisaatioHenkiloByHenkiloAndOrganisaatioErrorTest() {
        organisaatioHenkiloService.findOrganisaatioHenkiloByHenkiloAndOrganisaatio("1.2.3.4.5", "1.1.1.1.1");
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void passivoiHenkiloOrganisation() {
        OrganisaatioHenkilo organisaatioHenkilo = populate(organisaatioHenkilo(henkilo("1.2.3.4.5"), "1.1.1.1.1"));
        KayttoOikeusRyhma kayttoOikeusRyhma = populate(kayttoOikeusRyhma("käyttöoikeusryhmä"));
        MyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma = populate(kayttooikeusTapahtuma(organisaatioHenkilo, kayttoOikeusRyhma));

        this.organisaatioHenkiloService.passivoiHenkiloOrganisation("1.2.3.4.5", "1.1.1.1.1");
        assertThat(organisaatioHenkilo.isPassivoitu()).isTrue();
        assertThat(organisaatioHenkilo.getMyonnettyKayttoOikeusRyhmas()).isEmpty();
        assertThat(organisaatioHenkilo.getKayttoOikeusRyhmaHistorias()).extracting("tila").containsExactly(SULJETTU);
    }

    @Test(expected = NotFoundException.class)
    @WithMockUser(username = "1.2.3.4.5")
    public void passivoiHenkiloOrganisationNotFound() {
        organisaatioHenkiloService.passivoiHenkiloOrganisation("1.2.3.4.5", "1.1.1.1.1");
    }

    @Test
    public void kasitteleOrganisaatioidenLakkautus() {
        when(organisaatioClient.getLakkautetutOids()).thenReturn(Stream.of("organisaatio1").collect(toSet()));
        populate(henkilo(("kayttaja")));

        OrganisaatioHenkilo organisaatioHenkilo1 = populate(organisaatioHenkilo("henkilo1", "organisaatio1"));
        OrganisaatioHenkilo organisaatioHenkilo2 = populate(organisaatioHenkilo("henkilo2", "organisaatio2"));
        Anomus anomus1 = populate(anomus("example1@example.com")
                .organisaatioOid("organisaatio1")
                .tila(AnomuksenTila.ANOTTU)
                .withHaettuRyhma(haettuKayttooikeusryhma(KayttoOikeudenTila.ANOTTU)
                        .withRyhma(kayttoOikeusRyhma("kayttooikeusryhma1"))));
        Anomus anomus2 = populate(anomus("example2@example.com")
                .organisaatioOid("organisaatio2")
                .tila(AnomuksenTila.ANOTTU)
                .withHaettuRyhma(haettuKayttooikeusryhma(KayttoOikeudenTila.ANOTTU)
                        .withRyhma(kayttoOikeusRyhma("kayttooikeusryhma2"))));

        organisaatioHenkiloService.kasitteleOrganisaatioidenLakkautus("kayttaja");

        assertThat(organisaatioHenkiloRepository.findAll())
                .extracting(OrganisaatioHenkilo::getId, OrganisaatioHenkilo::isAktiivinen)
                .containsExactlyInAnyOrder(
                        tuple(organisaatioHenkilo1.getId(), false),
                        tuple(organisaatioHenkilo2.getId(), true));
        assertThat(anomusRepository.findAll())
                .extracting(Anomus::getId, Anomus::getAnomuksenTila)
                .containsExactlyInAnyOrder(
                        tuple(anomus1.getId(), AnomuksenTila.HYLATTY),
                        tuple(anomus2.getId(), AnomuksenTila.ANOTTU));

        OrganisaatioHenkilo organisaatioHenkilo3 = populate(organisaatioHenkilo("henkilo3", "organisaatio1"));
        Anomus anomus3 = populate(anomus("example3@example.com")
                .organisaatioOid("organisaatio1")
                .tila(AnomuksenTila.ANOTTU)
                .withHaettuRyhma(haettuKayttooikeusryhma(KayttoOikeudenTila.ANOTTU)
                        .withRyhma(kayttoOikeusRyhma("kayttooikeusryhma3"))));

        organisaatioHenkiloService.kasitteleOrganisaatioidenLakkautus("kayttaja");

        assertThat(organisaatioHenkiloRepository.findAll())
                .extracting(OrganisaatioHenkilo::getId, OrganisaatioHenkilo::isAktiivinen)
                .containsExactlyInAnyOrder(
                        tuple(organisaatioHenkilo1.getId(), false),
                        tuple(organisaatioHenkilo2.getId(), true),
                        tuple(organisaatioHenkilo3.getId(), true));
        assertThat(anomusRepository.findAll())
                .extracting(Anomus::getId, Anomus::getAnomuksenTila)
                .containsExactlyInAnyOrder(
                        tuple(anomus1.getId(), AnomuksenTila.HYLATTY),
                        tuple(anomus2.getId(), AnomuksenTila.ANOTTU),
                        tuple(anomus3.getId(), AnomuksenTila.ANOTTU));
    }

}
