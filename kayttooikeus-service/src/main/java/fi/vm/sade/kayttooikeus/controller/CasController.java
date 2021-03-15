package fi.vm.sade.kayttooikeus.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.vm.sade.kayttooikeus.config.security.casoppija.SuomiFiAuthenticationDetails;
import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.dto.enumeration.LogInRedirectType;
import fi.vm.sade.kayttooikeus.dto.enumeration.LoginTokenValidationCode;
import fi.vm.sade.kayttooikeus.service.EmailVerificationService;
import fi.vm.sade.kayttooikeus.service.HenkiloService;
import fi.vm.sade.kayttooikeus.service.IdentificationService;
import fi.vm.sade.kayttooikeus.service.VahvaTunnistusService;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloUpdateDto;
import fi.vm.sade.properties.OphProperties;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/cas", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@Api(tags = "CAS:a varten olevat rajapinnat.")
@RequiredArgsConstructor
public class CasController {

    private final IdentificationService identificationService;
    private final HenkiloService henkiloService;
    private final VahvaTunnistusService vahvaTunnistusService;
    private final EmailVerificationService emailVerificationService;
    private final OphProperties ophProperties;

    @ApiOperation(value = "Generoi autentikointitokenin henkilölle.",
            notes = "Generoi tokenin CAS autentikointia varten henkilölle annettujen IdP tunnisteiden pohjalta.")
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @RequestMapping(value = "/auth/oid/{oid}", method = RequestMethod.GET)
    public String generateAuthTokenForHenkilo(@PathVariable("oid") String oid,
                                              @RequestParam("idpkey") String idpKey,
                                              @RequestParam("idpid") String idpIdentifier) {
        return identificationService.generateAuthTokenForHenkilo(oid, idpKey, idpIdentifier);
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation(value = "Hakee henkilön OID:n autentikaation perusteella.",
            notes = "Hakee henkilön OID:n annettujen IdP tunnisteiden perusteella.")
    @RequestMapping(value = "/auth/idp/{idpkey}", method = RequestMethod.GET)
    public String getHenkiloOidByIdPAndIdentifier(@PathVariable("idpkey") String idpKey,
                                                  @RequestParam("idpid") String idpIdentifier) {
        return identificationService.getHenkiloOidByIdpAndIdentifier(idpKey, idpIdentifier);
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation("Palauttaa tiedon henkilön aiemmasta vahvasta tunnistautumisesta")
    @RequestMapping(value = "/auth/henkilo/{oidHenkilo}/vahvastiTunnistettu", method = RequestMethod.GET)
    public boolean isVahvastiTunnistettu(@PathVariable String oidHenkilo) {
        return this.henkiloService.isVahvastiTunnistettu(oidHenkilo);
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation("Palauttaa tiedon henkilön aiemmasta vahvasta tunnistautumisesta")
    @RequestMapping(value = "/auth/henkilo/username/{username}/vahvastiTunnistettu", method = RequestMethod.GET)
    public boolean isVahvastiTunnistettuByUsername(@PathVariable String username) {
        return this.henkiloService.isVahvastiTunnistettuByUsername(username);
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation("Palauttaa uri:n johon käyttäjä tulee ohjata kirjautumisen yhteydessä, tai null jos uudelleenohjausta ei tarvita")
    @RequestMapping(value = "/auth/henkilo/{oidHenkilo}/logInRedirect", method = RequestMethod.GET)
    public LogInRedirectType logInRedirectByOidHenkilo(@PathVariable("oidHenkilo") String oidHenkilo) {
        return this.henkiloService.logInRedirectByOidhenkilo(oidHenkilo);
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation("Palauttaa uri:n johon käyttäjä tulee ohjata kirjautumisen yhteydessä, tai null jos uudelleenohjausta ei tarvita")
    @RequestMapping(value = "/auth/henkilo/username/{username}/logInRedirect", method = RequestMethod.GET)
    public LogInRedirectType logInRedirectByUsername(@PathVariable("username") String username) {
        return this.henkiloService.logInRedirectByUsername(username);
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation("Luo tilapäisen tokenin henkilön vahvan tunnistaumisen ajaksi")
    @RequestMapping(value = "/auth/henkilo/{oidHenkilo}/loginToken", method = RequestMethod.GET)
    public String createLoginToken(@PathVariable String oidHenkilo, @RequestParam(required = false) Boolean salasananVaihto) {
        return this.identificationService.createLoginToken(oidHenkilo, salasananVaihto, null);
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation(value = "Hakee henkilön identiteetitiedot.",
            notes = "Hakee henkilön identieettitiedot annetun autentikointitokenin avulla ja invalidoi autentikointitokenin.")
    @RequestMapping(value = "/auth/token/{token}", method = RequestMethod.GET)
    public IdentifiedHenkiloTypeDto getIdentityByAuthToken(@PathVariable("token") String authToken) {
        return identificationService.findByTokenAndInvalidateToken(authToken);
    }

    @ApiOperation(value = "Virkailijan hetu-tunnistuksen jälkeinen käsittely. (rekisteröinti, hetu tunnistuksen pakotus, " +
            "mahdollinen kirjautuminen suomi.fi:n kautta.)")
    @RequestMapping(value = "/tunnistus", method = RequestMethod.GET)
    public void requestGet(HttpServletRequest request, HttpServletResponse response,
                           Principal principal,
                           @RequestParam(value="loginToken", required = false) String loginToken,
                           @RequestParam(value="kutsuToken", required = false) String kutsuToken,
                           @RequestParam(value = "locale", required = false) String kielisyys)
            throws IOException {
        assert(principal != null);
        assert(principal instanceof PreAuthenticatedAuthenticationToken);
        PreAuthenticatedAuthenticationToken token = (PreAuthenticatedAuthenticationToken) principal;
        SuomiFiAuthenticationDetails details =
                (SuomiFiAuthenticationDetails) token.getDetails();
        // kirjataan ulos, jotta virkailija-CAS ei hämmenny
        handleOppijaLogout(request, response);
        if (StringUtils.hasLength(kutsuToken)) {
            // Vaihdetaan kutsuToken väliaikaiseen ja tallennetaan tiedot vetumasta
            response.sendRedirect(vahvaTunnistusService.kasitteleKutsunTunnistus(
                    kutsuToken, kielisyys, details.hetu,
                    details.etunimet, details.sukunimi));
        } else if (StringUtils.hasLength(loginToken)) {
            // Kirjataan henkilön vahva tunnistautuminen järjestelmään, vaihe 1
            // Joko päästetään suoraan sisään tai käytetään lisätietojen keräyssivun kautta
            String redirectUrl = getVahvaTunnistusRedirectUrl(loginToken, kielisyys, details.hetu);
            response.sendRedirect(redirectUrl);
        } else {
            response.sendRedirect(
                    vahvaTunnistusService.kirjaaKayttajaVahvallaTunnistuksella(details.hetu, kielisyys));
        }
    }

    private String getVahvaTunnistusRedirectUrl(String loginToken, String kielisyys, String hetu) {
        try {
            return vahvaTunnistusService.kirjaaVahvaTunnistus(loginToken, kielisyys, hetu);
        } catch (Exception e) {
            log.error("User failed strong identification", e);
            return ophProperties.url("henkilo-ui.vahvatunnistus.virhe", kielisyys, loginToken);
        }
    }

    @PostMapping(value = "/uudelleenrekisterointi", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Virkailijan uudelleenrekisteröinti")
    public VahvaTunnistusResponseDto tunnistauduVahvasti(
            @RequestParam(value = "kielisyys") String kielisyys,
            @RequestParam(value = "loginToken") String loginToken,
            @RequestBody @Valid VahvaTunnistusRequestDto dto) {
        // Kirjataan henkilön vahva tunnistautuminen järjestelmään, vaihe 2
        return vahvaTunnistusService.tunnistaudu(loginToken, dto);
    }

    @ApiOperation(value = "Auttaa CAS session avaamisessa käyttöoikeuspalveluun.",
            notes = "Jos kutsuja haluaa tehdä useita rinnakkaisia kutsuja eikä CAS sessiota ole vielä avattu, " +
                    "täytyy tätä kutsua ensin.",
            authorizations = @Authorization("login"),
            response = ResponseEntity.class)
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/prequel", method = RequestMethod.GET)
    public ResponseEntity<String> requestGet() {
        return new ResponseEntity<>("ok", HttpStatus.OK);
    }

    @ApiOperation(value = "Auttaa CAS session avaamisessa käyttöoikeuspalveluun.",
            notes = "Jos kutsuja haluaa tehdä useita rinnakkaisia kutsuja eikä CAS sessiota ole vielä avattu, " +
                    "täytyy tätä kutsua ensin.",
            authorizations = @Authorization("login"),
            response = ResponseEntity.class)
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/prequel", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> requestPost() {
        return new ResponseEntity<>("ok", HttpStatus.OK);
    }

    @PostMapping(value = "/emailverification/{loginToken}", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation("Asettaa käyttäjän sähköpostiosoitteet vahvistetuksi")
    public EmailVerificationResponseDto emailVerification(@RequestBody @Validated HenkiloUpdateDto henkiloUpdate,
                                                          @PathVariable String loginToken) {
        return this.emailVerificationService.emailVerification(henkiloUpdate, loginToken);
    }

    @GetMapping(value = "/emailverification/loginTokenValidation/{loginToken}")
    @ApiOperation(value = "Palauttaa validatointikoodin loginTokenille",
            notes = "Validointikoodista käyttöliittymässä tiedetään täytyykö käyttäjälle näyttää virhesivu")
    public LoginTokenValidationCode getLoginTokenValidationCode(@PathVariable String loginToken) {
        return this.emailVerificationService.getLoginTokenValidationCode(loginToken);
    }

    @GetMapping(value = "/emailverification/redirectByLoginToken/{loginToken}")
    @ApiOperation("Palauttaa uudelleenohjausurlin loginTokenin perusteella.")
    public EmailVerificationResponseDto getFrontPageRedirectByLoginToken(@PathVariable String loginToken) {
        return this.emailVerificationService.redirectUrlByLoginToken(loginToken);
    }

    @GetMapping(value = "/henkilo/loginToken/{loginToken}")
    @ApiOperation("Hakee käyttäjän tiedot loginTokenin perusteella")
    public HenkiloDto getUserByLoginToken(@PathVariable("loginToken") String loginToken) {
        return this.emailVerificationService.getHenkiloByLoginToken(loginToken);
    }

    @ApiOperation(value = "Deprekoitu CAS palvelusta siirretty rajapinta",
            notes = "Deprekoitu. Käytä /henkilo/current/omattiedot ja oppijanumerorekisterin /henkilo/current/omattiedot" +
                    "rajapintoja.",
            authorizations = @Authorization("login"),
            response = ResponseEntity.class)
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/me", method = RequestMethod.GET)
    public MeDto getMe() throws JsonProcessingException {
        return this.henkiloService.getMe();
    }

    @ApiOperation(value = "Deprekoitu CAS palvelusta siirretty rajapinta",
            notes = "Deprekoitu. Käytä /henkilo/current/omattiedot rajapintaa.",
            authorizations = @Authorization("login"),
            response = ResponseEntity.class)
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/myroles", method = RequestMethod.GET)
    public List<String> getMyroles() {
        return this.henkiloService.getMyRoles();
    }

    private void handleOppijaLogout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null){
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        SecurityContextHolder.getContext().setAuthentication(null);
    }
}
