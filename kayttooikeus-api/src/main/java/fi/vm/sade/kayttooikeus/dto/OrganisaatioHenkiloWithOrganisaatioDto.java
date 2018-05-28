package fi.vm.sade.kayttooikeus.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.vm.sade.kayttooikeus.dto.enumeration.OrganisaatioStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class OrganisaatioHenkiloWithOrganisaatioDto extends OrganisaatioHenkiloDto {
    private OrganisaatioDto organisaatio;
    
    @Builder(builderMethodName = "organisaatioBuilder")
    public OrganisaatioHenkiloWithOrganisaatioDto(long id, String organisaatioOid,
                                                  OrganisaatioHenkiloTyyppi tyyppi,
                                                  String tehtavanimike, boolean passivoitu,
                                                  LocalDate voimassaAlkuPvm, LocalDate voimassaLoppuPvm,
                                                  OrganisaatioDto organisaatio) {
        super(id, organisaatioOid, tyyppi, tehtavanimike, passivoitu, voimassaAlkuPvm, voimassaLoppuPvm);
        this.organisaatio = organisaatio;
    }

    @Override
    @JsonIgnore
    public OrganisaatioHenkiloTyyppi getOrganisaatioHenkiloTyyppi() {
        return super.getOrganisaatioHenkiloTyyppi();
    }

    public OrganisaatioHenkiloTyyppi getTyyppi() {
        return getOrganisaatioHenkiloTyyppi();
    }

    @Override
    @JsonIgnore
    public String getOrganisaatioOid() {
        return super.getOrganisaatioOid();
    }

    public void setOrganisaatioOid(String oid) {
        super.setOrganisaatioOid(oid);
        if (organisaatio == null) {
            organisaatio = new OrganisaatioDto();
        }
        organisaatio.setOid(oid);
    }
    
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganisaatioDto {
        private String oid;
        private String parentOidPath;
        private TextGroupMapDto nimi;
        private List<String> tyypit;
        private List<OrganisaatioDto> children;
        private OrganisaatioStatus status;
    }
}
