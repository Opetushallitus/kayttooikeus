package fi.vm.sade.kayttooikeus.service.report.accessrights;

import fi.vm.sade.kayttooikeus.dto.OrganisaatioWithChildrenDto;
import fi.vm.sade.kayttooikeus.report.AccessRightReportRow;
import fi.vm.sade.kayttooikeus.service.OrganisaatioService;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
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

    private final OppijanumerorekisteriClient onrClient;

    @Override
    public List<AccessRightReportRow> getForOrganisation(final String oid) {
        final String lang = onrClient.resolveLanguageCodeForCurrentUser();
        final Map<String, OrganisaatioWithChildrenDto> orgs = resolveOrgs(oid);
        @SuppressWarnings("unchecked") final List<AccessRightReportRow> result = em
                .createNamedQuery("AccessRightReport")
                .setParameter("oids", orgs.keySet())
                .setParameter("lang", lang)
                .getResultList();
        enrichOrganisationName(result, orgs, lang);
        return result;
    }

    protected Map<String, OrganisaatioWithChildrenDto> resolveOrgs(final String oid) {
        return flatten(organisaatioService.getByOid(oid)).stream()
                .collect(Collectors.toMap(OrganisaatioWithChildrenDto::getOid, Function.identity()));
    }

    private List<OrganisaatioWithChildrenDto> flatten(final OrganisaatioWithChildrenDto node) {
        return Stream.concat(
                Stream.of(node),
                node.getChildren().stream().map(this::flatten).flatMap(Collection::stream)
        ).collect(Collectors.toList());
    }

    private void enrichOrganisationName(final List<AccessRightReportRow> result,
                                        final Map<String, OrganisaatioWithChildrenDto> orgs,
                                        final String lang) {
        result.forEach(resultRow -> resultRow.setOrganisationName(resolveOrgName(resultRow.getOrganisationOid(), orgs, lang)));
    }

    private String resolveOrgName(final String organisationOid,
                                  final Map<String, OrganisaatioWithChildrenDto> orgs,
                                  final String lang) {
        return orgs.get(organisationOid).getNimi().get(lang);
    }
}
