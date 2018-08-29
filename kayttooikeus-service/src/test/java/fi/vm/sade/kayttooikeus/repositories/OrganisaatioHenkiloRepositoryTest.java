package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloDto;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloWithOrganisaatioDto;
import fi.vm.sade.kayttooikeus.dto.PalveluRooliGroup;
import fi.vm.sade.kayttooikeus.model.KayttoOikeusRyhma;
import fi.vm.sade.kayttooikeus.model.MyonnettyKayttoOikeusRyhmaTapahtuma;
import fi.vm.sade.kayttooikeus.model.OrganisaatioHenkilo;
import fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusPopulator;
import fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaPopulator;
import fi.vm.sade.kayttooikeus.repositories.populate.MyonnettyKayttooikeusRyhmaTapahtumaPopulator;
import org.assertj.core.util.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloTyyppi.OPISKELIJA;
import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.henkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusPopulator.oikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloKayttoOikeusPopulator.myonnettyKayttoOikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloPopulator.organisaatioHenkilo;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.PALVELU_ANOMUSTENHALLINTA;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.PALVELU_HENKILONHALLINTA;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.ROLE_CRUD;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
public class OrganisaatioHenkiloRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private OrganisaatioHenkiloRepository organisaatioHenkiloRepository;
    
    @Test
    public void findDistinctOrganisaatiosForHenkiloOidEmptyTest() {
        List<String> results = organisaatioHenkiloRepository.findDistinctOrganisaatiosForHenkiloOid("oid");
        assertThat(results).isEmpty();
    }
    
    @Test
    public void findDistinctOrganisaatiosForHenkiloOidTest() {
        populate(organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"));
        List<String> results = organisaatioHenkiloRepository.findDistinctOrganisaatiosForHenkiloOid("1.2.3.4.5");
        assertThat(results).hasSize(1);
        assertThat(results).containsExactly("3.4.5.6.7");
    }

    @Test
    public void findOrganisaatioHenkiloListDtosTest() {
        OrganisaatioHenkilo oh1 = populate(organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"));
        OrganisaatioHenkilo oh2 =populate(organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.8")
                .voimassaAlkaen(LocalDate.now().minusDays(2)).tyyppi(OPISKELIJA)
                .voimassaAsti(LocalDate.now().plusYears(1))
                .tehtavanimike("Devaaja"));
        populate(organisaatioHenkilo(henkilo("1.2.3.4.6"), "3.4.5.6.9"));

        List<OrganisaatioHenkiloWithOrganisaatioDto> results = organisaatioHenkiloRepository.findActiveOrganisaatioHenkiloListDtos("1.2.3.4.5");
        assertThat(results).hasSize(2);
        assertThat(results).extracting(OrganisaatioHenkiloWithOrganisaatioDto::getOrganisaatioOid)
                .containsExactlyInAnyOrder("3.4.5.6.7", "3.4.5.6.8");
        assertThat(results).extracting(OrganisaatioHenkiloWithOrganisaatioDto::getId)
                .containsExactlyInAnyOrder(oh1.getId(), oh2.getId());
        OrganisaatioHenkiloWithOrganisaatioDto result2 = results.get(1);
        assertThat(result2.getTehtavanimike()).isEqualTo(oh2.getTehtavanimike());
        assertThat(result2.getOrganisaatioHenkiloTyyppi()).isEqualTo(OPISKELIJA);
        assertThat(result2.getVoimassaAlkuPvm()).isEqualTo(oh2.getVoimassaAlkuPvm());
        assertThat(result2.getVoimassaLoppuPvm()).isEqualTo(oh2.getVoimassaLoppuPvm());
    }

    @Test
    public void findActiveOrganisaatioHenkiloListDtosWithoutGivenRoles() {
        // organisaatio, johon 'henkilö'llä on voimassa oleva palvelurooli KAYTTOOIKEUS_READ
        OrganisaatioHenkilo oh1 = populate(organisaatioHenkilo(henkilo("henkilo"), "oh1")
                .voimassaAlkaen(LocalDate.now().minusDays(2)).tyyppi(OPISKELIJA)
                .voimassaAsti(LocalDate.now().plusYears(1))
                .tehtavanimike("Devaaja"));
        KayttoOikeusRyhmaPopulator kor1Populator = KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma("kor1Populator");
        kor1Populator.withOikeus(KayttoOikeusPopulator.oikeus("KAYTTOOIKEUS", "READ"));
        KayttoOikeusRyhma kor1 = populate(kor1Populator);
        MyonnettyKayttoOikeusRyhmaTapahtuma mkor1 = populate(MyonnettyKayttooikeusRyhmaTapahtumaPopulator.kayttooikeusTapahtuma(oh1, kor1));
        oh1.setMyonnettyKayttoOikeusRyhmas(Sets.newHashSet(Arrays.asList(mkor1)));

        //organisaatio, johon henkilöllä voimassa oleva palvelurooli KAYTTOOIKEUS_VASTUUKAYTTAJA
        OrganisaatioHenkilo oh2 = populate(organisaatioHenkilo(henkilo("henkilo"), "oh2")
            .voimassaAlkaen(LocalDate.now().minusDays(2)).tyyppi(OPISKELIJA)
                .voimassaAsti(LocalDate.now().plusYears(1))
                .tehtavanimike("Devaaja"));
        KayttoOikeusRyhmaPopulator kor2Populator = KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma("kor2Populator");
        kor2Populator.withOikeus(KayttoOikeusPopulator.oikeus("KAYTTOOIKEUS", "VASTUUKAYTTAJA"));
        KayttoOikeusRyhma kor2 = populate(kor2Populator);
        MyonnettyKayttoOikeusRyhmaTapahtuma mkor2 = populate(MyonnettyKayttooikeusRyhmaTapahtumaPopulator.kayttooikeusTapahtuma(oh2, kor2));
        oh2.setMyonnettyKayttoOikeusRyhmas(Sets.newHashSet(Arrays.asList(mkor2)));

        OrganisaatioHenkilo oh3 = populate(organisaatioHenkilo(henkilo("henkilo"),"oh3" ));

        List<OrganisaatioHenkiloWithOrganisaatioDto> results = organisaatioHenkiloRepository.findActiveOrganisaatioHenkiloListDtos("henkilo", PalveluRooliGroup.HENKILOHAKU);
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).getOrganisaatio().getOid()).isEqualTo("oh1");

    }

    @Test
    public void findByHenkiloOidAndOrganisaatioOidTest() {
        populate(organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"));
        Optional<OrganisaatioHenkiloDto> organisaatioHenkilo = organisaatioHenkiloRepository.findByHenkiloOidAndOrganisaatioOid("1.2.3.4.5", "3.4.5.6.7");
        assertThat(organisaatioHenkilo).isPresent()
                .map(OrganisaatioHenkiloDto::getOrganisaatioOid)
                .contains("3.4.5.6.7");

        organisaatioHenkilo = organisaatioHenkiloRepository.findByHenkiloOidAndOrganisaatioOid("1.2.3.4.5", "1.1.1.1.madeup");
        assertThat(organisaatioHenkilo).isNotPresent();

        organisaatioHenkilo = organisaatioHenkiloRepository.findByHenkiloOidAndOrganisaatioOid("1.2.3.4.madeup", "3.4.5.6.7");
        assertThat(organisaatioHenkilo).isNotPresent();
    }

    @Test
    public void findOrganisaatioHenkilosForHenkiloTest() throws Exception {
        populate(organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"));
        populate(organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.8"));
        populate(organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.9"));
        populate(organisaatioHenkilo(henkilo("1.2.3.4.6"), "3.4.5.6.9"));

        List<OrganisaatioHenkiloDto> organisaatioHenkilos = organisaatioHenkiloRepository.findOrganisaatioHenkilosForHenkilo("1.2.3.4.5");
        assertThat(organisaatioHenkilos)
                .extracting(OrganisaatioHenkiloDto::getOrganisaatioOid)
                .containsExactlyInAnyOrder("3.4.5.6.7", "3.4.5.6.8", "3.4.5.6.9");
    }

    @Test
    public void findValidByKayttooikeus() {
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.3.4.5", "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))));
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.3.4.5", "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA1").withOikeus(oikeus(PALVELU_ANOMUSTENHALLINTA, ROLE_CRUD))));
        Set<String> allowedOrganisaatioOids = this.organisaatioHenkiloRepository
                .findValidByKayttooikeus("1.2.3.4.5", PALVELU_HENKILONHALLINTA, ROLE_CRUD);
        assertThat(allowedOrganisaatioOids).containsExactly("3.4.5.6.7");
    }
}
