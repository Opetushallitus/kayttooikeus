package fi.vm.sade.kayttooikeus.repositories.impl;

import com.querydsl.core.types.Projections;
import fi.vm.sade.kayttooikeus.dto.PalveluDto;
import fi.vm.sade.kayttooikeus.model.Palvelu;
import fi.vm.sade.kayttooikeus.repositories.PalveluRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import static fi.vm.sade.kayttooikeus.model.QPalvelu.palvelu;

@Repository
public class PalveluRepositoryImpl extends AbstractRepository implements PalveluRepository {
    @Override
    public List<PalveluDto> findAll() {
        return jpa().from(palvelu).select(Projections.bean(PalveluDto.class,
                palvelu.id.as("id"),
                palvelu.name.as("name"),
                palvelu.palveluTyyppi.as("palveluTyyppi"),
                palvelu.description.id.as("descriptionId"),
                palvelu.kokoelma.id.as("kokolemaId")
            )).orderBy(palvelu.name.asc()).fetch();
    }
}
