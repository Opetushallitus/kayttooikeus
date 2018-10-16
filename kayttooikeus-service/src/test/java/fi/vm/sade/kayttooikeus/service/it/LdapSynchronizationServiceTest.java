package fi.vm.sade.kayttooikeus.service.it;

import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;
import fi.vm.sade.kayttooikeus.model.MyonnettyKayttoOikeusRyhmaTapahtuma;
import fi.vm.sade.kayttooikeus.repositories.KayttajaRepository;
import fi.vm.sade.kayttooikeus.repositories.LdapSynchronizationDataRepository;
import fi.vm.sade.kayttooikeus.repositories.LdapUpdateDataRepository;
import fi.vm.sade.kayttooikeus.repositories.RyhmaRepository;
import fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService;
import fi.vm.sade.kayttooikeus.service.TimeService;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import fi.vm.sade.oppijanumerorekisteri.dto.KielisyysDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.stream.Stream;

import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.henkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.virkailija;
import static fi.vm.sade.kayttooikeus.repositories.populate.IdentificationPopulator.identification;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttajatiedotPopulator.kayttajatiedot;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusPopulator.oikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloKayttoOikeusPopulator.myonnettyKayttoOikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloPopulator.organisaatioHenkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.PalveluPopulator.palvelu;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class LdapSynchronizationServiceTest extends AbstractServiceIntegrationTest {

    @Autowired
    private LdapSynchronizationService ldapSynchronizationService;
    @Autowired
    private KayttajaRepository kayttajaRepository;
    @Autowired
    private RyhmaRepository ryhmaRepository;
    @Autowired
    private LdapUpdateDataRepository ldapUpdateDataRepository;
    @Autowired
    private LdapSynchronizationDataRepository ldapSynchronizationDataRepository;

    @MockBean
    private TimeService timeService;
    @MockBean
    private OppijanumerorekisteriClient oppijanumerorekisteriClientMock;

    private HenkiloDto createValidHenkiloDto(String oid) {
        HenkiloDto henkiloDto = new HenkiloDto();
        henkiloDto.setOidHenkilo(oid);
        henkiloDto.setEtunimet("teppo");
        henkiloDto.setKutsumanimi("teppo");
        henkiloDto.setSukunimi("testaaja");
        return henkiloDto;
    }

    private MyonnettyKayttoOikeusRyhmaTapahtuma populateHenkiloWithKayttoOikeus(String oid,
            String kayttajatunnus, String organisaatioOid,
            String kayttoOikeusRyhma, String palvelu, String rooli) {
        HenkiloDto henkiloDto = createValidHenkiloDto(oid);
        when(oppijanumerorekisteriClientMock.findHenkiloByOid(any())).thenReturn(Optional.of(henkiloDto));
        HenkiloPopulator henkiloPopulator = henkilo(oid);
        populate(kayttajatiedot(henkiloPopulator, kayttajatunnus));
        return populate(myonnettyKayttoOikeus(organisaatioHenkilo(
                henkiloPopulator, organisaatioOid),
                kayttoOikeusRyhma(kayttoOikeusRyhma)
                        .withOikeus(oikeus(palvelu(palvelu), rooli)))
                .voimassaAlkaen(LocalDate.now().minusMonths(1))
                .voimassaPaattyen(LocalDate.now().plusMonths(1)));
    }

    private void setDayTime() {
        when(timeService.getDateTimeNow()).thenReturn(LocalDate.now()
                .atTime(LocalTime.of(12, 38)));
    }

    private void setNightTime() {
        when(timeService.getDateTimeNow()).thenReturn(LocalDate.now()
                .atTime(LocalTime.of(2, 5)));
    }

    @Before
    public void setup() {
        // ajetaan oletuksella testit päiväasetuksella
        setDayTime();
        cleanupLdap();
    }

    private void cleanupLdap() {
        kayttajaRepository.deleteAll();
        ryhmaRepository.deleteAll();
    }

    @Test
    public void updateHenkiloKayttajatiedot() {
        populate(kayttajatiedot(henkilo("oid1"), "user1"));
        HenkiloDto henkilo1Dto = createValidHenkiloDto("oid1");
        when(oppijanumerorekisteriClientMock.findHenkiloByOid(eq("oid1"))).thenReturn(Optional.of(henkilo1Dto));
        populate(kayttajatiedot(henkilo("oid2"), "user2"));
        HenkiloDto henkilo2Dto = createValidHenkiloDto("oid2");
        when(oppijanumerorekisteriClientMock.findHenkiloByOid(eq("oid2"))).thenReturn(Optional.of(henkilo2Dto));

        ldapSynchronizationService.updateHenkilo("oid1");
        ldapSynchronizationService.updateHenkilo("oid2");
        ldapSynchronizationService.runSynchronizer();

        assertThat(kayttajaRepository.findAll())
                .extracting("kayttajatunnus", "oid", "etunimet", "kutsumanimi", "sukunimi", "roolit")
                .containsExactly(
                        tuple("user1", "oid1", "teppo", "teppo", "testaaja", "[\"USER_user1\"]"),
                        tuple("user2", "oid2", "teppo", "teppo", "testaaja", "[\"USER_user2\"]")
                );
        assertThat(ryhmaRepository.findAll()).isEmpty();
    }

    @Test
    public void updateHenkiloKayttajatunnus() {
        Kayttajatiedot kayttajatiedot1 = populate(kayttajatiedot(henkilo("oid1"), "user1"));
        HenkiloDto henkilo1Dto = createValidHenkiloDto("oid1");
        when(oppijanumerorekisteriClientMock.findHenkiloByOid(eq("oid1"))).thenReturn(Optional.of(henkilo1Dto));

        ldapSynchronizationService.updateHenkilo("oid1");
        ldapSynchronizationService.runSynchronizer();

        assertThat(kayttajaRepository.findAll())
                .extracting("kayttajatunnus", "oid")
                .containsExactly(tuple("user1", "oid1"));

        kayttajatiedot1.setUsername("user1-updated");

        ldapSynchronizationService.updateHenkilo("oid1");
        ldapSynchronizationService.runSynchronizer();

        assertThat(kayttajaRepository.findAll())
                .extracting("kayttajatunnus", "oid")
                .containsExactly(tuple("user1", "oid1"), tuple("user1-updated", "oid1"));

        Kayttajatiedot kayttajatiedot2 = populate(kayttajatiedot(henkilo("oid2"), "user1"));
        HenkiloDto henkilo2Dto = createValidHenkiloDto("oid2");
        when(oppijanumerorekisteriClientMock.findHenkiloByOid(eq("oid2"))).thenReturn(Optional.of(henkilo2Dto));

        ldapSynchronizationService.updateHenkilo("oid2");
        ldapSynchronizationService.runSynchronizer();

        assertThat(kayttajaRepository.findAll())
                .extracting("kayttajatunnus", "oid")
                .containsExactly(tuple("user1", "oid2"), tuple("user1-updated", "oid1"));
    }

    @Test
    public void updateHenkiloAsiointikieli() {
        populate(kayttajatiedot(henkilo("oid1"), "user1"));
        HenkiloDto henkiloDto = createValidHenkiloDto("oid1");
        henkiloDto.setAsiointiKieli(new KielisyysDto("fi", "suomi"));
        when(oppijanumerorekisteriClientMock.findHenkiloByOid(any())).thenReturn(Optional.of(henkiloDto));

        ldapSynchronizationService.updateHenkilo("oid1");
        ldapSynchronizationService.runSynchronizer();

        assertThat(kayttajaRepository.findAll())
                .extracting("kayttajatunnus", "kieliKoodi")
                .containsExactly(tuple("user1", "fi"));
        assertThat(ryhmaRepository.findAll()).extracting("nimi").containsExactly("LANG_fi");
    }

    @Test
    public void updateHenkiloKayttajatunnusAndAsiointikieli() {
        Kayttajatiedot kayttajatiedot = populate(kayttajatiedot(henkilo("oid1"), "user1"));
        HenkiloDto henkiloDto = createValidHenkiloDto("oid1");
        henkiloDto.setAsiointiKieli(new KielisyysDto("fi", "suomi"));
        when(oppijanumerorekisteriClientMock.findHenkiloByOid(any())).thenReturn(Optional.of(henkiloDto));

        ldapSynchronizationService.updateHenkilo("oid1");
        ldapSynchronizationService.runSynchronizer();

        assertThat(kayttajaRepository.findAll())
                .extracting("kayttajatunnus", "kieliKoodi")
                .containsExactly(tuple("user1", "fi"));
        assertThat(ryhmaRepository.findAll()).extracting("nimi").containsExactly("LANG_fi");

        kayttajatiedot.setUsername("user1-updated");

        ldapSynchronizationService.updateHenkilo("oid1");
        ldapSynchronizationService.runSynchronizer();

        assertThat(kayttajaRepository.findAll())
                .extracting("kayttajatunnus", "oid")
                .containsExactlyInAnyOrder(tuple("user1", "oid1"), tuple("user1-updated", "oid1"));
        assertThat(ryhmaRepository.findAll())
                .extracting("nimi", "kayttajat")
                .containsExactly(tuple("LANG_fi", Stream.of("uid=user1,ou=People,dc=opintopolku,dc=fi", "uid=user1-updated,ou=People,dc=opintopolku,dc=fi").collect(toSet())));
    }

    @Test
    public void updateHenkiloTyyppi() {
        populate(kayttajatiedot(virkailija("oid1"), "user1"));
        HenkiloDto henkiloDto = createValidHenkiloDto("oid1");
        when(oppijanumerorekisteriClientMock.findHenkiloByOid(any())).thenReturn(Optional.of(henkiloDto));

        ldapSynchronizationService.updateHenkilo("oid1");
        ldapSynchronizationService.runSynchronizer();

        assertThat(kayttajaRepository.findAll())
                .extracting("kayttajatunnus", "oid")
                .containsExactly(tuple("user1", "oid1"));
        assertThat(ryhmaRepository.findAll()).extracting("nimi").containsExactly("VIRKAILIJA");
    }

    @Test
    public void updateHenkiloKayttoOikeusRyhma() {
        populateHenkiloWithKayttoOikeus("oid1", "user1", "organisaatio", "käyttöoikeusryhmä", "palvelu", "rooli");

        ldapSynchronizationService.updateHenkilo("oid1");
        ldapSynchronizationService.runSynchronizer();

        assertThat(kayttajaRepository.findAll())
                .extracting("kayttajatunnus", "oid")
                .containsExactly(tuple("user1", "oid1"));
        assertThat(ryhmaRepository.findAll()).extracting("nimi")
                .containsExactly("APP_palvelu", "APP_palvelu_rooli", "APP_palvelu_rooli_organisaatio");
    }

    @Test
    public void updateHenkiloIdentification() {
        HenkiloPopulator henkiloPopulator = henkilo("oid1");
        populate(kayttajatiedot(henkiloPopulator, "user1"));
        populate(identification("vetuma", "userVetuma", henkiloPopulator));
        HenkiloDto henkiloDto = createValidHenkiloDto("oid1");
        when(oppijanumerorekisteriClientMock.findHenkiloByOid(any())).thenReturn(Optional.of(henkiloDto));

        ldapSynchronizationService.updateHenkilo("oid1");
        ldapSynchronizationService.runSynchronizer();

        assertThat(kayttajaRepository.findAll())
                .extracting("kayttajatunnus", "oid")
                .containsExactly(tuple("user1", "oid1"));
        assertThat(ryhmaRepository.findAll()).extracting("nimi")
                .containsExactly("STRONG_AUTHENTICATED", "STRONG_AUTHENTICATED_VETUMA");
    }

    @Test
    public void updateHenkiloPassivointi() {
        populate(kayttajatiedot(henkilo("oid1"), "user1"));
        HenkiloDto henkiloDto = createValidHenkiloDto("oid1");
        when(oppijanumerorekisteriClientMock.findHenkiloByOid(any())).thenReturn(Optional.of(henkiloDto));

        ldapSynchronizationService.updateHenkilo("oid1");
        ldapSynchronizationService.runSynchronizer();
        assertThat(kayttajaRepository.findAll())
                .extracting("kayttajatunnus", "oid")
                .containsExactly(tuple("user1", "oid1"));

        henkiloDto.setPassivoitu(true);
        ldapSynchronizationService.updateHenkilo("oid1");
        ldapSynchronizationService.runSynchronizer();
        assertThat(kayttajaRepository.findAll()).isEmpty();
        assertThat(ryhmaRepository.findAll()).isEmpty();
    }

    @Test
    public void updateHenkiloRemoveKayttajatiedot() {
        Kayttajatiedot kayttajatiedot = populate(kayttajatiedot(henkilo("oid1"), "user1"));
        HenkiloDto henkiloDto = createValidHenkiloDto("oid1");
        when(oppijanumerorekisteriClientMock.findHenkiloByOid(any())).thenReturn(Optional.of(henkiloDto));

        ldapSynchronizationService.updateHenkilo("oid1");
        ldapSynchronizationService.runSynchronizer();
        assertThat(kayttajaRepository.findAll())
                .extracting("kayttajatunnus", "oid")
                .containsExactly(tuple("user1", "oid1"));

        kayttajatiedot.getHenkilo().setKayttajatiedot(null);
        em.remove(kayttajatiedot);
        ldapSynchronizationService.updateHenkilo("oid1");
        ldapSynchronizationService.runSynchronizer();
        assertThat(kayttajaRepository.findAll()).isEmpty();
        assertThat(ryhmaRepository.findAll()).isEmpty();
    }

    @Test
    public void updateHenkiloDeleteRyhma() {
        populate(kayttajatiedot(henkilo("oid1"), "user1"));
        HenkiloDto henkilo1Dto = createValidHenkiloDto("oid1");
        henkilo1Dto.setAsiointiKieli(new KielisyysDto("fi", "suomi"));
        when(oppijanumerorekisteriClientMock.findHenkiloByOid(eq("oid1"))).thenReturn(Optional.of(henkilo1Dto));
        populate(kayttajatiedot(henkilo("oid2"), "user2"));
        HenkiloDto henkilo2Dto = createValidHenkiloDto("oid2");
        henkilo2Dto.setAsiointiKieli(new KielisyysDto("fi", "suomi"));
        when(oppijanumerorekisteriClientMock.findHenkiloByOid(eq("oid2"))).thenReturn(Optional.of(henkilo2Dto));

        ldapSynchronizationService.updateHenkilo("oid1");
        ldapSynchronizationService.updateHenkilo("oid2");
        ldapSynchronizationService.runSynchronizer();
        assertThat(ryhmaRepository.findAll()).hasSize(1);

        henkilo1Dto.setAsiointiKieli(null);
        ldapSynchronizationService.updateHenkilo("oid1");
        ldapSynchronizationService.runSynchronizer();
        assertThat(ryhmaRepository.findAll()).hasSize(1);

        henkilo2Dto.setAsiointiKieli(null);
        ldapSynchronizationService.updateHenkilo("oid2");
        ldapSynchronizationService.runSynchronizer();
        assertThat(ryhmaRepository.findAll()).isEmpty();
    }

    @Test
    public void updateKayttoOikeusRyhma() {
        MyonnettyKayttoOikeusRyhmaTapahtuma myonnetty = populateHenkiloWithKayttoOikeus("oid1", "user", "organisaatio", "käyttöoikeusryhmä", "palvelu", "rooli");

        ldapSynchronizationService.updateKayttoOikeusRyhma(myonnetty.getKayttoOikeusRyhma().getId());
        ldapSynchronizationService.runSynchronizer();
        assertThat(ldapUpdateDataRepository.findAll())
                .extracting("henkiloOid")
                .containsExactly("oid1");
        ldapSynchronizationService.runSynchronizer();
        assertThat(ldapUpdateDataRepository.findAll()).isEmpty();
        assertThat(ldapSynchronizationDataRepository.findAll())
                .extracting("totalAmount", "runBatch")
                .containsExactly(tuple(1, true), tuple(1, false));
    }

    @Test
    public void updateAll() {
        populate(kayttajatiedot(henkilo("oid1"), "user1"));
        HenkiloDto henkilo1Dto = createValidHenkiloDto("oid1");
        when(oppijanumerorekisteriClientMock.findHenkiloByOid(eq("oid1"))).thenReturn(Optional.of(henkilo1Dto));
        populate(henkilo("oid2"));
        HenkiloDto henkilo2Dto = createValidHenkiloDto("oid2");
        when(oppijanumerorekisteriClientMock.findHenkiloByOid(eq("oid2"))).thenReturn(Optional.of(henkilo2Dto));
        populate(kayttajatiedot(henkilo("oid3"), "user3"));
        HenkiloDto henkilo3Dto = createValidHenkiloDto("oid3");
        when(oppijanumerorekisteriClientMock.findHenkiloByOid(eq("oid3"))).thenReturn(Optional.of(henkilo3Dto));

        ldapSynchronizationService.updateAllAtNight();
        assertThat(ldapUpdateDataRepository.findAll())
                .extracting("henkiloOid")
                .containsExactly("runall");
        ldapSynchronizationService.runSynchronizer();
        assertThat(ldapUpdateDataRepository.findAll())
                .extracting("henkiloOid")
                .containsExactly("runall");
        assertThat(ldapSynchronizationDataRepository.findAll())
                .isEmpty();
        setNightTime();
        ldapSynchronizationService.runSynchronizer();
        assertThat(ldapUpdateDataRepository.findAll())
                .extracting("henkiloOid")
                .containsExactly("oid1", "oid3");
        ldapSynchronizationService.runSynchronizer();
        assertThat(ldapUpdateDataRepository.findAll())
                .isEmpty();
        assertThat(ldapSynchronizationDataRepository.findAll())
                .extracting("totalAmount", "runBatch")
                .containsExactly(tuple(2, true), tuple(2, false));
    }

}
