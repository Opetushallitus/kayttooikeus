package fi.vm.sade.kayttooikeus.service.report.accessrights;

import fi.vm.sade.kayttooikeus.dto.OrganisaatioWithChildrenDto;
import fi.vm.sade.kayttooikeus.report.AccessRightReportRow;
import fi.vm.sade.kayttooikeus.repositories.AbstractRepositoryTest;
import fi.vm.sade.kayttooikeus.service.OrganisaatioService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator.henkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloKayttoOikeusPopulator.myonnettyKayttoOikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloPopulator.organisaatioHenkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.TextGroupPopulator.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccessRightReportImplTest extends AbstractRepositoryTest {

    static final String ORG = "orgOid";

    @Autowired
    private AccessRightReportImpl accessRightReport;

    @MockBean
    private OrganisaatioService organisaatioService;

    @Test
    public void createReport() {
        OrganisaatioWithChildrenDto root = mockOrg(ORG, List.of());
        when(organisaatioService.getByOid(ORG)).thenReturn(root);
        long id = createRowData("person", ORG);

        List<AccessRightReportRow> report = accessRightReport.getForOrganisation(ORG);

        assertThat(report).hasSize(1);
        assertThat(report.get(0).getAccessRightId()).isEqualTo(id);
    }

    @Test
    public void createReportMultipleRows() {
        int rows = 5;
        OrganisaatioWithChildrenDto root = mockOrg(ORG, List.of());
        when(organisaatioService.getByOid(ORG)).thenReturn(root);
        IntStream.range(0, rows).forEach(i -> createRowData("person" + i, ORG));

        List<AccessRightReportRow> report = accessRightReport.getForOrganisation(ORG);

        assertThat(report).hasSize(rows);
    }

    @Test
    public void createReportMultipleOrgs() {
        OrganisaatioWithChildrenDto level1 = mockOrg("level1", List.of());
        OrganisaatioWithChildrenDto root = mockOrg(ORG, List.of(level1));
        when(organisaatioService.getByOid(ORG)).thenReturn(root);
        createRowData("person0", ORG);
        createRowData("person1", "level1");

        List<AccessRightReportRow> report = accessRightReport.getForOrganisation(ORG);

        assertThat(report).hasSize(2);
    }

    @Test
    public void resolveOrgs() {
        OrganisaatioWithChildrenDto level2 = mockOrg("level2", List.of());
        OrganisaatioWithChildrenDto level1 = mockOrg("level1", List.of(level2));
        OrganisaatioWithChildrenDto root = mockOrg(ORG, List.of(level1));
        when(organisaatioService.getByOid(ORG)).thenReturn(root);

        Map<String, OrganisaatioWithChildrenDto> result = accessRightReport.resolveOrgs(ORG);

        assertThat(result)
                .hasSize(3)
                .containsKey(ORG)
                .containsKey("level1")
                .containsKey("level2");
    }

    private OrganisaatioWithChildrenDto mockOrg(String oid, List<OrganisaatioWithChildrenDto> children) {
        OrganisaatioWithChildrenDto org = mock(OrganisaatioWithChildrenDto.class, Answers.RETURNS_DEEP_STUBS);
        when(org.getOid()).thenReturn(oid);
        when(org.getChildren()).thenReturn(children);
        return org;
    }

    private long createRowData(String personOid, String orgOid) {
        return populate(myonnettyKayttoOikeus(
                organisaatioHenkilo(henkilo(personOid), orgOid),
                kayttoOikeusRyhma("RYHMA").withNimi(text("FI", "KUVAUS")))
                .voimassaPaattyen(LocalDate.now())).getKayttoOikeusRyhma().getId();
    }
}
