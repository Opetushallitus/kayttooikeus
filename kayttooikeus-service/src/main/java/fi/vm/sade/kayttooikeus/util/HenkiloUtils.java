package fi.vm.sade.kayttooikeus.util;

import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystiedotRyhmaDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoTyyppi;
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

}
