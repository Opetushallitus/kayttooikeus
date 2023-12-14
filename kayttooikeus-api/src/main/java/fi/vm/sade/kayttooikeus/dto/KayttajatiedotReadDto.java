package fi.vm.sade.kayttooikeus.dto;

import lombok.Data;

@Data
public class KayttajatiedotReadDto {
    private final String username;
    private final MfaProvider mfaProvider;
    private final KayttajaTyyppi kayttajaTyyppi;
}
