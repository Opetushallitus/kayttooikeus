package fi.vm.sade.kayttooikeus.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class VahvaTunnistusResponseDto {

    private String authToken;
    private String service;

}
