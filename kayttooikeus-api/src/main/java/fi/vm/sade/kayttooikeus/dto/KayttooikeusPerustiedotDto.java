package fi.vm.sade.kayttooikeus.dto;

import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KayttooikeusPerustiedotDto {
    protected String oidHenkilo;
    protected String username;
    protected KayttajaTyyppi kayttajaTyyppi;
    protected Set<KayttooikeusOrganisaatiotDto> organisaatiot = new HashSet<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KayttooikeusOrganisaatiotDto {
        protected String organisaatioOid;
        protected Set<KayttooikeusOikeudetDto> kayttooikeudet = new HashSet<>();

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        @EqualsAndHashCode
        public static class KayttooikeusOikeudetDto {
            protected String palvelu;
            protected String oikeus;
        }
    }
}
