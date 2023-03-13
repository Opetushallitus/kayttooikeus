package fi.vm.sade.kayttooikeus.dto;

import lombok.*;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentifiedHenkiloTypeDto {
    private String oidHenkilo;
    private KayttajaTyyppi henkiloTyyppi;
    private KayttajatiedotReadDto kayttajatiedot;
    private String idpEntityId;
}
