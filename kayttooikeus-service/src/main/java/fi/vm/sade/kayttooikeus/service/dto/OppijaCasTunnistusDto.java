package fi.vm.sade.kayttooikeus.service.dto;

public class OppijaCasTunnistusDto {

    public final String hetu;
    public final String sukunimi;
    public final String etunimet;

    public OppijaCasTunnistusDto(String hetu, String sukunimi, String etunimet) {
        assert(hetu != null);
        assert(sukunimi != null);
        assert(etunimet != null);
        this.hetu = hetu;
        this.sukunimi = sukunimi;
        this.etunimet = etunimet;
    }
}
