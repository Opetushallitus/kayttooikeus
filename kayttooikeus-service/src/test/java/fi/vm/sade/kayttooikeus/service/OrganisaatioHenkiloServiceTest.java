package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.KayttajaTyyppi;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloDto;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloWithOrganisaatioDto;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloWithOrganisaatioDto.OrganisaatioDto;
import fi.vm.sade.kayttooikeus.dto.TextGroupMapDto;
import fi.vm.sade.kayttooikeus.dto.enumeration.OrganisaatioStatus;
import fi.vm.sade.kayttooikeus.model.KayttoOikeusRyhma;
import fi.vm.sade.kayttooikeus.model.MyonnettyKayttoOikeusRyhmaTapahtuma;
import fi.vm.sade.kayttooikeus.model.OrganisaatioHenkilo;
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

import static fi.vm.sade.kayttooikeus.dto.KayttoOikeudenTila.SULJETTU;
import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.henkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma;
import static fi.vm.sade.kayttooikeus.repositories.populate.MyonnettyKayttooikeusRyhmaTapahtumaPopulator.kayttooikeusTapahtuma;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloPopulator.organisaatioHenkilo;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.*;
import static fi.vm.sade.kayttooikeus.util.JsonUtil.readJson;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
public class OrganisaatioHenkiloServiceTest extends AbstractServiceIntegrationTest {
    @MockBean
    private OrganisaatioClient organisaatioClient;

    @MockBean
    private OrganisaatioHenkiloRepository organisaatioHenkiloRepository;

    @MockBean
    private KayttoOikeusRepository kayttoOikeusRepository;

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
        given(this.organisaatioHenkiloRepository.findActiveOrganisaatioHenkiloListDtos("1.2.3.4.5", null)).willReturn(
                asList(OrganisaatioHenkiloWithOrganisaatioDto.organisaatioBuilder()
                                .id(1L)
                                .passivoitu(false)
                                .voimassaAlkuPvm(LocalDate.now())
                                .voimassaLoppuPvm(LocalDate.now().plusYears(1))
                                .tehtavanimike("Devaaja")
                                .organisaatio(OrganisaatioDto.builder().oid("1.2.3.4.1").build()).build(),
                        OrganisaatioHenkiloWithOrganisaatioDto.organisaatioBuilder()
                                .id(2L)
                                .voimassaAlkuPvm(LocalDate.now().minusYears(1))
                                .passivoitu(false)
                                .tehtavanimike("Opettaja")
                                .organisaatio(OrganisaatioDto.builder().oid("1.2.3.4.2").build())
                                .build(),
                        // Unknown organisation (not found in cache so defaulted)
                        OrganisaatioHenkiloWithOrganisaatioDto.organisaatioBuilder()
                                .id(3L)
                                .voimassaAlkuPvm(LocalDate.now().minusYears(1))
                                .passivoitu(false)
                                .tehtavanimike("Testaaja")
                                .organisaatio(OrganisaatioDto.builder().oid("1.2.3.4.3").build()).build()
                ));

        List<OrganisaatioHenkiloWithOrganisaatioDto> result = organisaatioHenkiloService.listOrganisaatioHenkilos("1.2.3.4.5", "fi", null);
        assertThat(result)
                .extracting(OrganisaatioHenkiloWithOrganisaatioDto::getOrganisaatio)
                .extracting(OrganisaatioDto::getOid)
                .containsExactlyInAnyOrder("1.2.3.4.1", "1.2.3.4.2", "1.2.3.4.3");
        assertThat(result)
                .extracting(OrganisaatioHenkiloWithOrganisaatioDto::getOrganisaatio)
                .flatExtracting(OrganisaatioDto::getChildren)
                .extracting(OrganisaatioDto::getOid)
                .containsExactlyInAnyOrder("1.2.3.4.2.2");
        assertThat(result)
                .extracting(OrganisaatioHenkiloWithOrganisaatioDto::getId)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(result)
                .extracting(OrganisaatioHenkiloWithOrganisaatioDto::getOrganisaatio)
                .extracting(OrganisaatioDto::getNimi)
                .extracting(TextGroupMapDto::getTexts)
                .flatExtracting(Map::values)
                .containsExactlyInAnyOrder("Only in English", "Suomeksi", "In English", "Okänd organisation", "Tuntematon organisaatio", "Unknown organisation");
        assertThat(result)
                .extracting(OrganisaatioHenkiloWithOrganisaatioDto::getOrganisaatio)
                .flatExtracting(OrganisaatioDto::getTyypit)
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
        given(this.kayttoOikeusRepository
                .isHenkiloMyonnettyKayttoOikeusToPalveluInRole("1.2.3.4.5", PALVELU_HENKILONHALLINTA, ROLE_ADMIN))
                .willReturn(true);

        List<KayttajaTyyppi> list = organisaatioHenkiloService.listPossibleHenkiloTypesAccessibleForCurrentUser();
        assertEquals(new HashSet<>(asList(KayttajaTyyppi.VIRKAILIJA, KayttajaTyyppi.PALVELU)), new HashSet<>(list));
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void listPossibleHenkiloTypesAccessibleForCurrentUserCrudTest() {
        given(this.kayttoOikeusRepository
                .isHenkiloMyonnettyKayttoOikeusToPalveluInRole("1.2.3.4.5", PALVELU_HENKILONHALLINTA, ROLE_ADMIN))
                .willReturn(false);
        given(this.kayttoOikeusRepository
                .isHenkiloMyonnettyKayttoOikeusToPalveluInRole("1.2.3.4.5", PALVELU_HENKILONHALLINTA, ROLE_CRUD))
                .willReturn(true);

        List<KayttajaTyyppi> list = organisaatioHenkiloService.listPossibleHenkiloTypesAccessibleForCurrentUser();
        assertEquals(new HashSet<>(asList(KayttajaTyyppi.VIRKAILIJA)), new HashSet<>(list));
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void findOrganisaatioHenkiloByHenkiloAndOrganisaatioTest() {
        given(this.organisaatioHenkiloRepository.findByHenkiloOidAndOrganisaatioOid("1.2.3.4.5", "5.6.7.8.9"))
                .willReturn(Optional.of(OrganisaatioHenkiloDto.builder()
                        .id(33L).organisaatioOid("5.6.7.8.9").build()));

        OrganisaatioHenkiloDto organisaatioHenkilo = organisaatioHenkiloService.findOrganisaatioHenkiloByHenkiloAndOrganisaatio("1.2.3.4.5", "5.6.7.8.9");
        assertNotNull(organisaatioHenkilo);
        assertEquals(33L, organisaatioHenkilo.getId());
        assertEquals("5.6.7.8.9", organisaatioHenkilo.getOrganisaatioOid());
    }

    @Test(expected = NotFoundException.class)
    @WithMockUser(username = "1.2.3.4.5")
    public void findOrganisaatioHenkiloByHenkiloAndOrganisaatioErrorTest() {
        given(this.organisaatioHenkiloRepository.findByHenkiloOidAndOrganisaatioOid("1.2.3.4.5", "1.1.1.1.1")).willReturn(Optional.empty());
        organisaatioHenkiloService.findOrganisaatioHenkiloByHenkiloAndOrganisaatio("1.2.3.4.5", "1.1.1.1.1");
    }

    @Test
    @WithMockUser(username = "1.2.3.4.5")
    public void passivoiHenkiloOrganisation() {
        OrganisaatioHenkilo organisaatioHenkilo = populate(organisaatioHenkilo(henkilo("henkilo1"), "1.1.1.1.1"));
        populate(henkilo("1.2.3.4.5"));
        given(this.organisaatioHenkiloRepository.findByHenkiloOidHenkiloAndOrganisaatioOid("1.2.3.4.5", "1.1.1.1.1"))
                .willReturn(Optional.of(organisaatioHenkilo));
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
        given(this.organisaatioHenkiloRepository.findByHenkiloOidHenkiloAndOrganisaatioOid("1.2.3.4.5", "1.1.1.1.1"))
                .willReturn(Optional.empty());
        organisaatioHenkiloService.passivoiHenkiloOrganisation("1.2.3.4.5", "1.1.1.1.1");
    }


}
