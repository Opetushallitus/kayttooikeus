package fi.vm.sade.kayttooikeus.dto;

import fi.vm.sade.kayttooikeus.dto.enumeration.OrganisaatioStatus;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganisaatioDto {
    private String oid;
    private String parentOidPath;
    private TextGroupMapDto nimi;
    private List<String> tyypit;
    private OrganisaatioStatus status;
}
