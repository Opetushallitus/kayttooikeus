package fi.vm.sade.kayttooikeus.controller;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fi.vm.sade.kayttooikeus.dto.GoogleAuthSetupDto;
import fi.vm.sade.kayttooikeus.service.IdentificationService;
import fi.vm.sade.kayttooikeus.service.MfaService;
import fi.vm.sade.kayttooikeus.service.exception.ForbiddenException;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;

import static fi.vm.sade.kayttooikeus.model.Identification.STRONG_AUTHENTICATION_IDP;

@RestController
@RequestMapping(value = "/mfasetup", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "/mfasetup", tags = "MFA setup API")
@RequiredArgsConstructor
public class MfaSetupController {
    private final MfaService mfaService;
    private final IdentificationService identificationService;

    private void validateSuomiFi() {
        String idpEntityId = identificationService.getIdpEntityIdForCurrentSession();
        if (!idpEntityId.equals(STRONG_AUTHENTICATION_IDP)) {
            throw new ForbiddenException("suomifi required");
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/gauth/setup")
    public GoogleAuthSetupDto setupMfa() {
        validateSuomiFi();
        return mfaService.setupGoogleAuth();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/gauth/enable")
    public boolean setupMfa(@RequestBody String token) {
        validateSuomiFi();
        return mfaService.enableGoogleAuth(token);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/gauth/disable")
    public boolean disableMfa() {
        validateSuomiFi();
        return mfaService.disableGoogleAuth();
    }
}
