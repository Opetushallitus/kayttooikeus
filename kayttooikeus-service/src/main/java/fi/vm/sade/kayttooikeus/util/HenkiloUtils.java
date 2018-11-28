package fi.vm.sade.kayttooikeus.util;

import fi.vm.sade.kayttooikeus.dto.enumeration.LogInRedirectType;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystiedotRyhmaDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoTyyppi;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

public final class HenkiloUtils {

    private HenkiloUtils() {
    }

    public static Optional<String> getYhteystieto(HenkiloDto dto, String ryhmaKuvaus, YhteystietoTyyppi yhteystietoTyyppi) {
        Set<YhteystiedotRyhmaDto> yhteystiedotRyhma = dto.getYhteystiedotRyhma();
        if (yhteystiedotRyhma == null) {
            return Optional.empty();
        }
        return yhteystiedotRyhma.stream()
                .filter(yhteystietoryhma -> ryhmaKuvaus.equals(yhteystietoryhma.getRyhmaKuvaus()))
                .flatMap(yhteystietoryhma -> yhteystietoryhma.getYhteystieto().stream())
                .filter(yhteystieto -> yhteystietoTyyppi.equals(yhteystieto.getYhteystietoTyyppi())
                        && yhteystieto.getYhteystietoArvo() != null && !yhteystieto.getYhteystietoArvo().isEmpty())
                .map(YhteystietoDto::getYhteystietoArvo)
                .findFirst();
    }

    public static LogInRedirectType getLoginRedirectType(Henkilo henkilo, boolean isVahvastiTunnistettu, LocalDateTime now) {
        if(Boolean.FALSE.equals(isVahvastiTunnistettu)) {
            return LogInRedirectType.STRONG_IDENTIFICATION;
        }

        LocalDateTime sixMonthsAgo = now.minusMonths(6);
        if(henkilo.getSahkopostivarmennusAikaleima() == null || henkilo.getSahkopostivarmennusAikaleima().isBefore(sixMonthsAgo)) {
            return LogInRedirectType.EMAIL_VERIFICATION;
        }

        return null;
    }

}
