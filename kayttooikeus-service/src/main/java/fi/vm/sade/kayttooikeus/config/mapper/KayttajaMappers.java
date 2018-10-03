package fi.vm.sade.kayttooikeus.config.mapper;

import fi.vm.sade.kayttooikeus.dto.KayttajaCriteriaDto;
import fi.vm.sade.kayttooikeus.repositories.criteria.OrganisaatioHenkiloCriteria;
import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.metadata.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Configuration
public class KayttajaMappers {

    @Bean
    public CustomConverter<KayttajaCriteriaDto, OrganisaatioHenkiloCriteria> kayttajaCriteriaDtoOrganisaatioHenkiloCriteriaConverter() {
        return new CustomConverter<KayttajaCriteriaDto, OrganisaatioHenkiloCriteria>() {
            @Override
            public OrganisaatioHenkiloCriteria convert(KayttajaCriteriaDto source, Type<? extends OrganisaatioHenkiloCriteria> destinationType, MappingContext mappingContext) {
                OrganisaatioHenkiloCriteria destination = new OrganisaatioHenkiloCriteria();
                destination.setKayttajaTyyppi(source.getKayttajaTyyppi());
                destination.setPassivoitu(source.getPassivoitu());
                destination.setOrganisaatioOids(source.getOrganisaatioOids());
                destination.setKayttooikeudet(Optional.ofNullable(source.getKayttooikeudet())
                        .map(kayttooikeudet -> kayttooikeudet.entrySet().stream()
                                .flatMap(entry -> entry.getValue().stream().map(value -> entry.getKey() + "_" + value))
                                .collect(toList())).orElse(null));
                return destination;
            }
        };
    }

}
