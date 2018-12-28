package fi.vm.sade.kayttooikeus.dto.permissioncheck;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionCheckResponseDto {

    private boolean accessAllowed = false;
    private String errorMessage;

    public boolean isAccessAllowed() {
        return accessAllowed;
    }

    public static PermissionCheckResponseDto allowed() {
        PermissionCheckResponseDto dto = new PermissionCheckResponseDto();
        dto.setAccessAllowed(true);
        return dto;
    }

    public static PermissionCheckResponseDto denied() {
        PermissionCheckResponseDto dto = new PermissionCheckResponseDto();
        dto.setAccessAllowed(false);
        return dto;
    }

}
