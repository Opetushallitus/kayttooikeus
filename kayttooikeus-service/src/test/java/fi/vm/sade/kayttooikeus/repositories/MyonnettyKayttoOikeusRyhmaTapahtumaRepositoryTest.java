package fi.vm.sade.kayttooikeus.repositories;

import com.google.common.collect.Sets;
import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.repositories.criteria.KayttooikeusCriteria;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.List;

import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.henkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusPopulator.oikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloKayttoOikeusPopulator.myonnettyKayttoOikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloPopulator.organisaatioHenkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.TextGroupPopulator.text;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.PALVELU_HENKILONHALLINTA;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.ROLE_CRUD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
public class MyonnettyKayttoOikeusRyhmaTapahtumaRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private MyonnettyKayttoOikeusRyhmaTapahtumaRepository myonnettyKayttoOikeusRyhmaTapahtumaRepository;

    @Test
    public void findMasterIdsByHenkiloTest(){
        Long id = populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA").withNimi(text("FI", "Kuvaus"))
                        .withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))
                        .withOikeus(oikeus("KOODISTO", "READ")))
                .voimassaPaattyen(LocalDate.now())).getKayttoOikeusRyhma().getId();

        Long id2 = populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "4.5.6.7.8").tehtavanimike("testaaja"),
                kayttoOikeusRyhma("RYHMA2")
                        .withOikeus(oikeus("KOODISTO", "WRITE")))
                .voimassaPaattyen(LocalDate.now().plusMonths(3))).getKayttoOikeusRyhma().getId();

        Long id3 = populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.7"), "4.5.6.7.8").tehtavanimike("testaaja"),
                kayttoOikeusRyhma("RYHMA2")
                        .withOikeus(oikeus("KOODISTO", "WRITE")))
                .voimassaPaattyen(LocalDate.now().plusMonths(3))).getKayttoOikeusRyhma().getId();

        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.9"), "4.5.6.7.8")
                        .tehtavanimike("testaaja"),
                kayttoOikeusRyhma("RYHMA2")
                        .withOikeus(oikeus("KOODISTO", "WRITE")))
                .voimassaAlkaen(LocalDate.now().minusMonths(2))
                .voimassaPaattyen(LocalDate.now().minusMonths(1)));

        populate(organisaatioHenkilo(henkilo("123.123.123.123"), "111.111.111.111"));
        List<Long> tapahtumas = myonnettyKayttoOikeusRyhmaTapahtumaRepository.findMasterIdsByHenkilo("1.2.3.4.5");
        assertEquals(2, tapahtumas.size());
        assertTrue(tapahtumas.contains(id));
        assertTrue(tapahtumas.contains(id2));

        tapahtumas = myonnettyKayttoOikeusRyhmaTapahtumaRepository.findMasterIdsByHenkilo("1.2.3.4.7");
        assertEquals(1, tapahtumas.size());
        assertEquals(id3, tapahtumas.get(0));

        tapahtumas = myonnettyKayttoOikeusRyhmaTapahtumaRepository.findMasterIdsByHenkilo("1.2.3.4.9");
        assertEquals(0, tapahtumas.size());
    }

    @Test
    public void findByHenkiloInOrganisaatioTest() throws Exception {
        LocalDate voimassaPaattyen = LocalDate.now().plusMonths(2);

        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA").withNimi(text("FI", "Kuvaus"))
                        .withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))
                        .withOikeus(oikeus("KOODISTO", "READ")))
                .voimassaPaattyen(voimassaPaattyen));

        List<MyonnettyKayttoOikeusDto> list = myonnettyKayttoOikeusRyhmaTapahtumaRepository.findByHenkiloInOrganisaatio("1.2.3.4.5", "3.4.5.6.7");
        assertEquals(1, list.size());
        assertEquals("3.4.5.6.7", list.get(0).getOrganisaatioOid());
        assertEquals(KayttoOikeudenTila.MYONNETTY, list.get(0).getTila());
        assertEquals(voimassaPaattyen, list.get(0).getVoimassaPvm());
        assertEquals("1.2.3.4.5", list.get(0).getKasittelijaOid());

        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "4.5.6.7.8").tehtavanimike("testaaja"),
                kayttoOikeusRyhma("RYHMA2")
                        .withOikeus(oikeus("KOODISTO", "WRITE")))
                .voimassaPaattyen(LocalDate.now().plusMonths(3)));

        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "4.5.6.7.9").tehtavanimike("testaaja"),
                kayttoOikeusRyhma("RYHMA2")
                        .withOikeus(oikeus("KOODISTO", "WRITE"))
                        .asPassivoitu())
                .voimassaPaattyen(LocalDate.now().plusMonths(3)));

        list = myonnettyKayttoOikeusRyhmaTapahtumaRepository.findByHenkiloInOrganisaatio("1.2.3.4.5", null);
        assertEquals(3, list.size());

        list = myonnettyKayttoOikeusRyhmaTapahtumaRepository.findByHenkiloInOrganisaatio("1.2.3.4.5", "3.4.5.6.7");
        assertEquals(1, list.size());
    }

    @Test
    public void findValidAccessRightsByOidTest() throws Exception {
        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA")
                        .withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))
                        .withOikeus(oikeus("KOODISTO", "READ")))
                .voimassaPaattyen(LocalDate.now().plusMonths(2)));

        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "4.5.6.7.8"),
                kayttoOikeusRyhma("RYHMA2")
                        .withOikeus(oikeus("KOODISTO", "WRITE")))
                .voimassaPaattyen(LocalDate.now().plusMonths(3)));

        //should not find these
        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "4.5.6.7.10").passivoitu(),
                kayttoOikeusRyhma("PASSIVOITU").withOikeus(oikeus("KOODISTO", "WRITE")))
                .voimassaPaattyen(LocalDate.now().plusMonths(3)));
        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "4.5.6.7.11"),
                kayttoOikeusRyhma("EIVOIMASSA").withOikeus(oikeus("KOODISTO", "WRITE")))
                .voimassaPaattyen(LocalDate.now().minusDays(3)));
        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "4.5.6.7.12"),
                kayttoOikeusRyhma("EIVOIMASSA2").withOikeus(oikeus("KOODISTO", "WRITE")))
                .voimassaAlkaen(LocalDate.now().plusDays(3)));

        assertEquals(0, myonnettyKayttoOikeusRyhmaTapahtumaRepository.findValidAccessRightsByOid("1.2.madeup.4").size());
        List<AccessRightTypeDto> list = myonnettyKayttoOikeusRyhmaTapahtumaRepository.findValidAccessRightsByOid("1.2.3.4.5");
        assertEquals(3, list.size());
        assertThat(list).extracting(AccessRightTypeDto::getPalvelu).containsExactlyInAnyOrder(PALVELU_HENKILONHALLINTA, "KOODISTO", "KOODISTO");
        assertThat(list).extracting(AccessRightTypeDto::getRooli).containsExactlyInAnyOrder(ROLE_CRUD, "READ", "WRITE");
        assertThat(list).extracting(AccessRightTypeDto::getOrganisaatioOid).containsExactlyInAnyOrder("3.4.5.6.7", "3.4.5.6.7", "4.5.6.7.8");
    }

    @Test
    public void findValidGroupsByHenkiloTest() throws Exception {
        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA")
                        .withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))
                        .withOikeus(oikeus("KOODISTO", "READ")))
                .voimassaPaattyen(LocalDate.now().plusMonths(2)));

        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "4.5.6.7.8"),
                kayttoOikeusRyhma("RYHMA2")
                        .withOikeus(oikeus("KOODISTO", "WRITE")))
                .voimassaPaattyen(LocalDate.now().plusMonths(3)));

        //should not find these
        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "4.5.6.7.10").passivoitu(),
                kayttoOikeusRyhma("PASSIVOITU").withOikeus(oikeus("KOODISTO", "WRITE")))
                .voimassaPaattyen(LocalDate.now().plusMonths(3)));
        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "4.5.6.7.11"),
                kayttoOikeusRyhma("EIVOIMASSA").withOikeus(oikeus("KOODISTO", "WRITE")))
                .voimassaPaattyen(LocalDate.now().minusDays(3)));
        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "4.5.6.7.12"),
                kayttoOikeusRyhma("EIVOIMASSA2").withOikeus(oikeus("KOODISTO", "WRITE")))
                .voimassaAlkaen(LocalDate.now().plusDays(3)));
        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.6"), "4.5.6.7.9"),
                kayttoOikeusRyhma("PASSIVOITU").withOikeus(oikeus("KOODISTO", "WRITE")))
                .voimassaPaattyen(LocalDate.now().plusMonths(3)));

        List<GroupTypeDto> list = this.myonnettyKayttoOikeusRyhmaTapahtumaRepository.findValidGroupsByHenkilo("1.2.3.4.5");
        assertEquals(2, list.size());
        assertThat(list).extracting(GroupTypeDto::getNimi).containsExactlyInAnyOrder("RYHMA", "RYHMA2");
        assertThat(list).extracting(GroupTypeDto::getOrganisaatioOid).containsExactlyInAnyOrder("3.4.5.6.7", "4.5.6.7.8");
    }

    @Test
    public void listCurrentKayttooikeusForHenkilo() {
        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA")
                        .withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))
                        .withOikeus(oikeus("KOODISTO", "READ")))
                .voimassaPaattyen(LocalDate.now().plusMonths(2)));
        List<KayttooikeusPerustiedotDto> kayttooikeusOrganisaatiotDtoList
                = this.myonnettyKayttoOikeusRyhmaTapahtumaRepository.listCurrentKayttooikeusForHenkilo(KayttooikeusCriteria.builder()
                .oidHenkilo("1.2.3.4.5").build(), 1000L, 0L);
        assertThat(kayttooikeusOrganisaatiotDtoList)
                .flatExtracting(KayttooikeusPerustiedotDto::getOrganisaatiot)
                .extracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto::getOrganisaatioOid)
                .containsExactly("3.4.5.6.7");
        assertThat(kayttooikeusOrganisaatiotDtoList)
                .flatExtracting(KayttooikeusPerustiedotDto::getOrganisaatiot)
                .flatExtracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto::getKayttooikeudet)
                .extracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto.KayttooikeusOikeudetDto::getOikeus)
                .containsExactlyInAnyOrder("READ", ROLE_CRUD);
        assertThat(kayttooikeusOrganisaatiotDtoList)
                .flatExtracting(KayttooikeusPerustiedotDto::getOrganisaatiot)
                .flatExtracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto::getKayttooikeudet)
                .extracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto.KayttooikeusOikeudetDto::getPalvelu)
                .containsExactlyInAnyOrder("KOODISTO", PALVELU_HENKILONHALLINTA);
    }

    @Test
    public void listCurrentKayttooikeusForHenkiloByUsername() {
        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5").withUsername("username"), "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA")
                        .withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))
                        .withOikeus(oikeus("KOODISTO", "READ")))
                .voimassaPaattyen(LocalDate.now().plusMonths(2)));
        List<KayttooikeusPerustiedotDto> kayttooikeusOrganisaatiotDtoList
                = this.myonnettyKayttoOikeusRyhmaTapahtumaRepository.listCurrentKayttooikeusForHenkilo(KayttooikeusCriteria.builder()
                .username("username").build(), 1000L, 0L);
        assertThat(kayttooikeusOrganisaatiotDtoList)
                .flatExtracting(KayttooikeusPerustiedotDto::getOrganisaatiot)
                .extracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto::getOrganisaatioOid)
                .containsExactly("3.4.5.6.7");
        assertThat(kayttooikeusOrganisaatiotDtoList)
                .flatExtracting(KayttooikeusPerustiedotDto::getOrganisaatiot)
                .flatExtracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto::getKayttooikeudet)
                .extracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto.KayttooikeusOikeudetDto::getOikeus)
                .containsExactlyInAnyOrder("READ", ROLE_CRUD);
        assertThat(kayttooikeusOrganisaatiotDtoList)
                .flatExtracting(KayttooikeusPerustiedotDto::getOrganisaatiot)
                .flatExtracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto::getKayttooikeudet)
                .extracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto.KayttooikeusOikeudetDto::getPalvelu)
                .containsExactlyInAnyOrder("KOODISTO", PALVELU_HENKILONHALLINTA);
    }

    @Test
    public void listCurrentKayttooikeusForHenkiloByHetu() {
        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5").withHetu("hetu"), "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA")
                        .withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))
                        .withOikeus(oikeus("KOODISTO", "READ")))
                .voimassaPaattyen(LocalDate.now().plusMonths(2)));
        List<KayttooikeusPerustiedotDto> kayttooikeusOrganisaatiotDtoList
                = this.myonnettyKayttoOikeusRyhmaTapahtumaRepository.listCurrentKayttooikeusForHenkilo(KayttooikeusCriteria.builder()
                .hetu("hetu").build(), 1000L, 0L);
        assertThat(kayttooikeusOrganisaatiotDtoList)
                .flatExtracting(KayttooikeusPerustiedotDto::getOrganisaatiot)
                .extracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto::getOrganisaatioOid)
                .containsExactly("3.4.5.6.7");
        assertThat(kayttooikeusOrganisaatiotDtoList)
                .flatExtracting(KayttooikeusPerustiedotDto::getOrganisaatiot)
                .flatExtracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto::getKayttooikeudet)
                .extracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto.KayttooikeusOikeudetDto::getOikeus)
                .containsExactlyInAnyOrder("READ", ROLE_CRUD);
        assertThat(kayttooikeusOrganisaatiotDtoList)
                .flatExtracting(KayttooikeusPerustiedotDto::getOrganisaatiot)
                .flatExtracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto::getKayttooikeudet)
                .extracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto.KayttooikeusOikeudetDto::getPalvelu)
                .containsExactlyInAnyOrder("KOODISTO", PALVELU_HENKILONHALLINTA);
    }

    // User has two MyonnettyKayttooikeusRyhmas for same organisation with separate Kayttooikeusryhmas which have same Kayttooikeus
    @Test
    public void listCurrentKayttooikeusForHenkiloByPalveluNameAndGroup() {
        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA")
                        .withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))
                        .withOikeus(oikeus("KOODISTO", "READ")))
                .voimassaPaattyen(LocalDate.now().plusMonths(2)));
        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA2")
                        .withOikeus(oikeus(PALVELU_HENKILONHALLINTA, ROLE_CRUD))
                        .withOikeus(oikeus("KOODISTO", "READ")))
                .voimassaPaattyen(LocalDate.now().plusMonths(2)));

        List<KayttooikeusPerustiedotDto> kayttooikeusOrganisaatiotDtoList
                = this.myonnettyKayttoOikeusRyhmaTapahtumaRepository.listCurrentKayttooikeusForHenkilo(KayttooikeusCriteria.builder()
                .palvelu(Sets.newHashSet("KOODISTO")).build(), 1000L, 0L);
        assertThat(kayttooikeusOrganisaatiotDtoList)
                .extracting(KayttooikeusPerustiedotDto::getOidHenkilo)
                .containsExactly("1.2.3.4.5");
        assertThat(kayttooikeusOrganisaatiotDtoList)
                .flatExtracting(KayttooikeusPerustiedotDto::getOrganisaatiot)
                .extracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto::getOrganisaatioOid)
                .containsExactly("3.4.5.6.7");
        assertThat(kayttooikeusOrganisaatiotDtoList)
                .flatExtracting(KayttooikeusPerustiedotDto::getOrganisaatiot)
                .flatExtracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto::getKayttooikeudet)
                .extracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto.KayttooikeusOikeudetDto::getOikeus)
                .containsExactlyInAnyOrder("READ", ROLE_CRUD);
        assertThat(kayttooikeusOrganisaatiotDtoList)
                .flatExtracting(KayttooikeusPerustiedotDto::getOrganisaatiot)
                .flatExtracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto::getKayttooikeudet)
                .extracting(KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto.KayttooikeusOikeudetDto::getPalvelu)
                .containsExactlyInAnyOrder("KOODISTO", PALVELU_HENKILONHALLINTA);
    }


}
