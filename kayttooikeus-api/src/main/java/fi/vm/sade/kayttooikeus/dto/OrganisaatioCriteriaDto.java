package fi.vm.sade.kayttooikeus.dto;

import fi.vm.sade.kayttooikeus.dto.enumeration.OrganisaatioStatus;
import fi.vm.sade.kayttooikeus.dto.enumeration.OrganisaatioTyyppi;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;

@Getter
@Setter
@ToString
public class OrganisaatioCriteriaDto {

    private OrganisaatioTyyppi tyyppi;
    private Set<OrganisaatioStatus> tila;

}
