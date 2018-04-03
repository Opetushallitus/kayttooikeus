package fi.vm.sade.kayttooikeus.config.mapper;

import fi.vm.sade.kayttooikeus.dto.PalvelukayttajaReadDto;
import fi.vm.sade.kayttooikeus.repositories.dto.HenkilohakuResultDto;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PalvelukayttajaMappers {

    @Bean
    public CustomMapper<HenkilohakuResultDto, PalvelukayttajaReadDto> palvelukayttajaReadMapper() {
        return new CustomMapper<HenkilohakuResultDto, PalvelukayttajaReadDto>() {
            @Override
            public void mapAtoB(HenkilohakuResultDto a, PalvelukayttajaReadDto b, MappingContext context) {
                b.setOid(a.getOidHenkilo());
                b.setNimi(a.getSukunimi());
            }
        };
    }

}
