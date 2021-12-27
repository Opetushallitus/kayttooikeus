package fi.vm.sade.kayttooikeus.service.report.accessrights;

import fi.vm.sade.kayttooikeus.report.AccessRightReportRow;

import java.util.List;

public interface AccessRightReport {

    List<AccessRightReportRow> getForOrganisation(String oid);
}
