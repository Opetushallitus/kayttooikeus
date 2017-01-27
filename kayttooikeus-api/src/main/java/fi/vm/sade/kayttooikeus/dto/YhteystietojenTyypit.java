package fi.vm.sade.kayttooikeus.dto;

/**
 * Käyttöoikeuden käyttämät koodit koodistosta "yhteystietotyypit".
 */
public final class YhteystietojenTyypit {

    private YhteystietojenTyypit() {
    }

    public static final String KOTIOSOITE = "yhteystietotyyppi1";
    public static final String TYOOSOITE = "yhteystietotyyppi2";
    public static final String VAPAA_AJAN_OSOITE = "yhteystietotyyppi3";
    public static final String MUU_OSOITE = "yhteystietotyyppi7";

    public static final String[] PRIORITY_ORDER = {
        TYOOSOITE, KOTIOSOITE, MUU_OSOITE, VAPAA_AJAN_OSOITE
    };

}
