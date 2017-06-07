package fi.vm.sade.kayttooikeus.dto;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HenkilohakuCriteriaDto {

    private Boolean subOrganisation;
    private Boolean noOrganisation;
    private Boolean passivoitu;
    private Boolean duplikaatti;

    private String nameQuery;

    private String organisaatioOid;
    private Long kayttooikeusryhmaId;

}