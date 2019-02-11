package fi.vm.sade.kayttooikeus.dto.permissioncheck;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class PermissionCheckRequestDto {

    private List<String> personOidsForSamePerson;
    private List<String> organisationOids = new ArrayList<>();
    private Set<String> loggedInUserRoles;
    private String loggedInUserOid;
}
