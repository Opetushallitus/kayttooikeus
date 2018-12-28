package fi.vm.sade.kayttooikeus.service.external;

import fi.vm.sade.kayttooikeus.dto.permissioncheck.ExternalPermissionService;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.PermissionCheckRequestDto;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.PermissionCheckResponseDto;

public interface ExternalPermissionClient {

    PermissionCheckResponseDto getPermission(ExternalPermissionService service, PermissionCheckRequestDto dto);

}
