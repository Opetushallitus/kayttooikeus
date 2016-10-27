package fi.vm.sade.kayttooikeus.repositories;

import com.querydsl.core.Tuple;
import fi.vm.sade.kayttooikeus.model.KayttoOikeusRyhma;
import fi.vm.sade.kayttooikeus.model.MyonnettyKayttoOikeusRyhmaTapahtuma;
import fi.vm.sade.kayttooikeus.model.QMyonnettyKayttoOikeusRyhmaTapahtuma;
import fi.vm.sade.kayttooikeus.model.QOrganisaatioHenkilo;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusPopulator.oikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma;
import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.henkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloKayttoOikeusPopulator.myonnettyKayttoOikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloPopulator.organisaatioHenkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.TextGroupPopulator.text;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
public class KayttoOikeusRyhmaRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private KayttoOikeusRyhmaRepository kayttoOikeusRyhmaRepository;

    @Test
    public void listAllTest() {
        populate(kayttoOikeusRyhma("RYHMÄ")
                .withOikeus(oikeus("APP1", "READ")) // ensure unique ryhmä
                .withOikeus(oikeus("APP2", "WRITE")));
        
        List<KayttoOikeusRyhma> ryhmas = kayttoOikeusRyhmaRepository.listAll();
        assertEquals(1, ryhmas.size());
        assertEquals("RYHMÄ", ryhmas.get(0).getName());
    }

    @Test
    public void Test() {
        QOrganisaatioHenkilo organisaatioHenkilo = QOrganisaatioHenkilo.organisaatioHenkilo;
        QMyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma
                = QMyonnettyKayttoOikeusRyhmaTapahtuma.myonnettyKayttoOikeusRyhmaTapahtuma;
        MyonnettyKayttoOikeusRyhmaTapahtuma tapahtuma = populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo("1.2.3.4.5"), "1.0.0.102.0"),
                kayttoOikeusRyhma("RYHMA").withKuvaus(text("FI", "Kuvaus")).withCustomId(20L)
                        .withOikeus(oikeus("HENKILOHALLINTA", "READ"))
        ).voimassaAlkaen(new LocalDate().minusMonths(3))
                .voimassaPaattyen(new LocalDate().plusMonths(3)));

        List<Tuple> list = this.kayttoOikeusRyhmaRepository.findOrganisaatioOidAndRyhmaIdByHenkiloOid(
                tapahtuma.getOrganisaatioHenkilo().getHenkilo().getOidHenkilo());
        assertEquals(list.get(0).get(organisaatioHenkilo.organisaatioOid),
                tapahtuma.getOrganisaatioHenkilo().getOrganisaatioOid());
        assertEquals(list.get(0).get(myonnettyKayttoOikeusRyhmaTapahtuma.kayttoOikeusRyhma.id),
                tapahtuma.getKayttoOikeusRyhma().getId());
    }

}
