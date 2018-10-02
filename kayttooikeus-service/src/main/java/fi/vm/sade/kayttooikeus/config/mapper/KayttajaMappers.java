package fi.vm.sade.kayttooikeus.config.mapper;

import fi.vm.sade.kayttooikeus.dto.KayttajaCriteriaDto;
import fi.vm.sade.kayttooikeus.repositories.criteria.OrganisaatioHenkiloCriteria;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.stream.Collectors.toSet;

@Configuration
public class KayttajaMappers {

    @Bean
    public CustomMapper<KayttajaCriteriaDto, OrganisaatioHenkiloCriteria> kayttajaCriteriaDtoOrganisaatioHenkiloCriteriaMapper() {
        return new CustomMapper<KayttajaCriteriaDto, OrganisaatioHenkiloCriteria>() {
            @Override
            public void mapAtoB(KayttajaCriteriaDto a, OrganisaatioHenkiloCriteria b, MappingContext context) {
                b.setKayttooikeudet(a.getKayttooikeudet() == null ? null : a.getKayttooikeudet().entrySet().stream()
                        .flatMap(entry -> entry.getValue().stream().map(value -> entry.getKey() + "_" + value))
                        .collect(toSet()));
            }

            @Override
            public void mapBtoA(OrganisaatioHenkiloCriteria b, KayttajaCriteriaDto a, MappingContext context) {
                throw new UnsupportedOperationException("Not implemented yet.");
            }
        };
    }

}
