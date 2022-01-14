package fi.vm.sade.kayttooikeus.controller;

import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import fi.vm.sade.kayttooikeus.service.report.accessrights.AccessRightReport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class ReportControllerTest extends AbstractControllerTest {

    private static final String TEST_OID = "test";
    private static final String accessRightsReportUrl = ReportController.REQUEST_MAPPING + ReportController.ACCESS_RIGHTS + "/" + TEST_OID;

    @MockBean
    AccessRightReport accessRightReport;

    @MockBean
    PermissionCheckerService permissionCheckerService;

    @Test
    public void getAccessRightsReportUnauthorized() throws Exception {
        this.mvc.perform(get(accessRightsReportUrl))
                .andExpect(status().is3xxRedirection()); // CAS

        verifyNoInteractions(accessRightReport);
    }

    @Test
    @WithMockUser
    public void getAccessRightsReportAccessDenied() throws Exception {
        given(permissionCheckerService.checkRoleForOrganisation(eq(List.of(TEST_OID)), any(Map.class))).willReturn(false);

        this.mvc.perform(get(accessRightsReportUrl))
                .andExpect(status().is4xxClientError()); // Permission checker

        verifyNoInteractions(accessRightReport);
    }

    @Test
    @WithMockUser
    public void getAccessRightsReport() throws Exception {
        given(permissionCheckerService.checkRoleForOrganisation(eq(List.of(TEST_OID)), any(Map.class))).willReturn(true);

        this.mvc.perform(get(accessRightsReportUrl))
                .andExpect(status().isOk());

        verify(accessRightReport, times(1)).getForOrganisation(TEST_OID);
    }
}
