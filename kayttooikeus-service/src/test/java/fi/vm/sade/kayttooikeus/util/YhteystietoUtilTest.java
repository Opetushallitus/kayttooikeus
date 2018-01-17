package fi.vm.sade.kayttooikeus.util;

import fi.vm.sade.oppijanumerorekisteri.dto.YhteystiedotRyhmaDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoTyyppi;
import java.util.Arrays;
import static java.util.Collections.emptyList;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import org.junit.Test;

public class YhteystietoUtilTest {

    @Test
    public void getYhteystietoArvo() {
        List<YhteystiedotRyhmaDto> yhteystietoRyhmat = Arrays.asList(
                YhteystiedotRyhmaDto.builder()
                        .yhteystieto(YhteystietoDto.builder()
                                .yhteystietoTyyppi(YhteystietoTyyppi.YHTEYSTIETO_KATUOSOITE)
                                .yhteystietoArvo("arvo1")
                                .build())
                        .build(),
                YhteystiedotRyhmaDto.builder()
                        .yhteystieto(YhteystietoDto.builder()
                                .yhteystietoTyyppi(YhteystietoTyyppi.YHTEYSTIETO_POSTINUMERO)
                                .yhteystietoArvo("arvo2")
                                .build())
                        .build(),
                YhteystiedotRyhmaDto.builder()
                        .yhteystieto(YhteystietoDto.builder()
                                .yhteystietoTyyppi(YhteystietoTyyppi.YHTEYSTIETO_KAUPUNKI)
                                .yhteystietoArvo("arvo3")
                                .build())
                        .build()
        );

        assertSoftly(softly -> {
            softly.assertThat(YhteystietoUtil.getYhteystietoArvo(yhteystietoRyhmat,
                    YhteystietoTyyppi.YHTEYSTIETO_KATUOSOITE)).hasValue("arvo1");
            softly.assertThat(YhteystietoUtil.getYhteystietoArvo(yhteystietoRyhmat,
                    YhteystietoTyyppi.YHTEYSTIETO_POSTINUMERO)).hasValue("arvo2");
            softly.assertThat(YhteystietoUtil.getYhteystietoArvo(yhteystietoRyhmat,
                    YhteystietoTyyppi.YHTEYSTIETO_KAUPUNKI)).hasValue("arvo3");
            softly.assertThat(YhteystietoUtil.getYhteystietoArvo(null,
                    YhteystietoTyyppi.YHTEYSTIETO_MAA)).isEmpty();
        });
    }

    @Test
    public void getYhteystietoArvoWithEmpty() {
        List<YhteystiedotRyhmaDto> yhteystietoRyhmat = emptyList();

        Optional<String> yhteystietoArvo = YhteystietoUtil.getYhteystietoArvo(yhteystietoRyhmat, YhteystietoTyyppi.YHTEYSTIETO_MAA);

        assertThat(yhteystietoArvo).isEmpty();
    }

    @Test
    public void getYhteystietoArvoWithSort() {
        List<YhteystiedotRyhmaDto> yhteystietoRyhmat = Arrays.asList(
                YhteystiedotRyhmaDto.builder()
                        .ryhmaKuvaus("ryhmaKuvaus1")
                        .yhteystieto(YhteystietoDto.builder()
                                .yhteystietoTyyppi(YhteystietoTyyppi.YHTEYSTIETO_KUNTA)
                                .yhteystietoArvo("arvo1")
                                .build())
                        .build(),
                YhteystiedotRyhmaDto.builder()
                        .ryhmaKuvaus("ryhmaKuvaus2")
                        .yhteystieto(YhteystietoDto.builder()
                                .yhteystietoTyyppi(YhteystietoTyyppi.YHTEYSTIETO_KUNTA)
                                .yhteystietoArvo("arvo2")
                                .build())
                        .build(),
                YhteystiedotRyhmaDto.builder()
                        .ryhmaKuvaus("ryhmaKuvaus3")
                        .yhteystieto(YhteystietoDto.builder()
                                .yhteystietoTyyppi(YhteystietoTyyppi.YHTEYSTIETO_KUNTA)
                                .yhteystietoArvo("")
                                .build())
                        .build()
        );

        assertSoftly(softly -> {
            softly.assertThat(YhteystietoUtil.getYhteystietoArvo(yhteystietoRyhmat,
                    YhteystietoTyyppi.YHTEYSTIETO_KUNTA, "ryhmaKuvaus4", "ryhmaKuvaus2", "ryhmaKuvaus5")).hasValue("arvo2");
            softly.assertThat(YhteystietoUtil.getYhteystietoArvo(yhteystietoRyhmat,
                    YhteystietoTyyppi.YHTEYSTIETO_KUNTA, "ryhmaKuvaus3", "ryhmaKuvaus1", "ryhmaKuvaus2")).hasValue("arvo1");
        });
    }

}
