package fi.vm.sade.kayttooikeus.service.it;

import fi.vm.sade.kayttooikeus.controller.KutsuPopulator;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static fi.vm.sade.kayttooikeus.model.Identification.HAKA_AUTHENTICATION_IDP;
import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.henkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.virkailija;
import static fi.vm.sade.kayttooikeus.repositories.populate.IdentificationPopulator.identification;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttajatiedotPopulator.kayttajatiedot;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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

        IdentifiedHenkiloTypeDto dto = identificationService.findByTokenAndInvalidateToken("12345");
        assertEquals("1.2.3.4.5", dto.getOidHenkilo());
        assertEquals(KayttajaTyyppi.VIRKAILIJA, dto.getHenkiloTyyppi());
        assertEquals("hakakäyttäjä", dto.getKayttajatiedot().getUsername());
        assertEquals("haka", dto.getIdpEntityId());
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
    public void updateHakatunnuksetByHenkiloAndIdp() {
        String oid = "1.2.3.4.5";
        populate(identification("email", "test@example.com", henkilo(oid)));

        assertThat(identificationService.updateTunnisteetByHenkiloAndIdp(HAKA_AUTHENTICATION_IDP, oid, Stream.of("tunniste1", "tunniste2").collect(toSet())))
                .containsExactlyInAnyOrder("tunniste1", "tunniste2");
        assertThat(identificationService.getTunnisteetByHenkiloAndIdp(HAKA_AUTHENTICATION_IDP, oid)).containsExactlyInAnyOrder("tunniste1", "tunniste2");
        assertThat(identificationService.updateTunnisteetByHenkiloAndIdp(HAKA_AUTHENTICATION_IDP, oid, Stream.of("tunniste2", "tunniste3").collect(toSet())))
                .containsExactlyInAnyOrder("tunniste2", "tunniste3");
        assertThat(identificationService.getTunnisteetByHenkiloAndIdp(HAKA_AUTHENTICATION_IDP, oid)).containsExactlyInAnyOrder("tunniste2", "tunniste3");

        assertThat(identificationRepository.findByHenkiloOidHenkiloAndIdpEntityId(oid, "email"))
                .extracting(Identification::getIdentifier)
                .containsExactly("test@example.com");
    }

    @Test
    public void updateMpassidTunnus() {
        String oid = "1.2.3.4.5";
        populate(identification("email", "test@example.com", henkilo(oid)));

        identificationService.updateTunnisteetByHenkiloAndIdp("mpassid", oid, Set.of("MPASSOID.391ea8eb34f1e27024ab1342603537bdbd494900"));
        assertThat(identificationService.getTunnisteetByHenkiloAndIdp("mpassid", oid))
                .containsExactlyInAnyOrder("MPASSOID.391ea8eb34f1e27024ab1342603537bdbd494900");
    }
    @Test
    public void bothHakaAndMpassidTunnusCanCoexist() {
        String oid = "1.2.3.4.5";
        populate(identification("email", "test@example.com", henkilo(oid)));

        identificationService.updateTunnisteetByHenkiloAndIdp("mpassid", oid, Set.of("MPASSOID.391ea8eb34f1e27024ab1342603537bdbd494900"));
        identificationService.updateTunnisteetByHenkiloAndIdp("haka", oid, Set.of("user@yliopisto.fi"));

        assertThat(identificationService.getTunnisteetByHenkiloAndIdp("mpassid", oid)).containsExactlyInAnyOrder("MPASSOID.391ea8eb34f1e27024ab1342603537bdbd494900");
        assertThat(identificationService.getTunnisteetByHenkiloAndIdp("haka", oid)).containsExactlyInAnyOrder("user@yliopisto.fi");

        identificationService.updateTunnisteetByHenkiloAndIdp("mpassid", oid, Set.of());
        assertThat(identificationService.getTunnisteetByHenkiloAndIdp("mpassid", oid)).isEmpty();
        assertThat(identificationService.getTunnisteetByHenkiloAndIdp("haka", oid)).containsExactlyInAnyOrder("user@yliopisto.fi");
    }


    @Test
    public void updateKutsuAndGenerateTemporaryKutsuToken() {
        Kutsu kutsu = populate(KutsuPopulator.kutsu("arpa", "kuutio", "arpa@kuutio.fi").salaisuus("123"));
        String temporaryToken = this.identificationService
                .updateKutsuAndGenerateTemporaryKutsuToken("123", "hetu", "arpa arpa2", "kuutio").get();
        assertThat(kutsu.getEtunimi()).isEqualTo("arpa arpa2");
        assertThat(kutsu.getSukunimi()).isEqualTo("kuutio");
        assertThat(kutsu.getSahkoposti()).isEqualTo("arpa@kuutio.fi");
        assertThat(kutsu.getKieliKoodi()).isEqualTo("fi");
        assertThat(kutsu.getHetu()).isEqualTo("hetu");
        assertThat(kutsu.getTemporaryToken()).isEqualTo(temporaryToken);
        assertThat(kutsu.getTemporaryTokenCreated()).isBeforeOrEqualTo(LocalDateTime.now());
    }
}
