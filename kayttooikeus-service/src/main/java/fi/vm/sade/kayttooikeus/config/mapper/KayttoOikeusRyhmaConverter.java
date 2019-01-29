package fi.vm.sade.kayttooikeus.config.mapper;

import fi.vm.sade.kayttooikeus.dto.KayttoOikeusRyhmaDto;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioViiteDto;
import fi.vm.sade.kayttooikeus.dto.TextGroupDto;
import fi.vm.sade.kayttooikeus.model.KayttoOikeusRyhma;
import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.metadata.Type;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class KayttoOikeusRyhmaConverter extends CustomConverter<KayttoOikeusRyhma, KayttoOikeusRyhmaDto> {

    @Override
    public KayttoOikeusRyhmaDto convert(KayttoOikeusRyhma source, Type<? extends KayttoOikeusRyhmaDto> destinationType, MappingContext mappingContext) {
        KayttoOikeusRyhmaDto.KayttoOikeusRyhmaDtoBuilder destinationBuilder = KayttoOikeusRyhmaDto.builder()
                .id(source.getId())
                .tunniste(source.getTunniste())
                .rooliRajoite(source.getRooliRajoite())
                .nimi(new TextGroupDto(source.getNimi().getId()))
                .ryhmaRestriction(source.isRyhmaRestriction())
                .passivoitu(source.isPassivoitu())
                .organisaatioViite(source.getOrganisaatioViite().stream().map(organisaatioViite -> OrganisaatioViiteDto.builder()
                        .id(organisaatioViite.getId())
                        .organisaatioTyyppi(organisaatioViite.getOrganisaatioTyyppi())
                        .build()).collect(Collectors.toList()))
                .sallittuKayttajatyyppi(source.getSallittuKayttajatyyppi());

        if (source.getKuvaus() != null) {
            destinationBuilder.kuvaus(new TextGroupDto(source.getKuvaus().getId()));
        }

        return destinationBuilder.build();
    }

}
