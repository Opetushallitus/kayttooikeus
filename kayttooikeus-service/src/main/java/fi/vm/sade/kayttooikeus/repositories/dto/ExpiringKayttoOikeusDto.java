package fi.vm.sade.kayttooikeus.repositories.dto;

import fi.vm.sade.kayttooikeus.dto.Localizable;
import fi.vm.sade.kayttooikeus.dto.LocalizableDto;
import fi.vm.sade.kayttooikeus.dto.TextGroupDto;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.stream.Stream;

import static fi.vm.sade.kayttooikeus.dto.TextGroupDto.localizeLaterById;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpiringKayttoOikeusDto implements Serializable, LocalizableDto {
    private String henkiloOid;
    private Long kayttoOikeusRyhmaId;
    private Long myonnettyTapahtumaId;
    private LocalDate voimassaLoppuPvm;
    private String ryhmaName;
    private TextGroupDto ryhmaDescription;
    
    public void setRyhmaDescriptionId(Long ryhmaDescriptionId) {
        this.ryhmaDescription = localizeLaterById(ryhmaDescriptionId);
    }

    @Override
    public Stream<Localizable> localizableTexts() {
        return LocalizableDto.of(ryhmaDescription).localizableTexts();
    }
}
