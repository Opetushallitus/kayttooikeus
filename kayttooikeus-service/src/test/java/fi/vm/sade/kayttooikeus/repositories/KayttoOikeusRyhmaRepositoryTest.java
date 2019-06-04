package fi.vm.sade.kayttooikeus.repositories;

import com.querydsl.core.Tuple;
import fi.vm.sade.kayttooikeus.dto.KayttoOikeusRyhmaDto;
import fi.vm.sade.kayttooikeus.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.henkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusPopulator.oikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloKayttoOikeusPopulator.myonnettyKayttoOikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloPopulator.organisaatioHenkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.TextGroupPopulator.text;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class KayttoOikeusRyhmaRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private KayttoOikeusRyhmaRepository kayttoOikeusRyhmaRepository;

    @Test
    public void listAllTest() {
        populate(kayttoOikeusRyhma("RYHMÄ")
                .withNimi(text("FI", "ryhmän kuvaus"))
                .withOikeus(oikeus("APP1", "READ"))
                .withOikeus(oikeus("APP2", "WRITE")));

        populate(kayttoOikeusRyhma("Piilotettu ryhmä")
                .asPassivoitu()
                .withOikeus(oikeus("APP1", "READ"))
                .withOikeus(oikeus("APP2", "WRITE")));

        populate(kayttoOikeusRyhma("RYHMÄ2")
                .withNimi(text("FI", "ryhmän 2 kuvaus"))
                .withOikeus(oikeus("APP1", "READ"))
                .withOikeus(oikeus("APP2", "WRITE")));

        List<KayttoOikeusRyhmaDto> ryhmas = kayttoOikeusRyhmaRepository.listAll(false);
        assertEquals(2, ryhmas.size());
        assertEquals("RYHMÄ", ryhmas.get(0).getName());
    }

    @Test
    public void findOrganisaatioOidAndRyhmaIdByHenkiloOidTest() {
        QOrganisaatioHenkilo organisaatioHenkilo = QOrganisaatioHenkilo.organisaatioHenkilo;
        QMyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma
                = QMyonnettyKayttoOikeusRyhmaTapahtuma.myonnettyKayttoOikeusRyhmaTapahtuma;
        MyonnettyKayttoOikeusRyhmaTapahtuma tapahtuma = populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "1.0.0.102.0"),
                kayttoOikeusRyhma("RYHMA").withNimi(text("FI", "Kuvaus"))
                        .withOikeus(oikeus("HENKILOHALLINTA", "READ"))
        ).voimassaAlkaen(LocalDate.now().minusMonths(3))
                .voimassaPaattyen(LocalDate.now().plusMonths(3)));

        List<Tuple> list = this.kayttoOikeusRyhmaRepository.findOrganisaatioOidAndRyhmaIdByHenkiloOid(
                tapahtuma.getOrganisaatioHenkilo().getHenkilo().getOidHenkilo());
        assertEquals(list.get(0).get(organisaatioHenkilo.organisaatioOid),
                tapahtuma.getOrganisaatioHenkilo().getOrganisaatioOid());
        assertEquals(list.get(0).get(myonnettyKayttoOikeusRyhmaTapahtuma.kayttoOikeusRyhma.id),
                tapahtuma.getKayttoOikeusRyhma().getId());
    }


    @Test
    public void findByIdListTest(){
        Long id1 =populate(kayttoOikeusRyhma("RYHMÄ")
                .withOikeus(oikeus("APP1", "READ"))
                .withOikeus(oikeus("APP2", "WRITE"))).getId();

        Long id2 = populate(kayttoOikeusRyhma("RYHMÄ2")
                .withOikeus(oikeus("APP1", "READ"))
                .withOikeus(oikeus("APP2", "WRITE"))).getId();

        Long id3 = populate(kayttoOikeusRyhma("RYHMÄ2")
                .asPassivoitu()
                .withOikeus(oikeus("APP1", "READ"))
                .withOikeus(oikeus("APP2", "WRITE"))).getId();

        List<KayttoOikeusRyhmaDto> ryhmas = kayttoOikeusRyhmaRepository.findByIdList(Arrays.asList(id2));
        assertEquals(1, ryhmas.size());
        assertEquals("RYHMÄ2", ryhmas.get(0).getName());

        ryhmas = kayttoOikeusRyhmaRepository.findByIdList(Arrays.asList());
        assertEquals(0, ryhmas.size());

        ryhmas = kayttoOikeusRyhmaRepository.findByIdList(null);
        assertEquals(0, ryhmas.size());

        ryhmas = kayttoOikeusRyhmaRepository.findByIdList(Arrays.asList(id1, id2));
        assertEquals(2, ryhmas.size());

        ryhmas = kayttoOikeusRyhmaRepository.findByIdList(Arrays.asList(id1, id2, id3));
        assertEquals(2, ryhmas.size());

        ryhmas = kayttoOikeusRyhmaRepository.findByIdList(Arrays.asList(id1, id2, id2));
        assertEquals(2, ryhmas.size());

        ryhmas = kayttoOikeusRyhmaRepository.findByIdList(Arrays.asList(id1, id2, 675467546L));
        assertEquals(2, ryhmas.size());
    }

    @Test
    public void findByIdTest(){
        Long id = populate(kayttoOikeusRyhma("RYHMÄ")
                .withNimi(text("FI", "Kuvaus"))
                .withOrganisaatiorajoite("TYYPPI")
                .withOikeus(oikeus("APP1", "READ"))
                .withOikeus(oikeus("APP2", "WRITE"))).getId();

        Long hiddenRyhmaId = populate(kayttoOikeusRyhma("RYHMÄ2")
                .asPassivoitu()
                .withOikeus(oikeus("APP1", "READ"))
                .withOikeus(oikeus("APP2", "WRITE"))).getId();

        Optional<KayttoOikeusRyhma> ryhma = kayttoOikeusRyhmaRepository.findByRyhmaId(id, false);
        assertTrue(ryhma.isPresent());
        KayttoOikeusRyhma koRyhma = ryhma.get();
        assertEquals("RYHMÄ", koRyhma.getTunniste());
        assertEquals(1, koRyhma.getOrganisaatioViite().size());
        assertEquals("RYHMÄ", koRyhma.getOrganisaatioViite().iterator().next().getKayttoOikeusRyhma().getTunniste());
        assertEquals("Kuvaus", koRyhma.getNimi().getTexts().stream().filter(text -> text.getLang().equals("FI")).findFirst().get().getText());
        assertEquals(2, koRyhma.getKayttoOikeus().size());

        Optional<KayttoOikeusRyhma> hiddenRyhma = kayttoOikeusRyhmaRepository.findById(hiddenRyhmaId);
        assertTrue(hiddenRyhma.isPresent());

        hiddenRyhma = kayttoOikeusRyhmaRepository.findByRyhmaId(hiddenRyhmaId, false);
        assertFalse(hiddenRyhma.isPresent());

        Optional<KayttoOikeusRyhma> nonexistent = kayttoOikeusRyhmaRepository.findByRyhmaId(434323423L, false);
        assertFalse(nonexistent.isPresent());
    }


    @Test
    public void ryhmaNameExistTest(){
        populate(kayttoOikeusRyhma("RYHMÄ")
                .withNimi(text("FI", "Kuvaus")));

        populate(kayttoOikeusRyhma("RYHMÄ")
                .withNimi(text("EN", "Kuvaus en")));

        Boolean found = kayttoOikeusRyhmaRepository.ryhmaNameFiExists("Kuvaus");
        assertTrue(found);

        found = kayttoOikeusRyhmaRepository.ryhmaNameFiExists("Kuvaus en");
        assertFalse(found);

        found = kayttoOikeusRyhmaRepository.ryhmaNameFiExists("madeup");
        assertFalse(found);
    }

    @Test
    public void insertTest(){

        KayttoOikeus oikeus = populate(oikeus("APP1", "READ"));
        List<KayttoOikeusRyhmaDto> ryhmas = kayttoOikeusRyhmaRepository.listAll(false);
        assertEquals(0, ryhmas.size());

        KayttoOikeusRyhma kor = new KayttoOikeusRyhma();
        kor.setTunniste("TEST");
        kor.setPassivoitu(false);
        TextGroup textGroup = new TextGroup();
        textGroup.addText(new Text(textGroup, "FI", "kuvaus"));
        kor.setNimi(textGroup);
        kor.getKayttoOikeus().add(oikeus);
        kor.setRooliRajoite("roolirajoite");
        kor = kayttoOikeusRyhmaRepository.save(kor);

        ryhmas = kayttoOikeusRyhmaRepository.listAll(false);
        assertEquals(1, ryhmas.size());

        KayttoOikeusRyhma ryhma = kayttoOikeusRyhmaRepository.findByRyhmaId(kor.getId(), false).get();
        assertEquals("TEST", ryhma.getTunniste());
        assertEquals("roolirajoite", ryhma.getRooliRajoite());
        assertEquals("kuvaus", ryhma.getNimi().getTexts().stream().findFirst().get().getText());
        assertEquals("APP1", ryhma.getKayttoOikeus().iterator().next().getPalvelu().getName());
        assertEquals("READ", ryhma.getKayttoOikeus().iterator().next().getRooli());
    }

    @Test
    public void findKayttoOikeusRyhmasByKayttoOikeusIdsTest() throws Exception {
        KayttoOikeus oikeus = populate(oikeus("APP", "READ"));
        KayttoOikeus crudOikeus = populate(oikeus("APP", "CRUD"));

        KayttoOikeusRyhma ryhma = populate(kayttoOikeusRyhma("RYHMÄ")
                .withNimi(text("FI", "ryhmän kuvaus"))
                .withOikeus(oikeus("APP2", "WRITE")));
        ryhma.getKayttoOikeus().add(oikeus);
        ryhma.getKayttoOikeus().add(crudOikeus);

        KayttoOikeusRyhma piilotettuRyhma = populate(kayttoOikeusRyhma("Piilotettu ryhmä")
                .asPassivoitu()
                .withOikeus(oikeus("APP3", "READ"))
                .withOikeus(oikeus("APP4", "WRITE")));
        piilotettuRyhma.getKayttoOikeus().add(oikeus);
        piilotettuRyhma.getKayttoOikeus().add(crudOikeus);

        KayttoOikeusRyhma ryhma2 = populate(kayttoOikeusRyhma("CRUDRYHMÄ")
                .withOikeus(oikeus("APP5", "READ"))
                .withOikeus(oikeus("APP6", "WRITE")));
        ryhma2.getKayttoOikeus().add(crudOikeus);

        List<KayttoOikeusRyhmaDto> ryhmat = kayttoOikeusRyhmaRepository.findKayttoOikeusRyhmasByKayttoOikeusIds(
                Collections.singletonList(oikeus.getId()));
        assertEquals(1, ryhmat.size());
        assertEquals("RYHMÄ", ryhmat.get(0).getName());

        ryhmat = kayttoOikeusRyhmaRepository.findKayttoOikeusRyhmasByKayttoOikeusIds(
                Collections.singletonList(crudOikeus.getId()));
        assertEquals(2, ryhmat.size());

        assertTrue(Arrays.asList("RYHMÄ", "CRUDRYHMÄ").containsAll(
                ryhmat.stream().map(KayttoOikeusRyhmaDto::getName).collect(Collectors.toList())));

        ryhma = populate(kayttoOikeusRyhma("UUSRYHMÄ")
                .withOikeus(oikeus("APP7", "WRITE")));
        ryhma.getKayttoOikeus().add(oikeus);

        ryhmat = kayttoOikeusRyhmaRepository.findKayttoOikeusRyhmasByKayttoOikeusIds(Arrays.asList(crudOikeus.getId(), oikeus.getId()));
        assertEquals(3, ryhmat.size());
        assertTrue(Arrays.asList("RYHMÄ", "CRUDRYHMÄ", "UUSRYHMÄ").containsAll(
                ryhmat.stream().map(KayttoOikeusRyhmaDto::getName).collect(Collectors.toList())));

        ryhmat = kayttoOikeusRyhmaRepository.findKayttoOikeusRyhmasByKayttoOikeusIds(Collections.singletonList(234234L));
        assertEquals(0, ryhmat.size());
        ryhmat = kayttoOikeusRyhmaRepository.findKayttoOikeusRyhmasByKayttoOikeusIds(Collections.emptyList());
        assertEquals(0, ryhmat.size());
    }
}
