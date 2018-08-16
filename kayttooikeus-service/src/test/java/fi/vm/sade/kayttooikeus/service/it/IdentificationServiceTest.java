package fi.vm.sade.kayttooikeus.service.it;

import fi.vm.sade.kayttooikeus.controller.KutsuPopulator;
import fi.vm.sade.kayttooikeus.dto.AccessRightTypeDto;
import fi.vm.sade.kayttooikeus.dto.GroupTypeDto;
import fi.vm.sade.kayttooikeus.dto.IdentifiedHenkiloTypeDto;
import fi.vm.sade.kayttooikeus.dto.KayttajaTyyppi;
import fi.vm.sade.kayttooikeus.model.Identification;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;
import fi.vm.sade.kayttooikeus.model.Kutsu;
import fi.vm.sade.kayttooikeus.repositories.IdentificationRepository;
import fi.vm.sade.kayttooikeus.service.IdentificationService;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystiedotRyhmaDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoTyyppi;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
public class IdentificationServiceTest extends AbstractServiceIntegrationTest {

    @Autowired
    private IdentificationService identificationService;

    @Autowired
    private IdentificationRepository identificationRepository;

    @MockBean
    OppijanumerorekisteriClient oppijanumerorekisteriClient;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void generateAuthTokenForHenkiloNotFoundTest() {
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("no henkilo found with oid:[1.1.1.1.1]");
        identificationService.generateAuthTokenForHenkilo("1.1.1.1.1", "key", "identifier");
    }

    @Test
    public void generateAuthTokenForHenkiloTest() {
        populate(henkilo("1.2.3.4.5"));
        populate(henkilo("1.2.3.4.6"));

        String token = identificationService.generateAuthTokenForHenkilo("1.2.3.4.5",
                "key", "identifier");
        assertTrue(token.length() > 20);
        Optional<Identification> identification = identificationRepository.findByAuthtokenIsValid(token);
        assertTrue(identification.isPresent());
        assertEquals("identifier", identification.get().getIdentifier());
        assertEquals("key", identification.get().getIdpEntityId());
        assertEquals("1.2.3.4.5", identification.get().getHenkilo().getOidHenkilo());

        // expiration date should be set for haka
        token = identificationService.generateAuthTokenForHenkilo("1.2.3.4.6", "haka", "hakaidentifier");
        assertTrue(token.length() > 20);
        identification = identificationRepository.findByAuthtokenIsValid(token);
        assertTrue(identification.isPresent());
        assertEquals("hakaidentifier", identification.get().getIdentifier());
        assertEquals("haka", identification.get().getIdpEntityId());
        assertEquals("1.2.3.4.6", identification.get().getHenkilo().getOidHenkilo());
    }

    @Test(expected = NotFoundException.class)
    public void getHenkiloOidByIdpAndIdentifierNotFoundTest() throws Exception {
        identificationService.getHenkiloOidByIdpAndIdentifier("haka", "identifier");
    }

    @Test
    public void getHenkiloOidByIdpAndIdentifierTest() throws Exception {
        populate(identification("haka", "identifier", henkilo("1.2.3.4.5")));
        String oid = identificationService.getHenkiloOidByIdpAndIdentifier("haka", "identifier");
        assertEquals(oid, "1.2.3.4.5");
    }

    @Test
    public void validateAuthTokenTest() throws Exception {
        Identification identification = populate(identification("haka", "identifier",
                virkailija("1.2.3.4.5")).withAuthToken("12345"));

        Kayttajatiedot kayttajatiedot = new Kayttajatiedot();
        kayttajatiedot.setHenkilo(identification.getHenkilo());
        kayttajatiedot.setUsername("hakakäyttäjä");
        identification.getHenkilo().setKayttajatiedot(kayttajatiedot);

        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(virkailija("1.2.3.4.5"), "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA")
                        .withOikeus(oikeus("HENKILOHALLINTA", "CRUD"))
                        .withOikeus(oikeus(palvelu("KOODISTO"), "READ"))));

        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(virkailija("1.2.3.4.5"), "4.5.6.7.8"),
                kayttoOikeusRyhma("RYHMA2").withOikeus(oikeus("KOODISTO", "CRUD"))));

        YhteystietoDto yhteystieto = YhteystietoDto.builder()
                .yhteystietoTyyppi(YhteystietoTyyppi.YHTEYSTIETO_SAHKOPOSTI)
                .yhteystietoArvo("test@test.com")
                .build();
        YhteystiedotRyhmaDto yhteystietoRyhma = YhteystiedotRyhmaDto.builder()
                .yhteystieto(yhteystieto)
                .build();
        given(oppijanumerorekisteriClient.getHenkiloByOid("1.2.3.4.5"))
                .willReturn(HenkiloDto.builder()
                        .etunimet("Teemu")
                        .kutsumanimi("Teemu")
                        .sukunimi("Testi")
                        .hetu("11111-1111")
                        .sukupuoli("1")
                        .passivoitu(false)
                        .yhteystiedotRyhma(Stream.of(yhteystietoRyhma).collect(toSet()))
                        .build());

        IdentifiedHenkiloTypeDto dto = identificationService.findByTokenAndInvalidateToken("12345");
        assertEquals("1.2.3.4.5", dto.getOidHenkilo());
        assertEquals(KayttajaTyyppi.VIRKAILIJA, dto.getHenkiloTyyppi());
        assertEquals("haka", dto.getIdpEntityId());
        assertEquals("identifier", dto.getIdentifier());
        assertEquals("hakakäyttäjä", dto.getKayttajatiedot().getUsername());
        assertFalse(dto.isPassivoitu());
        assertEquals("Teemu", dto.getKutsumanimi());
        assertEquals("Teemu", dto.getEtunimet());
        assertEquals("Testi", dto.getSukunimi());
        assertEquals("11111-1111", dto.getHetu());
        assertEquals("MIES", dto.getSukupuoli());
        assertEquals("test@test.com", dto.getEmail());

        List<AccessRightTypeDto> accessRights = dto.getAuthorizationData().getAccessrights().getAccessRight();
        assertEquals(3, accessRights.size());

        List<AccessRightTypeDto> rights = accessRights.stream().filter(right -> right.getPalvelu().equals("HENKILOHALLINTA")).collect(toList());
        assertEquals(1, rights.size());
        assertEquals("CRUD", rights.get(0).getRooli());
        assertEquals("3.4.5.6.7", rights.get(0).getOrganisaatioOid());
        rights = accessRights.stream().filter(right -> right.getPalvelu().equals("KOODISTO")).collect(toList());
        assertEquals(2, rights.size());
        assertTrue(rights.stream().map(AccessRightTypeDto::getRooli).collect(toList()).containsAll(Arrays.asList("READ", "CRUD")));
        assertTrue(rights.stream().map(AccessRightTypeDto::getOrganisaatioOid).collect(toList()).containsAll(Arrays.asList("3.4.5.6.7", "4.5.6.7.8")));

        List<GroupTypeDto> groups = dto.getAuthorizationData().getGroups().getGroup();
        assertEquals(2, groups.size());
        List<GroupTypeDto> group = groups.stream().filter(groupType -> groupType.getNimi().equals("RYHMA")).collect(toList());
        assertEquals(1, group.size());
        assertEquals("3.4.5.6.7", group.get(0).getOrganisaatioOid());
        group = groups.stream().filter(groupType -> groupType.getNimi().equals("RYHMA2")).collect(toList());
        assertEquals(1, group.size());
        assertEquals("4.5.6.7.8", group.get(0).getOrganisaatioOid());

    }

    @Test(expected = NotFoundException.class)
    public void validateAuthTokenNotFoundTest() {
        identificationService.findByTokenAndInvalidateToken("12345");
    }

    @Test(expected = NotFoundException.class)
    public void validateAuthTokenUsedTest() {
        YhteystietoDto yhteystieto = YhteystietoDto.builder()
                .yhteystietoTyyppi(YhteystietoTyyppi.YHTEYSTIETO_SAHKOPOSTI)
                .yhteystietoArvo("test@test.com")
                .build();
        YhteystiedotRyhmaDto yhteystietoRyhma = YhteystiedotRyhmaDto.builder()
                .yhteystieto(yhteystieto)
                .build();
        given(oppijanumerorekisteriClient.getHenkiloByOid("1.2.3.4.5"))
                .willReturn(HenkiloDto.builder()
                        .etunimet("Teemu")
                        .kutsumanimi("Teemu")
                        .sukunimi("Testi")
                        .hetu("11111-1111")
                        .sukupuoli("1")
                        .passivoitu(false)
                        .yhteystiedotRyhma(Stream.of(yhteystietoRyhma).collect(toSet()))
                        .build());

        populate(identification("haka", "identifier", henkilo("1.2.3.4.5")).withAuthToken("12345"));
        identificationService.findByTokenAndInvalidateToken("12345");
        identificationService.findByTokenAndInvalidateToken("12345");
    }

    @Test
    public void updateIdentificationAndGenerateTokenForHenkiloByHetuNotFoundTest() throws Exception {
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("Henkilo not found with oid 1.2.3");
        identificationService.updateIdentificationAndGenerateTokenForHenkiloByOid("1.2.3");
    }

    @Test
    public void updateIdentificationAndGenerateTokenForHenkiloByOid() {
        String oid = "oid1";
        populate(kayttajatiedot(henkilo(oid), "user1"));

        String token1 = identificationService.updateIdentificationAndGenerateTokenForHenkiloByOid(oid);
        assertThat(identificationRepository.findByHenkiloOidHenkiloAndIdpEntityId(oid, "vetuma"))
                .extracting(Identification::getIdentifier)
                .containsExactly("user1");

        String token2 = identificationService.updateIdentificationAndGenerateTokenForHenkiloByOid(oid);
        assertThat(identificationRepository.findByHenkiloOidHenkiloAndIdpEntityId(oid, "vetuma"))
                .extracting(Identification::getIdentifier)
                .containsExactly("user1");

        assertThat(token2).isNotEqualTo(token1);
    }

    @Test
    public void updateIdentificationAndGenerateTokenForHenkiloByHetuTest() throws Exception {
        populate(kayttajatiedot(henkilo("1.2.3.4.5"), "user1"));

        given(oppijanumerorekisteriClient.getOidByHetu("090689-1393")).willReturn("1.2.3.4.5");
        //create new
        String token = identificationService.updateIdentificationAndGenerateTokenForHenkiloByHetu("090689-1393");
        assertTrue(token.length() > 20);
        Optional<Identification> identification = identificationRepository.findByAuthtokenIsValid(token);
        assertTrue(identification.isPresent());
        assertEquals("vetuma", identification.get().getIdpEntityId());
        assertEquals("user1", identification.get().getIdentifier());
        Long id = identification.get().getId();

        //update old
        token = identificationService.updateIdentificationAndGenerateTokenForHenkiloByHetu("090689-1393");
        assertTrue(token.length() > 20);
        identification = identificationRepository.findByAuthtokenIsValid(token);
        assertTrue(identification.isPresent());
        assertEquals("vetuma", identification.get().getIdpEntityId());
        assertEquals("user1", identification.get().getIdentifier());
        assertEquals(id, identification.get().getId());
    }

    @Test
    public void updateHakatunnuksetByHenkiloAndIdp() {
        String oid = "1.2.3.4.5";
        populate(identification("email", "test@example.com", henkilo(oid)));

        assertThat(identificationService.updateHakatunnuksetByHenkiloAndIdp(oid, Stream.of("tunniste1", "tunniste2").collect(toSet())))
                .containsExactlyInAnyOrder("tunniste1", "tunniste2");
        assertThat(identificationService.getHakatunnuksetByHenkiloAndIdp(oid)).containsExactlyInAnyOrder("tunniste1", "tunniste2");
        assertThat(identificationService.updateHakatunnuksetByHenkiloAndIdp(oid, Stream.of("tunniste2", "tunniste3").collect(toSet())))
                .containsExactlyInAnyOrder("tunniste2", "tunniste3");
        assertThat(identificationService.getHakatunnuksetByHenkiloAndIdp(oid)).containsExactlyInAnyOrder("tunniste2", "tunniste3");

        assertThat(identificationRepository.findByHenkiloOidHenkiloAndIdpEntityId(oid, "email"))
                .extracting(Identification::getIdentifier)
                .containsExactly("test@example.com");
    }

    @Test
    public void updateKutsuAndGenerateTemporaryKutsuToken() {
        Kutsu kutsu = populate(KutsuPopulator.kutsu("arpa", "kuutio", "arpa@kuutio.fi").salaisuus("123"));
        String temporaryToken = this.identificationService
                .updateKutsuAndGenerateTemporaryKutsuToken("123", "hetu", "arpa arpa2", "kuutio");
        assertThat(kutsu.getEtunimi()).isEqualTo("arpa arpa2");
        assertThat(kutsu.getSukunimi()).isEqualTo("kuutio");
        assertThat(kutsu.getSahkoposti()).isEqualTo("arpa@kuutio.fi");
        assertThat(kutsu.getKieliKoodi()).isEqualTo("fi");
        assertThat(kutsu.getHetu()).isEqualTo("hetu");
        assertThat(kutsu.getTemporaryToken()).isEqualTo(temporaryToken);
        assertThat(kutsu.getTemporaryTokenCreated()).isBeforeOrEqualTo(LocalDateTime.now());
    }
}
