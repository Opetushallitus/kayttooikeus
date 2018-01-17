package fi.vm.sade.kayttooikeus.util;

import fi.vm.sade.oppijanumerorekisteri.dto.YhteystiedotRyhmaDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoTyyppi;
import static java.util.Comparator.comparing;
import java.util.Optional;
import java.util.stream.StreamSupport;

public final class YhteystietoUtil {

    private YhteystietoUtil() {
    }

    public static Optional<String> getYhteystietoArvo(
            Iterable<YhteystiedotRyhmaDto> yhteystietoRyhmat,
            YhteystietoTyyppi tyyppi, String... ryhmaKuvausJarjestys) {
        if (yhteystietoRyhmat == null) {
            return Optional.empty();
        }
        return StreamSupport.stream(yhteystietoRyhmat.spliterator(), false)
                .sorted(comparing(YhteystiedotRyhmaDto::getRyhmaKuvaus,
                        new CustomOrderComparator<>(ryhmaKuvausJarjestys)))
                .flatMap(t -> t.getYhteystieto().stream()
                        .filter(u -> tyyppi.equals(u.getYhteystietoTyyppi()))
                        .filter(u -> u.getYhteystietoArvo() != null
                                && !u.getYhteystietoArvo().isEmpty()))
                .map(YhteystietoDto::getYhteystietoArvo)
                .findFirst();
    }

}
