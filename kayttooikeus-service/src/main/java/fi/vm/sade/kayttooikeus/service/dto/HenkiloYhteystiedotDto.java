package fi.vm.sade.kayttooikeus.service.dto;

import fi.vm.sade.oppijanumerorekisteri.dto.YhteystiedotRyhmaDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HenkiloYhteystiedotDto {

    private String oidHenkilo;
    private String etunimet;
    private String kutsumanimi;
    private String sukunimi;
    private String asiointikieli;
    private List<YhteystiedotRyhmaDto> yhteystiedotRyhma;

}
