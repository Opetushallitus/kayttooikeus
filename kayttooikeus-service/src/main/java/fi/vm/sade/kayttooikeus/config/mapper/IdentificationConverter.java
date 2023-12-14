package fi.vm.sade.kayttooikeus.config.mapper;

import fi.vm.sade.kayttooikeus.dto.IdentifiedHenkiloTypeDto;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotReadDto;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.Identification;
import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.metadata.Type;
import org.springframework.stereotype.Component;

@Component
public class IdentificationConverter extends CustomConverter<Identification, IdentifiedHenkiloTypeDto> {

    @Override
    public IdentifiedHenkiloTypeDto convert(Identification identification,
            Type<? extends IdentifiedHenkiloTypeDto> destinationType, MappingContext mappingContext) {
        Henkilo henkilo = identification.getHenkilo();
        return IdentifiedHenkiloTypeDto.builder()
                .oidHenkilo(henkilo.getOidHenkilo())
                .henkiloTyyppi(henkilo.getKayttajaTyyppi())
                .kayttajatiedot(
                        henkilo.getKayttajatiedot() != null
                                ? new KayttajatiedotReadDto(
                                        henkilo.getKayttajatiedot().getUsername(),
                                        henkilo.getKayttajatiedot().getMfaProvider(),
                                        // Tässä on vähän hassusti käyttäjätyyppi tuplana, mutta /userDetails palauttaa vain KayttajatiedotReadDto objektin
                                        // ja siinäkin tarvitaan sama tieto
                                        henkilo.getKayttajaTyyppi()
                                    )
                                : null)
                .idpEntityId(identification.getIdpEntityId())
                .build();
    }

}
