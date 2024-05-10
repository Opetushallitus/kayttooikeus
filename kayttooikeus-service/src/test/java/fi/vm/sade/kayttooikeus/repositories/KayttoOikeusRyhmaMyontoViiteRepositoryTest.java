package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.model.KayttoOikeusRyhmaMyontoViite;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusPopulator.oikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaMyontoViitePopulator.kayttoOikeusRyhmaMyontoViite;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma;
import static fi.vm.sade.kayttooikeus.repositories.populate.TextGroupPopulator.text;
import static org.junit.Assert.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@Sql("/truncate_tables.sql")
public class KayttoOikeusRyhmaMyontoViiteRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private KayttoOikeusRyhmaMyontoViiteRepository kayttoOikeusRyhmaMyontoViiteRepository;

    @Test
    public void getSlaveIdsByMasterIdsTest() throws Exception {
        long master1 = populate(kayttoOikeusRyhma("MASTER1").withNimi(text("FI", "Master1").put("EN", "Master1"))).getId();
        long master2 = populate(kayttoOikeusRyhma("MASTER2").withNimi(text("FI", "Master2").put("EN", "Master2"))).getId();

        Long id = populate(kayttoOikeusRyhma("RYHMA").withNimi(text("FI", "Käyttäjähallinta")
                .put("EN", "User management"))
                .withOikeus(oikeus("HENKILOHALLINTA", "CRUD"))
                .withOikeus(oikeus("KOODISTO", "READ"))).getId();

        Long id2 = populate(kayttoOikeusRyhma("RYHMA2")
                .withOikeus(oikeus("PALVELU1", "CRUD"))
                .withOikeus(oikeus("PALVELU2", "READ"))).getId();

        Long id3 = populate(kayttoOikeusRyhma("RYHMA3")
                .withOikeus(oikeus("PALVELU3", "CRUD"))
                .withOikeus(oikeus("PALVELU4", "READ"))).getId();

        populate(kayttoOikeusRyhmaMyontoViite(master1, id));
        populate(kayttoOikeusRyhmaMyontoViite(master1, id2));
        populate(kayttoOikeusRyhmaMyontoViite(master2, id3));

        List<Long> ids = kayttoOikeusRyhmaMyontoViiteRepository.getSlaveIdsByMasterIds(Collections.singletonList(master1));
        assertEquals(2, ids.size());
        assertTrue(ids.containsAll(Arrays.asList(id, id2)));
        ids = kayttoOikeusRyhmaMyontoViiteRepository.getSlaveIdsByMasterIds(Collections.singletonList(master2));
        assertEquals(1, ids.size());
        assertTrue(ids.containsAll(Collections.singletonList(id3)));
    }

    @Test
    public void getSlaveIdsByMasterIdsEmpty() {
        List<Long> ids = this.kayttoOikeusRyhmaMyontoViiteRepository.getSlaveIdsByMasterIds(Collections.singletonList(1000L));
        assertThat(ids).isNotNull().isEmpty();
    }

    @Test
    public void isCyclicMyontoViiteTest() throws Exception {
        long master1 = populate(kayttoOikeusRyhma("MASTER1").withNimi(text("FI", "Master1").put("EN", "Master1"))).getId();

        Long id = populate(kayttoOikeusRyhma("RYHMA").withNimi(text("FI", "Käyttäjähallinta")
                .put("EN", "User management"))
                .withOikeus(oikeus("HENKILOHALLINTA", "CRUD"))
                .withOikeus(oikeus("KOODISTO", "READ"))).getId();

        Long id2 = populate(kayttoOikeusRyhma("RYHMA2")
                .withOikeus(oikeus("PALVELU1", "CRUD"))
                .withOikeus(oikeus("PALVELU2", "READ"))).getId();

        populate(kayttoOikeusRyhmaMyontoViite(master1, id));
        populate(kayttoOikeusRyhmaMyontoViite(master1, id2));

        boolean isCyclic = kayttoOikeusRyhmaMyontoViiteRepository.isCyclicMyontoViite(id, Collections.singletonList(master1));
        assertTrue(isCyclic);
        isCyclic = kayttoOikeusRyhmaMyontoViiteRepository.isCyclicMyontoViite(id, Arrays.asList(master1, 2000L));
        assertTrue(isCyclic);
        isCyclic = kayttoOikeusRyhmaMyontoViiteRepository.isCyclicMyontoViite(id, Arrays.asList(3000L, 7000L));
        assertFalse(isCyclic);
        isCyclic = kayttoOikeusRyhmaMyontoViiteRepository.isCyclicMyontoViite(5555L, Arrays.asList(3000L, master1));
        assertFalse(isCyclic);
    }

    @Test
    public void getMyontoViitesTest() throws Exception {
        long master1 = populate(kayttoOikeusRyhma("MASTER1").withNimi(text("FI", "Master1").put("EN", "Master1"))).getId();
        long master2 = populate(kayttoOikeusRyhma("MASTER2").withNimi(text("FI", "Master2").put("EN", "Master2"))).getId();

        Long id = populate(kayttoOikeusRyhma("RYHMA").withNimi(text("FI", "Käyttäjähallinta")
                .put("EN", "User management"))
                .withOikeus(oikeus("HENKILOHALLINTA", "CRUD"))
                .withOikeus(oikeus("KOODISTO", "READ"))).getId();

        Long id2 = populate(kayttoOikeusRyhma("RYHMA2")
                .withOikeus(oikeus("PALVELU1", "CRUD"))
                .withOikeus(oikeus("PALVELU2", "READ"))).getId();

        populate(kayttoOikeusRyhmaMyontoViite(master1, id));
        populate(kayttoOikeusRyhmaMyontoViite(master2, id));

        List<KayttoOikeusRyhmaMyontoViite> viites = kayttoOikeusRyhmaMyontoViiteRepository.getMyontoViites(master1);
        assertEquals(1, viites.size());
        assertEquals(master1, viites.get(0).getMasterId().longValue());
        assertEquals(id, viites.get(0).getSlaveId());

        populate(kayttoOikeusRyhmaMyontoViite(master1, id2));
        viites = kayttoOikeusRyhmaMyontoViiteRepository.getMyontoViites(master1);
        assertEquals(2, viites.size());
    }

}
