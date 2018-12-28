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
        onRequest().havingMethod(is("POST"))
                .respond().withStatus(OK).withContentType(MediaType.APPLICATION_JSON.getType()).withBody("{}");

        Arrays.stream(ExternalPermissionService.values()).forEach(this::getPermission);
    }

    private void getPermission(ExternalPermissionService service) {
        PermissionCheckRequestDto request = new PermissionCheckRequestDto();

        PermissionCheckResponseDto response = client.getPermission(service, request);

        assertThat(response).returns(false, PermissionCheckResponseDto::isAccessAllowed);
    }

}
