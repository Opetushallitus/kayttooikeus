package fi.vm.sade.kayttooikeus.service.dto;

import static java.util.Objects.requireNonNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HenkiloVahvaTunnistusDto {

    private final String hetu;
    private String tyosahkopostiosoite;

    public HenkiloVahvaTunnistusDto(String hetu) {
        this.hetu = requireNonNull(hetu);
    }

}
