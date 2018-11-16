package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.dto.EmailVerificationResponseDto;
import fi.vm.sade.kayttooikeus.dto.enumeration.LoginTokenValidationCode;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.TunnistusToken;
import fi.vm.sade.kayttooikeus.repositories.TunnistusTokenDataRepository;
import fi.vm.sade.kayttooikeus.service.EmailVerificationService;
import fi.vm.sade.kayttooikeus.service.IdentificationService;
import fi.vm.sade.kayttooikeus.service.exception.LoginTokenException;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloUpdateDto;
import fi.vm.sade.properties.OphProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static fi.vm.sade.kayttooikeus.model.Identification.CAS_AUTHENTICATION_IDP;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final TunnistusTokenDataRepository tunnistusTokenDataRepository;
    private final OphProperties ophProperties;
    private final OppijanumerorekisteriClient oppijanumerorekisteriClient;
    private final IdentificationService identificationService;

    @Override
    @Transactional
    public EmailVerificationResponseDto emailVerification(HenkiloUpdateDto henkiloUpdateDto, String loginToken) {
        TunnistusToken tunnistusToken = tunnistusTokenDataRepository.findByValidLoginToken(loginToken)
                .orElseThrow(() -> new NotFoundException(String.format("Logintoken %s on vanhentunut tai sitä ei löydy", loginToken)));

        if(tunnistusToken.getKaytetty() != null){
            throw new LoginTokenException(String.format("Login token %s on jo käytetty", loginToken));
        }

        Henkilo henkilo = tunnistusToken.getHenkilo();

        oppijanumerorekisteriClient.updateHenkilo(henkiloUpdateDto);
        henkilo.setSahkopostivarmennusAikaleima(LocalDateTime.now());

        String authToken = identificationService.consumeLoginToken(tunnistusToken.getLoginToken(), CAS_AUTHENTICATION_IDP);
        return EmailVerificationResponseDto.builder()
                .authToken(authToken)
                .service(ophProperties.url("virkailijan-tyopoyta"))
                .build();
    }

    @Override
    @Transactional
    public EmailVerificationResponseDto redirectUrlByLoginToken(String loginToken) {
        TunnistusToken tunnistusToken = tunnistusTokenDataRepository.findByValidLoginToken(loginToken)
                .orElseThrow(() -> new NotFoundException(String.format("Login tokenia %s ei löydy tai se on vanhentunut", loginToken)));
        if(tunnistusToken.getKaytetty() != null){
            throw new LoginTokenException(String.format("Login token %s on jo käytetty", loginToken));
        }
        String authToken = identificationService.consumeLoginToken(tunnistusToken.getLoginToken(), CAS_AUTHENTICATION_IDP);
        return EmailVerificationResponseDto.builder()
                .authToken(authToken)
                .service(ophProperties.url("virkailijan-tyopoyta"))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public HenkiloDto getHenkiloByLoginToken(String loginToken) {
        TunnistusToken tunnistusToken = tunnistusTokenDataRepository.findByLoginToken(loginToken)
                .orElseThrow(() -> new NotFoundException(String.format("Login tokenia %s ei löytynyt", loginToken)));
        String oid = tunnistusToken.getHenkilo().getOidHenkilo();
        return this.oppijanumerorekisteriClient.getHenkiloByOid(oid);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginTokenValidationCode getLoginTokenValidationCode(String loginToken) {
        TunnistusToken tunnistusToken = tunnistusTokenDataRepository.findByValidLoginToken(loginToken)
                .orElse(null);

        if(tunnistusToken == null) {
            log.error(String.format("Logintoken %s on vanhentunut tai sitä ei löydy", loginToken));
            return LoginTokenValidationCode.TOKEN_VANHENTUNUT_TAI_EI_LOYDY;
        }

        if(tunnistusToken.getKaytetty() != null) {
            log.error(String.format("Logintoken %s on jo käytetty", loginToken));
            return LoginTokenValidationCode.TOKEN_KAYTETTY;
        }

        return LoginTokenValidationCode.TOKEN_OK;
    }

}
