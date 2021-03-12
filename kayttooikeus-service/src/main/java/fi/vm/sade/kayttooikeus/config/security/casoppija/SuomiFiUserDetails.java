package fi.vm.sade.kayttooikeus.config.security.casoppija;

import java.io.Serializable;

public class SuomiFiUserDetails implements Serializable {

    public final String hetu;
    public final String sukunimi;
    public final String etunimet;

    public SuomiFiUserDetails(String hetu, String sukunimi, String etunimet) {
        this.hetu = hetu;
        this.sukunimi = sukunimi;
        this.etunimet = etunimet;
    }

}
