package fi.vm.sade.kayttooikeus.service.it;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.enumeration.OrderByHenkilohaku;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;
import fi.vm.sade.kayttooikeus.model.MyonnettyKayttoOikeusRyhmaTapahtuma;
import fi.vm.sade.kayttooikeus.model.OrganisaatioHenkilo;
import fi.vm.sade.kayttooikeus.repositories.KayttajatiedotRepository;
import fi.vm.sade.kayttooikeus.repositories.MyonnettyKayttoOikeusRyhmaTapahtumaRepository;
import fi.vm.sade.kayttooikeus.repositories.OrganisaatioHenkiloRepository;
import fi.vm.sade.kayttooikeus.repositories.dto.HenkilohakuResultDto;
import fi.vm.sade.kayttooikeus.service.HenkiloService;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.henkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttajatiedotPopulator.kayttajatiedot;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusPopulator.oikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloKayttoOikeusPopulator.myonnettyKayttoOikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloPopulator.organisaatioHenkilo;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.PALVELU_HENKILONHALLINTA;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.ROLE_ADMIN;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.ROLE_CRUD;
import static fi.vm.sade.kayttooikeus.util.CreateUtil.creaetOrganisaatioPerustietoWithNimi;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
public class HenkiloServiceTest extends AbstractServiceIntegrationTest {

    @Autowired
    private HenkiloService henkiloService;

    @Autowired
    private KayttajatiedotRepository kayttajatiedotRepository;

    @Autowired
    private OrganisaatioHenkiloRepository organisaatioHenkiloRepository;

    @Autowired
    private MyonnettyKayttoOikeusRyhmaTapahtumaRepository myonnettyKayttoOikeusRyhmaTapahtumaRepository;

    @Autowired
    private CommonProperties commonProperties;

    @MockBean
    private OrganisaatioClient organisaatioClient;

    @Test
    @WithMockUser(value = "1.2.3.4.1", authorities = {"ROLE_APP_HENKILONHALLINTA_OPHREKISTERI", "ROLE_APP_HENKILONHALLINTA_OPHREKISTERI_1.2.246.562.10.00000000001"})
    public void getByKayttajatunnus() {
        populate(kayttajatiedot(henkilo("oid1"), "user1"));

        HenkiloReadDto dto = henkiloService.getByKayttajatunnus("user1");

        assertThat(dto.getOid()).isEqualTo("oid1");
    }

    @Test
    @Transactional
    @WithMockUser(value = "1.2.3.4.1", authorities = {"ROLE_APP_HENKILONHALLINTA_OPHREKISTERI", "ROLE_APP_HENKILONHALLINTA_OPHREKISTERI_1.2.246.562.10.00000000001"})
    public void passivoiHenkilo() {
        // Käsittelijä
        populate(kayttajatiedot(henkilo("1.2.3.4.1"), "Käsittelijä"));
        // Passivoitava
        String oidHenkilo = "1.2.3.4.5";
        MyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma = populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo(oidHenkilo).withUsername("Passivoitava"), "4.5.6.7.8")
                        .tehtavanimike("testaaja"),
                kayttoOikeusRyhma("RYHMA2")
                        .withOikeus(oikeus("KOODISTO", "WRITE")))
                .voimassaAlkaen(LocalDate.now().minusMonths(1)).voimassaPaattyen(LocalDate.now().plusMonths(1)));
        myonnettyKayttoOikeusRyhmaTapahtuma.getOrganisaatioHenkilo().setMyonnettyKayttoOikeusRyhmas(Sets.newHashSet(myonnettyKayttoOikeusRyhmaTapahtuma));
        this.em.persist(myonnettyKayttoOikeusRyhmaTapahtuma);
        this.henkiloService.passivoi("1.2.3.4.5", "1.2.3.4.1");

        assertThat(kayttajatiedotRepository.findByHenkiloOidHenkilo("1.2.3.4.1")).map(Kayttajatiedot::getUsername)
                .hasValue("Käsittelijä");
        assertThat(kayttajatiedotRepository.findByHenkiloOidHenkilo("1.2.3.4.5")).isNotPresent();
        List<OrganisaatioHenkilo> henkilo = this.organisaatioHenkiloRepository.findByHenkiloOidHenkilo(oidHenkilo);
        assertThat(henkilo.size()).isEqualTo(1);
        assertThat(henkilo.get(0).getMyonnettyKayttoOikeusRyhmas()).isEmpty();
        Optional<MyonnettyKayttoOikeusRyhmaTapahtuma> mkrt = this.myonnettyKayttoOikeusRyhmaTapahtumaRepository.findById(myonnettyKayttoOikeusRyhmaTapahtuma.getId());
        assertThat(mkrt).isEmpty();
    }

    @Test
    @WithMockUser(value = "1.2.3.4.5", authorities = {"ROLE_APP_HENKILONHALLINTA_OPHREKISTERI_1.2.246.562.10.00000000001"})
    public void henkilohakuCountSearch() {
        populate(henkilo("1.2.3.4.2").withNimet("arpa", "kuutio"));
        populate(henkilo("1.2.3.4.3").withNimet("arpa", "kuutio"));
        HenkilohakuCriteriaDto henkilohakuCriteriaDto = new HenkilohakuCriteriaDto(true, true, true, true, null, null, null, "arpa", null, null);
        Long henkilohakuCount = this.henkiloService.henkilohakuCount(henkilohakuCriteriaDto);
        assertThat(henkilohakuCount).isEqualTo(2);
    }

    @Test
    @WithMockUser(value = "1.2.3.4.1", authorities = {"ROLE_APP_HENKILONHALLINTA_OPHREKISTERI", "ROLE_APP_HENKILONHALLINTA_OPHREKISTERI_1.2.246.562.10.00000000001"})
    public void henkilohakuCountWithUsername() {
        populate(kayttajatiedot(henkilo("1.2.3.4.2").withNimet("arpa", "kuutio"), "noppa"));
        populate(kayttajatiedot(henkilo("1.2.3.4.3").withNimet("toinen", "sukunimi"), "heppa"));

        HenkilohakuCriteriaDto henkilohakuCriteriaDto = new HenkilohakuCriteriaDto(null, true,
                null, null, null, null, null, "noppa", null, null);
        Collection<HenkilohakuResultDto> henkilohakuResultDtoList = this.henkiloService.henkilohaku(henkilohakuCriteriaDto, 0L, OrderByHenkilohaku.HENKILO_NIMI_ASC);
        assertThat(henkilohakuResultDtoList).extracting(HenkilohakuResultDto::getOidHenkilo).containsExactly("1.2.3.4.2");
        assertThat(henkiloService.henkilohakuCount(henkilohakuCriteriaDto)).isEqualTo(1);
    }

    @Test
    @WithMockUser(value = "1.2.3.4.1", authorities = {"ROLE_APP_HENKILONHALLINTA_OPHREKISTERI", "ROLE_APP_HENKILONHALLINTA_OPHREKISTERI_1.2.246.562.10.00000000001"})
    public void henkilohakuAsAdminSearchByName() {
        populate(henkilo("1.2.3.4.2").withNimet("arpa", "kuutio").withPassive(false).withDuplikate(true));
        populate(henkilo("1.2.3.4.3").withNimet("arpa", "kuutio").withPassive(true).withDuplikate(false));

        HenkilohakuCriteriaDto henkilohakuCriteriaDto = new HenkilohakuCriteriaDto(null, true,
                false, true, null, null, null, "arpa", null, null);
        Collection<HenkilohakuResultDto> henkilohakuResultDtoList = this.henkiloService.henkilohaku(henkilohakuCriteriaDto, 0L, OrderByHenkilohaku.HENKILO_NIMI_ASC);
        assertThat(henkilohakuResultDtoList).extracting(HenkilohakuResultDto::getNimi).containsExactly("kuutio, arpa");
    }

    @Test
    @WithMockUser(value = "1.2.3.4.1", authorities = {"ROLE_APP_HENKILONHALLINTA_OPHREKISTERI", "ROLE_APP_HENKILONHALLINTA_OPHREKISTERI_1.2.246.562.10.00000000001"})
    public void henkilohakuAsAdminSearchByNameBothOppijaAndVirkailija() {
        populate(henkilo("1.2.3.4.2").withNimet("arpa1", "kuutio").withPassive(false).withDuplikate(false));
        // current user
        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.1").withNimet("arpa2", "kuutio").withPassive(false).withDuplikate(false), "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA")
        ));

        HenkilohakuCriteriaDto henkilohakuCriteriaDto = new HenkilohakuCriteriaDto(null, true,
                false, true, null, null, null, "arpa", null, null);
        Collection<HenkilohakuResultDto> henkilohakuResultDtoList = this.henkiloService.henkilohaku(henkilohakuCriteriaDto, 0L, OrderByHenkilohaku.HENKILO_NIMI_ASC);
        assertThat(henkilohakuResultDtoList).extracting(HenkilohakuResultDto::getNimi).containsExactly("kuutio, arpa1", "kuutio, arpa2");
    }

    @Test
    @WithMockUser(value = "1.2.3.4.1", authorities = {"ROLE_APP_HENKILONHALLINTA_CRUD", "ROLE_APP_HENKILONHALLINTA_CRUD_1.2.246.562.10.00000000001"})
    public void henkilohakuAsNormalUserCantFindOppija() {
        populate(henkilo("1.2.3.4.2").withNimet("arpa1", "kuutio").withPassive(false).withDuplikate(false));
        // current user
        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.1").withNimet("arpa2", "kuutio").withPassive(false).withDuplikate(false), "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA")
        ));

        HenkilohakuCriteriaDto henkilohakuCriteriaDto = new HenkilohakuCriteriaDto(null, true,
                false, true, null, null, null, "arpa", null, null);
        Collection<HenkilohakuResultDto> henkilohakuResultDtoList = this.henkiloService.henkilohaku(henkilohakuCriteriaDto, 0L, OrderByHenkilohaku.HENKILO_NIMI_ASC);
        assertThat(henkilohakuResultDtoList).extracting(HenkilohakuResultDto::getNimi).containsExactly("kuutio, arpa2");
    }

    @Test
    @WithMockUser(value = "1.2.3.4.1", authorities = {"ROLE_APP_HENKILONHALLINTA_OPHREKISTERI", "ROLE_APP_HENKILONHALLINTA_OPHREKISTERI_1.2.246.562.10.00000000001"})
    public void henkilohakuAsAdminSearchOrganisationRequired() {
        populate(henkilo("1.2.3.4.2").withNimet("arpa", "kuutio").withPassive(false).withDuplikate(true));
        populate(henkilo("1.2.3.4.3").withNimet("arpa", "kuutio").withPassive(true).withDuplikate(false));

        HenkilohakuCriteriaDto henkilohakuCriteriaDto = new HenkilohakuCriteriaDto(null, false,
                false, true, null, null, null, "arpa", null, null);
        Collection<HenkilohakuResultDto> henkilohakuResultDtoList = this.henkiloService.henkilohaku(henkilohakuCriteriaDto, 0L, OrderByHenkilohaku.HENKILO_NIMI_ASC);
        assertThat(henkilohakuResultDtoList).isEmpty();
    }

    @Test
    @WithMockUser(value = "1.2.3.4.1", authorities = {"ROLE_APP_HENKILONHALLINTA_OPHREKISTERI", "ROLE_APP_HENKILONHALLINTA_OPHREKISTERI_1.2.246.562.10.00000000001"})
    public void henkilohakuAsAdminByOrganisation() {
        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.1"), commonProperties.getRootOrganizationOid()),
                kayttoOikeusRyhma("RYHMA")
        ));

        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA")
        ));

        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.6"), "3.4.5.6.8"),
                kayttoOikeusRyhma("RYHMA")
        ));

        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.7"), "3.4.5.6.7").passivoitu(),
                kayttoOikeusRyhma("RYHMA")
        ));

        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(eq("3.4.5.6.7")))
                .willReturn(Optional.of(creaetOrganisaatioPerustietoWithNimi("3.4.5.6.7", "nimiFi")));
        given(this.organisaatioClient.getChildOids("3.4.5.6.7"))
                .willReturn(Lists.newArrayList("3.4.5.6.7"));

        HenkilohakuCriteriaDto henkilohakuCriteriaDto = new HenkilohakuCriteriaDto(true, null,
                null, null, null, null, null, null, singleton("3.4.5.6.7"), null);
        Collection<HenkilohakuResultDto> henkilohakuResultDtoList = this.henkiloService.henkilohaku(henkilohakuCriteriaDto, 0L, OrderByHenkilohaku.HENKILO_NIMI_ASC);
        assertThat(henkilohakuResultDtoList).extracting(HenkilohakuResultDto::getOidHenkilo).containsExactly("1.2.3.4.5");
        assertThat(henkilohakuResultDtoList).flatExtracting(HenkilohakuResultDto::getOrganisaatioNimiList)
                .extracting(OrganisaatioMinimalDto::getLocalisedLabels)
                .extracting("fi")
                .containsExactly("nimiFi");
    }

    @Test
    @WithMockUser(value = "1.2.3.4.1", authorities = {"ROLE_APP_HENKILONHALLINTA_OPHREKISTERI", "ROLE_APP_HENKILONHALLINTA_OPHREKISTERI_1.2.246.562.10.00000000001"})
    public void henkilohakuAsAdminByKayttooikeusryhma() {
        MyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma = populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA")
        ));

        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.6"), "3.4.5.6.8"),
                kayttoOikeusRyhma("RYHMA2")
        ));

        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(eq("3.4.5.6.7")))
                .willReturn(Optional.of(creaetOrganisaatioPerustietoWithNimi("3.4.5.6.7", "nimiFi")));

        HenkilohakuCriteriaDto henkilohakuCriteriaDto = new HenkilohakuCriteriaDto(null, null,
                null, null, null, null, null, null, null,
                myonnettyKayttoOikeusRyhmaTapahtuma.getKayttoOikeusRyhma().getId());
        Collection<HenkilohakuResultDto> henkilohakuResultDtoList = this.henkiloService.henkilohaku(henkilohakuCriteriaDto, 0L, OrderByHenkilohaku.HENKILO_NIMI_ASC);
        assertThat(henkilohakuResultDtoList).extracting(HenkilohakuResultDto::getOidHenkilo).containsExactly("1.2.3.4.5");
        assertThat(henkilohakuResultDtoList).flatExtracting(HenkilohakuResultDto::getOrganisaatioNimiList)
                .extracting(OrganisaatioMinimalDto::getLocalisedLabels)
                .extracting("fi")
                .containsExactly("nimiFi");
    }

    @Test
    @WithMockUser(value = "1.2.3.4.1", authorities = {"ROLE_APP_HENKILONHALLINTA_OPHREKISTERI", "ROLE_APP_HENKILONHALLINTA_OPHREKISTERI_1.2.246.562.10.00000000001"})
    public void getOmatTiedotAdmin() {
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.3.4.1", "1.2.3.4.100"),
                kayttoOikeusRyhma("tunniste").withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_ADMIN))));
        OmatTiedotDto omatTiedotDto = this.henkiloService.getOmatTiedot();
        assertThat(omatTiedotDto.getIsAdmin()).isTrue();
        assertThat(omatTiedotDto.getIsMiniAdmin()).isTrue();
        assertThat(omatTiedotDto.getOidHenkilo()).isEqualTo("1.2.3.4.1");
        assertThat(omatTiedotDto.getOrganisaatiot())
                .extracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto::getOrganisaatioOid)
                .containsExactly("1.2.3.4.100");
        assertThat(omatTiedotDto.getOrganisaatiot())
                .flatExtracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto::getKayttooikeudet)
                .extracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto.KayttooikeusOikeudetDto::getPalvelu)
                .containsExactly(PALVELU_HENKILONHALLINTA);
        assertThat(omatTiedotDto.getOrganisaatiot())
                .flatExtracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto::getKayttooikeudet)
                .extracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto.KayttooikeusOikeudetDto::getOikeus)
                .containsExactly(ROLE_ADMIN);
    }

    @Test
    @WithMockUser(value = "1.2.3.4.1", authorities = {"ROLE_APP_HENKILONHALLINTA_CRUD", "ROLE_APP_HENKILONHALLINTA_CRUD_1.2.246.562.10.00000000001"})
    public void getOmatTiedotOphVirkailija() {
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.3.4.1", "1.2.3.4.100"),
                kayttoOikeusRyhma("tunniste").withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))));
        OmatTiedotDto omatTiedotDto = this.henkiloService.getOmatTiedot();
        assertThat(omatTiedotDto.getIsAdmin()).isFalse();
        assertThat(omatTiedotDto.getIsMiniAdmin()).isTrue();
        assertThat(omatTiedotDto.getOidHenkilo()).isEqualTo("1.2.3.4.1");
        assertThat(omatTiedotDto.getOrganisaatiot())
                .extracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto::getOrganisaatioOid)
                .containsExactly("1.2.3.4.100");
        assertThat(omatTiedotDto.getOrganisaatiot())
                .flatExtracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto::getKayttooikeudet)
                .extracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto.KayttooikeusOikeudetDto::getPalvelu)
                .containsExactly(PALVELU_HENKILONHALLINTA);
        assertThat(omatTiedotDto.getOrganisaatiot())
                .flatExtracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto::getKayttooikeudet)
                .extracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto.KayttooikeusOikeudetDto::getOikeus)
                .containsExactly(ROLE_CRUD);
    }
}
