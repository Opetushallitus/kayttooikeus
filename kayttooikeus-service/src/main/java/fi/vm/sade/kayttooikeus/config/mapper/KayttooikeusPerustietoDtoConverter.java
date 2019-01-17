package fi.vm.sade.kayttooikeus.config.mapper;

import fi.vm.sade.kayttooikeus.dto.KayttooikeusPerustiedotDto;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;
import fi.vm.sade.kayttooikeus.model.MyonnettyKayttoOikeusRyhmaTapahtuma;
import fi.vm.sade.kayttooikeus.model.Palvelu;
import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.metadata.Type;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class KayttooikeusPerustietoDtoConverter extends CustomConverter<Henkilo, KayttooikeusPerustiedotDto> {
    @Override
    public KayttooikeusPerustiedotDto convert(Henkilo source, Type<? extends KayttooikeusPerustiedotDto> destinationType, MappingContext mappingContext) {
        Set<KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto> organisaatiot = Optional.ofNullable(source.getOrganisaatioHenkilos()).orElseGet(HashSet::new).stream()
                .filter(Objects::nonNull)
                .filter(organisaatioHenkilo -> !organisaatioHenkilo.isPassivoitu())
                .map(organisaatioHenkilo -> KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto.builder()
                        .organisaatioOid(organisaatioHenkilo.getOrganisaatioOid())
                        .kayttooikeudet(Optional.ofNullable(organisaatioHenkilo.getMyonnettyKayttoOikeusRyhmas()).orElseGet(HashSet::new).stream()
                                .filter(Objects::nonNull)
                                .map(MyonnettyKayttoOikeusRyhmaTapahtuma::getKayttoOikeusRyhma)
                                .filter(Objects::nonNull)
                                .filter(kayttoOikeusRyhma -> !kayttoOikeusRyhma.isPassivoitu())
                                .flatMap(kayttoOikeusRyhma -> Optional.ofNullable(kayttoOikeusRyhma.getKayttoOikeus()).orElseGet(HashSet::new).stream())
                                .filter(Objects::nonNull)
                                .map(kayttoOikeus -> KayttooikeusPerustiedotDto.KayttooikeusOrganisaatiotDto.KayttooikeusOikeudetDto.builder()
                                        .oikeus(kayttoOikeus.getRooli())
                                        .palvelu(Optional.ofNullable(kayttoOikeus.getPalvelu()).map(Palvelu::getName).orElse(null))
                                .build())
                                .collect(Collectors.toSet()))
                        .build())
                .collect(Collectors.toSet());
        String username = Optional.ofNullable(source.getKayttajatiedot())
                .map(Kayttajatiedot::getUsername)
                .orElse(null);
        return KayttooikeusPerustiedotDto.builder()
                .oidHenkilo(source.getOidHenkilo())
                .username(username)
                .kayttajaTyyppi(source.getKayttajaTyyppi())
                .organisaatiot(organisaatiot)
                .build();
    }
}
