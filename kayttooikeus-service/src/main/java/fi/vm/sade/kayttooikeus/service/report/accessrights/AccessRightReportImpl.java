package fi.vm.sade.kayttooikeus.service.report.accessrights;

import fi.vm.sade.kayttooikeus.dto.OrganisaatioWithChildrenDto;
import fi.vm.sade.kayttooikeus.report.AccessRightReportRow;
import fi.vm.sade.kayttooikeus.service.OrganisaatioService;
import fi.vm.sade.kayttooikeus.service.impl.KayttoOikeusServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccessRightReportImpl implements AccessRightReport {

    private final EntityManager em;

    private final OrganisaatioService organisaatioService;

    @Override
    public List<AccessRightReportRow> getForOrganisation(final String oid) {
        final Map<String, OrganisaatioWithChildrenDto> orgs = resolveOrgs(oid);
        @SuppressWarnings("unchecked") final List<AccessRightReportRow> result = em
                .createNamedQuery("AccessRightReport")
                .setParameter("oids", orgs.keySet())
                .getResultList();
        enrichOrganisationName(result, orgs);
        return result;
    }

    protected Map<String, OrganisaatioWithChildrenDto> resolveOrgs(final String oid) {
        return flatten(organisaatioService.getByOid(oid)).stream()
                .collect(Collectors.toMap(OrganisaatioWithChildrenDto::getOid, Function.identity()));
    }

    private List<OrganisaatioWithChildrenDto> flatten(OrganisaatioWithChildrenDto node) {
        return Stream.concat(
                Stream.of(node),
                node.getChildren().stream().map(this::flatten).flatMap(Collection::stream)
        ).collect(Collectors.toList());
    }

    private void enrichOrganisationName(final List<AccessRightReportRow> result, final Map<String, OrganisaatioWithChildrenDto> orgs) {
        result.forEach(resultRow -> resultRow.setOrganisationName(resolveOrgName(resultRow.getOrganisationOid(), orgs)));
    }

    private String resolveOrgName(String organisationOid, Map<String, OrganisaatioWithChildrenDto> orgs) {
        return orgs.get(organisationOid).getNimi().get(KayttoOikeusServiceImpl.FI);
    }
}
