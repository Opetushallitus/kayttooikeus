package fi.vm.sade.kayttooikeus.service.external;

import fi.vm.sade.kayttooikeus.dto.permissioncheck.ExternalPermissionService;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.PermissionCheckRequestDto;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.PermissionCheckResponseDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

import static net.jadler.Jadler.onRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(SpringRunner.class)
public class ExternalPermissionClientTest extends AbstractClientTest {

    @Autowired
    private ExternalPermissionClient client;

    @Test
    public void getPermission() {
        onRequest().havingMethod(is("POST")).havingBodyEqualTo("{\"personOidsForSamePerson\":null,\"organisationOids\":[],\"loggedInUserRoles\":null,\"loggedInUserOid\":\"1.2.2.1\"}")
                .respond().withStatus(OK).withContentType(MediaType.APPLICATION_JSON.getType()).withBody("{}");
        onRequest().havingMethod(is("POST")).havingBodyEqualTo("{\"personOidsForSamePerson\":null,\"organisationOids\":[],\"loggedInUserRoles\":null,\"loggedInUserOid\":\"1.2.2.2\"}")
                .respond().withStatus(OK).withContentType(MediaType.APPLICATION_JSON.getType()).withBody("{\"accessAllowed\":true,\"errorMessage\":null}");

        Arrays.stream(ExternalPermissionService.values()).forEach(this::getPermission);
    }

    private void getPermission(ExternalPermissionService service) {
        PermissionCheckRequestDto dto = new PermissionCheckRequestDto();
        if (service == ExternalPermissionService.HAKU_APP) {
            dto.setLoggedInUserOid("1.2.2.2");
            PermissionCheckResponseDto response = client.getPermission(service, dto);

            assertThat(response).returns(true, PermissionCheckResponseDto::isAccessAllowed);
        } else {
            dto.setLoggedInUserOid("1.2.2.1");
            PermissionCheckResponseDto response = client.getPermission(service, dto);

            assertThat(response).returns(false, PermissionCheckResponseDto::isAccessAllowed);
        }
    }

}