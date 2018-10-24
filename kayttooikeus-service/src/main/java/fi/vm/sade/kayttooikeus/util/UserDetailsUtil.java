package fi.vm.sade.kayttooikeus.util;

import com.google.common.collect.Lists;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoTyyppi;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public class UserDetailsUtil {

    private static final String DEFAULT_LANGUAGE_CODE = "fi";

    public static String getCurrentUserOid() throws NullPointerException {
        String oid = SecurityContextHolder.getContext().getAuthentication().getName();
        if (oid == null) {
            throw new NullPointerException("No user name available from SecurityContext!");
        }
        return oid;
    }

    /**
     * Returns name for given {@link HenkiloDto}.
     *
     * @param henkilo
     * @return name
     */
    public static String getName(HenkiloDto henkilo) {
        return henkilo.getKutsumanimi() + " " + henkilo.getSukunimi();
    }

    /**
     * Palauttaa {@link HenkiloDto henkilön} asiointikielen (jos ei löydy niin
     * palautetaan {@link #DEFAULT_LANGUAGE_CODE}).
     *
     * @param henkilo henkilö
     * @param kielikoodit sallitut kielikoodit (tyhjä hyväksyy kaikki)
     * @return kielikoodi
     */
    public static String getLanguageCode(HenkiloDto henkilo, String... kielikoodit) {
        List<String> kielikoodilista = Arrays.asList(kielikoodit);
        return ofNullable(henkilo.getAsiointiKieli())
                .flatMap(kielisyys -> ofNullable(kielisyys.getKieliKoodi()))
                .filter(kielikoodi -> kielikoodilista.isEmpty() || kielikoodilista.contains(kielikoodi))
                .orElse(DEFAULT_LANGUAGE_CODE);
    }

    /**
     * Palauttaa {@link HenkiloPerustietoDto henkilön} asiointikielen (jos ei
     * löydy niin palautetaan {@link #DEFAULT_LANGUAGE_CODE}).
     *
     * @param henkilo henkilö
     * @param kielikoodit sallitut kielikoodit
     * @return kielikoodi
     */
    public static String getLanguageCode(HenkiloPerustietoDto henkilo, String... kielikoodit) {
        List<String> kielikoodilista = Arrays.asList(kielikoodit);
        return ofNullable(henkilo.getAsiointiKieli())
                .flatMap(kielisyys -> ofNullable(kielisyys.getKieliKoodi()))
                .filter(kielikoodi -> kielikoodilista.isEmpty() || kielikoodilista.contains(kielikoodi))
                .orElse(DEFAULT_LANGUAGE_CODE);
    }

    /**
     * Emails are parsed and preferred using YhteystiedotComparator
     * 1. work email
     * 2. home email
     * 3. other email
     * 4. free time email
     * @param henkilo
     * @return First email by priority
     */
    public static Optional<String> getEmailByPriority(HenkiloDto henkilo) {
        YhteystiedotComparator yhteystiedotComparator = new YhteystiedotComparator();
        return henkilo.getYhteystiedotRyhma().stream()
                .sorted(yhteystiedotComparator)
                .flatMap(yhteystiedotRyhmaDto -> yhteystiedotRyhmaDto.getYhteystieto().stream())
                .filter(yhteystietoDto -> yhteystietoDto.getYhteystietoTyyppi().equals(YhteystietoTyyppi.YHTEYSTIETO_SAHKOPOSTI)
                        && !StringUtils.isEmpty(yhteystietoDto.getYhteystietoArvo()))

                .map(YhteystietoDto::getYhteystietoArvo).findFirst();
    }

    public static OrganisaatioPerustieto createUnknownOrganisation(String organisaatioOid) {
        return new OrganisaatioPerustieto().toBuilder()
                .oid(organisaatioOid)
                .nimi(new HashMap<String, String>() {{
                    put("fi", "Tuntematon organisaatio");
                    put("sv", "Okänd organisation");
                    put("en", "Unknown organisation");
                }})
                .organisaatiotyypit(Lists.newArrayList())
                .build();
    }
}
