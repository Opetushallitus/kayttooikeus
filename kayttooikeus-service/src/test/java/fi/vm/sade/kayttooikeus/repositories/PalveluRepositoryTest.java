package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.dto.PalveluDto;
import fi.vm.sade.kayttooikeus.dto.PalveluTyyppi;
import fi.vm.sade.kayttooikeus.model.Palvelu;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static fi.vm.sade.kayttooikeus.repositories.populate.PalveluPopulator.palvelu;
import static fi.vm.sade.kayttooikeus.repositories.populate.TextGroupPopulator.text;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@Sql("/truncate_tables.sql")
public class PalveluRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private PalveluRepository palveluRepository;

    @Test
    public void findAllTest() {
        populate(palvelu("PALVELU1"));
        populate(palvelu("PALVELU2"));
        
        List<PalveluDto> palvelus = palveluRepository.findAll();
        assertEquals(2, palvelus.size());
        assertEquals(new HashSet<>(asList("PALVELU1", "PALVELU2")),
                palvelus.stream().map(PalveluDto::getName).collect(toSet()));
    }

    @Test
    public void findByNameTest() throws Exception {
        Optional<Palvelu> palvelu = palveluRepository.findByName("PALVELU3");
        assertFalse(palvelu.isPresent());

        populate(palvelu("PALVELU3")
                .kuvaus(text("FI", "Henkilöpalvelu")));
        palvelu = palveluRepository.findByName("PALVELU3");
        assertTrue(palvelu.isPresent());
        assertEquals("PALVELU3", palvelu.get().getName());
        assertEquals(PalveluTyyppi.YKSITTAINEN, palvelu.get().getPalveluTyyppi());
        assertEquals("Henkilöpalvelu", palvelu.get().getDescription().getTexts().stream().findFirst().get().getText());
    }

}
