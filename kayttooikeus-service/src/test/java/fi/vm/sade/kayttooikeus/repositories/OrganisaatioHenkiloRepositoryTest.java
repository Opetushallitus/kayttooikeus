package fi.vm.sade.kayttooikeus.repositories;

import com.google.common.collect.Lists;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.henkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloPopulator.organisaatioHenkilo;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class OrganisaatioHenkiloRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private OrganisaatioHenkiloRepository organisaatioHenkiloRepository;
    
    @Test
    public void findDistinctOrganisaatiosForHenkiloOidEmptyTest() {
        List<String> results = organisaatioHenkiloRepository.findDistinctOrganisaatiosForHenkiloOid("oid");
        assertEquals(0, results.size());
    }
    
    @Test
    public void findDistinctOrganisaatiosForHenkiloOidTest() {
        populate(organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"));
        List<String> results = organisaatioHenkiloRepository.findDistinctOrganisaatiosForHenkiloOid("1.2.3.4.5");
        assertEquals(1, results.size());
        assertEquals("3.4.5.6.7", results.get(0));
    }

    @Test
    public void findByHenkiloOidAndOrganisaatioOidTest() {
        populate(organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"));
        Optional<OrganisaatioHenkiloDto> organisaatioHenkilo = organisaatioHenkiloRepository.findByHenkiloOidAndOrganisaatioOid("1.2.3.4.5", "3.4.5.6.7");
        assertTrue(organisaatioHenkilo.isPresent());
        assertEquals("3.4.5.6.7", organisaatioHenkilo.get().getOrganisaatioOid());

        organisaatioHenkilo = organisaatioHenkiloRepository.findByHenkiloOidAndOrganisaatioOid("1.2.3.4.5", "1.1.1.1.madeup");
        assertFalse(organisaatioHenkilo.isPresent());

        organisaatioHenkilo = organisaatioHenkiloRepository.findByHenkiloOidAndOrganisaatioOid("1.2.3.4.madeup", "3.4.5.6.7");
        assertFalse(organisaatioHenkilo.isPresent());
    }

    @Test
    public void findOrganisaatioHenkilosForHenkiloTest() throws Exception {
        populate(organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.7"));
        populate(organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.8"));
        populate(organisaatioHenkilo(henkilo("1.2.3.4.5"), "3.4.5.6.9"));
        populate(organisaatioHenkilo(henkilo("1.2.3.4.6"), "3.4.5.6.9"));

        List<OrganisaatioHenkiloDto> organisaatioHenkilos = organisaatioHenkiloRepository.findOrganisaatioHenkilosForHenkilo("1.2.3.4.5");
        assertEquals(3, organisaatioHenkilos.size());
        List<String> oids = organisaatioHenkilos.stream().map(OrganisaatioHenkiloDto::getOrganisaatioOid).collect(Collectors.toList());
        assertTrue(oids.containsAll(Lists.newArrayList("3.4.5.6.7", "3.4.5.6.8", "3.4.5.6.9")));
    }
}
