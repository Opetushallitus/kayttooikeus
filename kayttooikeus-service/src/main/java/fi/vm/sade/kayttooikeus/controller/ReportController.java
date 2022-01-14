package fi.vm.sade.kayttooikeus.controller;

import fi.vm.sade.kayttooikeus.report.AccessRightReportRow;
import fi.vm.sade.kayttooikeus.service.report.accessrights.AccessRightReport;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.HttpURLConnection;
import java.util.List;

@Api(tags = "Report generation endpoints")
@RestController
@RequestMapping(ReportController.REQUEST_MAPPING)
@RequiredArgsConstructor
public class ReportController {

    protected static final String REQUEST_MAPPING = "/reports";
    protected static final String ACCESS_RIGHTS = "/accessrights";

    private final AccessRightReport accessRightReport;

    @GetMapping(ACCESS_RIGHTS + "/{oid}")
    @PreAuthorize("@permissionCheckerServiceImpl.checkRoleForOrganisation({#oid}, {'KAYTTOOIKEUS': {'ACCESS_RIGHTS_REPORT'}})")
    @ApiOperation("Report of access rights for all users under given organisation")
    @ApiResponses(value = {
            @ApiResponse(
                    code = HttpURLConnection.HTTP_OK,
                    message = "Report of access rights for all users under given organisation"
            )})
    public List<AccessRightReportRow> getAccessRightsReport(@PathVariable(value = "oid") final String oid) {
        return accessRightReport.getForOrganisation(oid);
    }
}
