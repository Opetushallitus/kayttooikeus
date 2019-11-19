package fi.vm.sade.kayttooikeus.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class OrganisaatioHenkiloWithOrganisaatioDto extends OrganisaatioHenkiloDto {
    private OrganisaatioWithChildrenDto organisaatio;
    
    @Builder(builderMethodName = "organisaatioBuilder")
    public OrganisaatioHenkiloWithOrganisaatioDto(long id, String organisaatioOid,
                                                  OrganisaatioHenkiloTyyppi tyyppi,
                                                  String tehtavanimike, boolean passivoitu,
                                                  LocalDate voimassaAlkuPvm, LocalDate voimassaLoppuPvm,
                                                  OrganisaatioWithChildrenDto organisaatio) {
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
            organisaatio = new OrganisaatioWithChildrenDto();
        }
        organisaatio.setOid(oid);
    }

}
