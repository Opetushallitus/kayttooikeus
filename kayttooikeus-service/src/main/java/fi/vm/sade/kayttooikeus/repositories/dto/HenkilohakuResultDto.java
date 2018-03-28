package fi.vm.sade.kayttooikeus.repositories.dto;

import fi.vm.sade.kayttooikeus.dto.OrganisaatioMinimalDto;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import static java.util.stream.Collectors.joining;
import java.util.stream.Stream;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "oidHenkilo")
public class HenkilohakuResultDto {

    private String oidHenkilo;

    private String etunimet;

    private String sukunimi;

    private String kayttajatunnus;

    private List<OrganisaatioMinimalDto> organisaatioNimiList = new ArrayList<>();

    public HenkilohakuResultDto(String oidHenkilo, String etunimet, String sukunimi, String kayttajatunnus) {
        this.oidHenkilo = oidHenkilo;
        this.etunimet = etunimet;
        this.sukunimi = sukunimi;
        this.kayttajatunnus = kayttajatunnus;
    }

    public String getNimi() {
        return Stream.of(sukunimi, etunimet)
                .filter(Objects::nonNull)
                .collect(joining(", "));
    }

}
