package fi.vm.sade.kayttooikeus.service.it;

import fi.vm.sade.kayttooikeus.dto.AccessRightTypeDto;
import fi.vm.sade.kayttooikeus.dto.GroupTypeDto;
import fi.vm.sade.kayttooikeus.dto.HenkiloTyyppi;
import fi.vm.sade.kayttooikeus.dto.IdentifiedHenkiloTypeDto;
import fi.vm.sade.kayttooikeus.dto.YhteystietojenTyypit;
import fi.vm.sade.kayttooikeus.model.Identification;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;
import fi.vm.sade.kayttooikeus.repositories.IdentificationRepository;
import fi.vm.sade.kayttooikeus.service.IdentificationService;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkilonYhteystiedotViewDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystiedotDto;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.henkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.IdentificationPopulator.identification;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttajatiedotPopulator.kayttajatiedot;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusPopulator.oikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloKayttoOikeusPopulator.myonnettyKayttoOikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloPopulator.organisaatioHenkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.PalveluPopulator.palvelu;
import static java.util.stream.Collectors.toList;
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
        Optional<Identification> identification = identificationRepository.findByAuthtoken(token);
        assertTrue(identification.isPresent());
        assertEquals("identifier", identification.get().getIdentifier());
        assertEquals("key", identification.get().getIdpEntityId());
        assertEquals("1.2.3.4.5", identification.get().getHenkilo().getOidHenkilo());

        // expiration date should be set for haka
        token = identificationService.generateAuthTokenForHenkilo("1.2.3.4.6", "haka", "hakaidentifier");
        assertTrue(token.length() > 20);
        identification = identificationRepository.findByAuthtoken(token);
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
                henkilo("1.2.3.4.5")).withAuthToken("12345"));

        Kayttajatiedot kayttajatiedot = new Kayttajatiedot();
        kayttajatiedot.setHenkilo(identification.getHenkilo());
        kayttajatiedot.setUsername("hakakäyttäjä");
        identification.getHenkilo().setKayttajatiedot(kayttajatiedot);

        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"),
                kayttoOikeusRyhma("RYHMA")
                        .withOikeus(oikeus("HENKILOHALLINTA", "CRUD"))
                        .withOikeus(oikeus(palvelu("KOODISTO"), "READ"))));

        populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "4.5.6.7.8"),
                kayttoOikeusRyhma("RYHMA2").withOikeus(oikeus("KOODISTO", "CRUD"))));

        given(oppijanumerorekisteriClient.getPerustietoByOid("1.2.3.4.5"))
                .willReturn(HenkiloPerustietoDto.builder()
                        .etunimet("Teemu")
                        .kutsumanimi("Teemu")
                        .sukunimi("Testi")
                        .hetu("11111-1111")
                        .sukupuoli("1")
                        .build());

        HenkilonYhteystiedotViewDto tiedot = new HenkilonYhteystiedotViewDto();
        YhteystiedotDto yhteystiedotDto = new YhteystiedotDto();
        yhteystiedotDto.setSahkoposti("test@test.com");
        tiedot.put(YhteystietojenTyypit.TYOOSOITE, yhteystiedotDto);
        given(oppijanumerorekisteriClient.getYhteystiedotByOid("1.2.3.4.5")).willReturn(tiedot);

        IdentifiedHenkiloTypeDto dto = identificationService.findByTokenAndInvalidateToken("12345");
        assertEquals("1.2.3.4.5", dto.getOidHenkilo());
        assertEquals(HenkiloTyyppi.VIRKAILIJA, dto.getHenkiloTyyppi());
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
        given(oppijanumerorekisteriClient.getPerustietoByOid("1.2.3.4.5"))
                .willReturn(HenkiloPerustietoDto.builder()
                        .etunimet("Teemu")
                        .kutsumanimi("Teemu")
                        .sukunimi("Testi")
                        .hetu("11111-1111")
                        .sukupuoli("1")
                        .build());

        HenkilonYhteystiedotViewDto tiedot = new HenkilonYhteystiedotViewDto();
        YhteystiedotDto yhteystiedotDto = new YhteystiedotDto();
        yhteystiedotDto.setSahkoposti("test@test.com");
        tiedot.put(YhteystietojenTyypit.TYOOSOITE, yhteystiedotDto);
        given(oppijanumerorekisteriClient.getYhteystiedotByOid("1.2.3.4.5")).willReturn(tiedot);

        populate(identification("haka", "identifier", henkilo("1.2.3.4.5")).withAuthToken("12345"));
        identificationService.findByTokenAndInvalidateToken("12345");
        identificationService.findByTokenAndInvalidateToken("12345");
    }

    @Test
    public void updateIdentificationAndGenerateTokenForHenkiloByHetuNotFoundTest() throws Exception {
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("henkilo not found");
        identificationService.updateIdentificationAndGenerateTokenForHenkiloByHetu("1.2.3");
    }

    @Test
    public void updateIdentificationAndGenerateTokenForHenkiloByHetuTest() throws Exception {
        populate(kayttajatiedot(henkilo("1.2.3.4.5"), "user1"));

        given(oppijanumerorekisteriClient.getOidByHetu("090689-1393")).willReturn("1.2.3.4.5");
        //create new
        String token = identificationService.updateIdentificationAndGenerateTokenForHenkiloByHetu("090689-1393");
        assertTrue(token.length() > 20);
        Optional<Identification> identification = identificationRepository.findByAuthtoken(token);
        assertTrue(identification.isPresent());
        assertEquals("vetuma", identification.get().getIdpEntityId());
        assertEquals("user1", identification.get().getIdentifier());
        Long id = identification.get().getId();

        //update old
        token = identificationService.updateIdentificationAndGenerateTokenForHenkiloByHetu("090689-1393");
        assertTrue(token.length() > 20);
        identification = identificationRepository.findByAuthtoken(token);
        assertTrue(identification.isPresent());
        assertEquals("vetuma", identification.get().getIdpEntityId());
        assertEquals("user1", identification.get().getIdentifier());
        assertEquals(id, identification.get().getId());
    }
}
