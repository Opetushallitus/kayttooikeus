package fi.vm.sade.kayttooikeus.config.mapper;

import fi.vm.sade.kayttooikeus.dto.Asiointikieli;
import fi.vm.sade.kayttooikeus.dto.KutsuReadDto;
import fi.vm.sade.kayttooikeus.model.Kutsu;
import fi.vm.sade.kayttooikeus.service.impl.email.SahkopostiHenkiloDto;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class KutsuMappers {

    @Bean
    public CustomMapper<Kutsu, SahkopostiHenkiloDto> sahkopostiHenkiloDtoClassMap() {
        return new CustomMapper<Kutsu, SahkopostiHenkiloDto>() {
            @Override
            public void mapAtoB(Kutsu a, SahkopostiHenkiloDto b, MappingContext context) {
                b.setEtunimet(a.getEtunimi());
                b.setKutsumanimi(a.getEtunimi());
                b.setSukunimi(a.getSukunimi());
            }
        };
    }

    @Bean
    public CustomMapper<Kutsu, KutsuReadDto> kutsuKutsuReadDtoCustomMapper() {
        return new CustomMapper<Kutsu, KutsuReadDto>() {
            @Override
            public void mapAtoB(Kutsu kutsu, KutsuReadDto kutsuReadDto, MappingContext context) {
                super.mapAtoB(kutsu, kutsuReadDto, context);
                kutsuReadDto.setKutsujaOid(kutsu.getKutsuja());
                kutsuReadDto.setAsiointikieli(Asiointikieli.valueOf(kutsu.getKieliKoodi()));
                kutsuReadDto.setHakaIdentifier(StringUtils.hasLength(kutsu.getHakaIdentifier()));
            }
            @Override
            public void mapBtoA(KutsuReadDto kutsuReadDto, Kutsu kutsu, MappingContext context) {
                super.mapBtoA(kutsuReadDto, kutsu, context);
                kutsu.setKutsuja(kutsuReadDto.getKutsujaOid());
                kutsu.setKieliKoodi(kutsuReadDto.getAsiointikieli().name());
            }
        };
    }

}
