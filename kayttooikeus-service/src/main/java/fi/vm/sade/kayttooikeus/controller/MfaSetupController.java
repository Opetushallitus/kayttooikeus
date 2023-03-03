package fi.vm.sade.kayttooikeus.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fi.vm.sade.kayttooikeus.config.security.casoppija.SuomiFiAuthenticationDetails;
import fi.vm.sade.kayttooikeus.dto.GoogleAuthSetupDto;
import fi.vm.sade.kayttooikeus.service.MfaService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = "/mfasetup", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "/mfasetup", tags = "MFA setup API")
@RequiredArgsConstructor
public class MfaSetupController {
    private final MfaService mfaService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/gauth/setup")
    public GoogleAuthSetupDto setupMfa() {
      return mfaService.setupGoogleAuth();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/gauth/enable")
    public boolean setupMfa(@RequestBody String token) {
      return mfaService.enableGoogleAuth(token);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/gauth/testsuomifi")
    public String isSuomiFi(HttpServletRequest request) {
      Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      try {
        if (principal instanceof SuomiFiAuthenticationDetails) {
          log.warn("suomifi");
          log.warn(((SuomiFiAuthenticationDetails)principal).sukunimi);
        }
        if (principal instanceof WebAuthenticationDetails) {
          log.warn("webauth");
          log.warn(((WebAuthenticationDetails)principal).getSessionId());
        }
        if (principal instanceof UserDetails) {
          log.warn("uesrdetails");
          log.warn(((UserDetails)principal).getUsername());
        }
        
        log.warn("can cast");
        log.warn(((SuomiFiAuthenticationDetails)principal).sukunimi);
        log.warn(((UserDetails)principal).getUsername());
      } catch (Exception e) {
        log.warn("failed to cast", e);
      }
      log.warn("suomifi from session: " + request.getSession().getAttribute("SUOMIFI_SESSION"));
      return principal.getClass().getName();
    }
}
