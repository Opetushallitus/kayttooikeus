package fi.vm.sade.kayttooikeus.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Deprecated
public class MeDto {
    // username
    private String uid;

    private String oid;

    // kutsumanimi
    private String firstName;

    private String lastName;

    private String[] groups = new String[0];

    private String roles;

    private String lang;

}
