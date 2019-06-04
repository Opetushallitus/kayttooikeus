package fi.vm.sade.kayttooikeus.config.mapper;

import fi.vm.sade.kayttooikeus.dto.KutsuCreateDto;
import fi.vm.sade.kayttooikeus.model.KayttoOikeusRyhma;
import fi.vm.sade.kayttooikeus.repositories.KayttoOikeusRyhmaRepository;
import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.metadata.Type;
import org.springframework.stereotype.Component;

@Component
public class KutsuCreateKayttoOikeusRyhmaConverter extends CustomConverter<KutsuCreateDto.KutsuKayttoOikeusRyhmaCreateDto, KayttoOikeusRyhma> {

    private final KayttoOikeusRyhmaRepository kayttoOikeusRyhmaRepository;

    public KutsuCreateKayttoOikeusRyhmaConverter(KayttoOikeusRyhmaRepository kayttoOikeusRyhmaRepository) {
        this.kayttoOikeusRyhmaRepository = kayttoOikeusRyhmaRepository;
    }

    @Override
    public KayttoOikeusRyhma convert(KutsuCreateDto.KutsuKayttoOikeusRyhmaCreateDto source, Type<? extends KayttoOikeusRyhma> destinationType, MappingContext mappingContext) {
        return kayttoOikeusRyhmaRepository.findById(source.getId())
                .orElseThrow(() -> new IllegalArgumentException("KayttoOikeusRyhma not found with id " + source.getId()));
    }

}
