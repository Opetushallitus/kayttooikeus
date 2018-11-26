package fi.vm.sade.kayttooikeus.service.it;

import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloCreateDto;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloDto;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloUpdateDto;
import fi.vm.sade.kayttooikeus.dto.enumeration.OrganisaatioStatus;
import fi.vm.sade.kayttooikeus.service.OrganisaatioHenkiloService;
import fi.vm.sade.kayttooikeus.service.OrganisaatioService;
import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.henkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloPopulator.organisaatioHenkilo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
public class OrganisaatioHenkiloTest extends AbstractServiceIntegrationTest {
    @MockBean
    private OrganisaatioClient organisaatioClient;

    @MockBean
    private PermissionCheckerService permissionCheckerService;

    @MockBean
    private OrganisaatioService organisaatioService;

    @Autowired
    private OrganisaatioHenkiloService organisaatioHenkiloService;

    @Test
    @WithMockUser(username = "user1")
    public void addOrganisaatioHenkilotShouldOnlyAddNewOrganisaatio() {
        OrganisaatioPerustieto organisaatio = OrganisaatioPerustieto.builder().status(OrganisaatioStatus.AKTIIVINEN).build();
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(anyString())).willReturn(Optional.of(organisaatio));
        populate(organisaatioHenkilo(henkilo("henkilo1"), "organisaatio1").tehtavanimike("tehtävä1"));
        populate(organisaatioHenkilo(henkilo("henkilo1"), "organisaatio3").tehtavanimike("tehtävä3"));
        List<OrganisaatioHenkiloCreateDto> organisaatioHenkilot = new ArrayList<>();
        OrganisaatioHenkiloCreateDto organisaatio1 = new OrganisaatioHenkiloCreateDto();
        organisaatio1.setOrganisaatioOid("organisaatio1");
        organisaatio1.setTehtavanimike("tehtävä1-päivitetty");
        organisaatioHenkilot.add(organisaatio1);
        OrganisaatioHenkiloCreateDto organisaatio2 = new OrganisaatioHenkiloCreateDto();
        organisaatio2.setOrganisaatioOid("organisaatio2");
        organisaatio2.setTehtavanimike("tehtävä2");
        organisaatioHenkilot.add(organisaatio2);

        List<OrganisaatioHenkiloDto> addOrganisaatioHenkilot = organisaatioHenkiloService.addOrganisaatioHenkilot("henkilo1", organisaatioHenkilot);

        assertThat(addOrganisaatioHenkilot).extracting("organisaatioOid", "tehtavanimike")
                .containsExactlyInAnyOrder(
                        tuple("organisaatio1", "tehtävä1"),
                        tuple("organisaatio2", "tehtävä2"),
                        tuple("organisaatio3", "tehtävä3")
                );
    }

    @Test
    @WithMockUser("henkilo1")
    public void CreateOrUpdateOrganisaatioHenkilos() {
        OrganisaatioPerustieto organisaatio = OrganisaatioPerustieto.builder().status(OrganisaatioStatus.AKTIIVINEN).build();
        given(this.organisaatioClient.getOrganisaatioPerustiedotCached(anyString())).willReturn(Optional.of(organisaatio));
        populate(organisaatioHenkilo(henkilo("henkilo1"), "organisaatio1").tehtavanimike("tehtävä1"));
        populate(organisaatioHenkilo(henkilo("henkilo1"), "organisaatio3").tehtavanimike("tehtävä3"));

        List<OrganisaatioHenkiloUpdateDto> organisaatioHenkilot = new ArrayList<>();
        OrganisaatioHenkiloUpdateDto organisaatio1 = new OrganisaatioHenkiloUpdateDto();
        organisaatio1.setOrganisaatioOid("organisaatio1");
        organisaatio1.setTehtavanimike("tehtävä1-päivitetty");
        organisaatioHenkilot.add(organisaatio1);
        OrganisaatioHenkiloUpdateDto organisaatio2 = new OrganisaatioHenkiloUpdateDto();
        organisaatio2.setOrganisaatioOid("organisaatio2");
        organisaatio2.setTehtavanimike("tehtävä2");
        organisaatioHenkilot.add(organisaatio2);

        List<OrganisaatioHenkiloDto> addOrganisaatioHenkilot = this.organisaatioHenkiloService.createOrUpdateOrganisaatioHenkilos("henkilo1", organisaatioHenkilot);

        assertThat(addOrganisaatioHenkilot).extracting("organisaatioOid", "tehtavanimike")
                .containsExactlyInAnyOrder(
                        tuple("organisaatio1", "tehtävä1-päivitetty"),
                        tuple("organisaatio2", "tehtävä2"),
                        tuple("organisaatio3", "tehtävä3")
                );
    }
}
