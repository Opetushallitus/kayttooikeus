package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.dto.VahvaTunnistusRequestDto;
import fi.vm.sade.kayttooikeus.dto.VahvaTunnistusResponseDto;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import static fi.vm.sade.kayttooikeus.model.Identification.STRONG_AUTHENTICATION_IDP;
import fi.vm.sade.kayttooikeus.model.TunnistusToken;
import fi.vm.sade.kayttooikeus.service.IdentificationService;
import fi.vm.sade.kayttooikeus.service.KayttajatiedotService;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService;
import fi.vm.sade.kayttooikeus.service.VahvaTunnistusService;
import fi.vm.sade.kayttooikeus.service.dto.HenkiloVahvaTunnistusDto;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.properties.OphProperties;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
@RequiredArgsConstructor
public class VahvaTunnistusServiceImpl implements VahvaTunnistusService {

    private final IdentificationService identificationService;
    private final KayttajatiedotService kayttajatiedotService;

    private final OppijanumerorekisteriClient oppijanumerorekisteriClient;
    private final OphProperties ophProperties;

    @Override
    public VahvaTunnistusResponseDto tunnistaudu(String loginToken, VahvaTunnistusRequestDto lisatiedotDto) {
        TunnistusToken tunnistusToken = identificationService.consumeLoginToken(loginToken);
        return tunnistaudu(tunnistusToken, lisatiedotDto);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public VahvaTunnistusResponseDto tunnistauduIlmanTransaktiota(String loginToken, VahvaTunnistusRequestDto lisatiedotDto) {
        TunnistusToken tunnistusToken = identificationService.consumeLoginToken(loginToken);
        return tunnistaudu(tunnistusToken, lisatiedotDto);
    }

    public VahvaTunnistusResponseDto tunnistaudu(TunnistusToken tunnistusToken, VahvaTunnistusRequestDto lisatiedotDto) {
        Henkilo henkiloByLoginToken = tunnistusToken.getHenkilo();
        String henkiloOid = henkiloByLoginToken.getOidHenkilo();

        HenkiloVahvaTunnistusDto vahvaTunnistusDto = new HenkiloVahvaTunnistusDto(tunnistusToken.getHetu());
        Optional.ofNullable(lisatiedotDto.getTyosahkopostiosoite())
                .filter(StringUtils::hasLength)
                .ifPresent(vahvaTunnistusDto::setTyosahkopostiosoite);
        oppijanumerorekisteriClient.setStrongIdentifiedHetu(henkiloOid, vahvaTunnistusDto);

        Optional.ofNullable(lisatiedotDto.getSalasana())
                .filter(StringUtils::hasLength)
                .ifPresent(salasana -> kayttajatiedotService.changePasswordAsAdmin(henkiloOid, salasana, LdapSynchronizationService.LdapSynchronizationType.NOW));

        String authToken = identificationService.generateAuthTokenForHenkilo(henkiloOid, STRONG_AUTHENTICATION_IDP,
                tunnistusToken.getHenkilo().getKayttajatiedot().getUsername());

        return VahvaTunnistusResponseDto.builder()
                .authToken(authToken)
                .service(ophProperties.url("virkailijan-tyopoyta"))
                .build();
    }

}
