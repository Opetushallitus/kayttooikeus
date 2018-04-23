package fi.vm.sade.kayttooikeus.controller;

import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.service.HenkiloService;
import fi.vm.sade.kayttooikeus.service.IdentificationService;
import fi.vm.sade.kayttooikeus.service.exception.*;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.properties.OphProperties;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import fi.vm.sade.kayttooikeus.model.TunnistusToken;
import fi.vm.sade.kayttooikeus.service.VahvaTunnistusService;
import fi.vm.sade.kayttooikeus.util.HenkiloUtils;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoTyyppi;
import java.util.Optional;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/cas")
@Api(tags = "CAS:a varten olevat rajapinnat.")
@RequiredArgsConstructor
public class CasController {

    private final IdentificationService identificationService;
    private final HenkiloService henkiloService;
    private final VahvaTunnistusService vahvaTunnistusService;

    private final OppijanumerorekisteriClient oppijanumerorekisteriClient;

    private final OphProperties ophProperties;

    @ApiOperation(value = "Generoi autentikointitokenin henkilölle.",
            notes = "Generoi tokenin CAS autentikointia varten henkilölle annettujen IdP tunnisteiden pohjalta.")
    @PreAuthorize("hasAnyRole('ROLE_APP_HENKILONHALLINTA_READ',"
            + "'ROLE_APP_HENKILONHALLINTA_READ_UPDATE',"
            + "'ROLE_APP_HENKILONHALLINTA_CRUD',"
            + "'ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA',"
            + "'ROLE_APP_HENKILONHALLINTA_OPHREKISTERI')")
    @RequestMapping(value = "/auth/oid/{oid}", method = RequestMethod.GET)
    public String generateAuthTokenForHenkilo(@PathVariable("oid") String oid,
                                              @RequestParam("idpkey") String idpKey,
                                              @RequestParam("idpid") String idpIdentifier) {
        return identificationService.generateAuthTokenForHenkilo(oid, idpKey, idpIdentifier);
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_HENKILONHALLINTA_READ',"
            + "'ROLE_APP_HENKILONHALLINTA_READ_UPDATE',"
            + "'ROLE_APP_HENKILONHALLINTA_CRUD',"
            + "'ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA',"
            + "'ROLE_APP_HENKILONHALLINTA_OPHREKISTERI')")
    @ApiOperation(value = "Hakee henkilön OID:n autentikaation perusteella.",
            notes = "Hakee henkilön OID:n annettujen IdP tunnisteiden perusteella.")
    @RequestMapping(value = "/auth/idp/{idpkey}", method = RequestMethod.GET)
    public String getHenkiloOidByIdPAndIdentifier(@PathVariable("idpkey") String idpKey,
                                                  @RequestParam("idpid") String idpIdentifier) {
        return identificationService.getHenkiloOidByIdpAndIdentifier(idpKey, idpIdentifier);
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA', 'ROLE_APP_HENKILONHALLINTA_OPHREKISTERI')")
    @ApiOperation("Palauttaa tiedon henkilön aiemmasta vahvasta tunnistautumisesta")
    @RequestMapping(value = "/auth/henkilo/{oidHenkilo}/vahvastiTunnistettu", method = RequestMethod.GET)
    public boolean isVahvastiTunnistettu(@PathVariable String oidHenkilo) {
        return this.henkiloService.isVahvastiTunnistettu(oidHenkilo);
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA', 'ROLE_APP_HENKILONHALLINTA_OPHREKISTERI')")
    @ApiOperation("Palauttaa tiedon henkilön aiemmasta vahvasta tunnistautumisesta")
    @RequestMapping(value = "/auth/henkilo/username/{username}/vahvastiTunnistettu", method = RequestMethod.GET)
    public boolean isVahvastiTunnistettuByUsername(@PathVariable String username) {
        return this.henkiloService.isVahvastiTunnistettuByUsername(username);
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA', 'ROLE_APP_HENKILONHALLINTA_OPHREKISTERI')")
    @ApiOperation("Luo tilapäisen tokenin henkilön vahvan tunnistaumisen ajaksi")
    @RequestMapping(value = "/auth/henkilo/{oidHenkilo}/loginToken", method = RequestMethod.GET)
    public String createLoginToken(@PathVariable String oidHenkilo, @RequestParam(required = false) Boolean salasananVaihto) {
        return this.identificationService.createLoginToken(oidHenkilo, salasananVaihto);
    }

    // Palomuurilla rajoitettu pääsy vain verkon sisältä
    @ApiOperation(value = "Hakee henkilön identiteetitiedot.",
            notes = "Hakee henkilön identieettitiedot annetun autentikointitokenin avulla ja invalidoi autentikointitokenin.")
    @RequestMapping(value = "/auth/token/{token}", method = RequestMethod.GET)
    public IdentifiedHenkiloTypeDto getIdentityByAuthToken(@PathVariable("token") String authToken) throws IOException {
        return identificationService.findByTokenAndInvalidateToken(authToken);
    }

    // Palomuurilla rajoitettu pääsy vain verkon sisältä
    @ApiOperation(value = "Luo tai päivittää henkilön identiteetitiedot ja palauttaa kertakäyttöisen autentikointitokenin.",
            notes = "Luo tai päivittää henkilön identiteetitiedot hetun mukaan ja palauttaa kertakäyttöisen autentikointitokenin.")
    @RequestMapping(value = "/henkilo/{hetu}", method = RequestMethod.GET)
    public String updateIdentificationAndGenerateTokenForHenkiloByHetu(@PathVariable("hetu") String hetu) throws IOException {
        return identificationService.updateIdentificationAndGenerateTokenForHenkiloByHetu(hetu);
    }

    // Palomuurilla rajoitettu pääsy vain verkon sisältä
    @ApiOperation(value = "Virkailijan hetu-tunnistuksen jälkeinen käsittely. (rekisteröinti, hetu tunnistuksen pakotus, " +
            "mahdollinen kirjautuminen suomi.fi:n kautta.)")
    @RequestMapping(value = "/tunnistus", method = RequestMethod.GET)
    public void requestGet(HttpServletResponse response,
                           @RequestParam(value="loginToken", required = false) String loginToken,
                           @RequestParam(value="kutsuToken", required = false) String kutsuToken,
                           @RequestParam(value = "kielisyys", required = false) String kielisyys,
                           @RequestHeader(value = "nationalidentificationnumber", required = false) String hetu,
                           @RequestHeader(value = "firstname", required = false) String etunimet,
                           @RequestHeader(value = "sn", required = false) String sukunimi) throws IOException {
        // Vaihdetaan kutsuToken väliaikaiseen ja tallennetaan tiedot vetumasta
        Map<String, String> queryParams;
        if (StringUtils.hasLength(kutsuToken)) {
            try {
                // Dekoodataan etunimet ja sukunimi manuaalisesti, koska shibboleth välittää ASCII-enkoodatut request headerit UTF-8 -merkistössä
                Charset windows1252 = Charset.forName("Windows-1252");
                Charset utf8 = Charset.forName("UTF-8");
                etunimet = new String(etunimet.getBytes(windows1252), utf8);
                sukunimi = new String(sukunimi.getBytes(windows1252), utf8);

                String temporaryKutsuToken = this.identificationService
                        .updateKutsuAndGenerateTemporaryKutsuToken(kutsuToken, hetu, etunimet, sukunimi);
                queryParams = new HashMap<String, String>() {{
                    put("temporaryKutsuToken", temporaryKutsuToken);
                }};
                response.sendRedirect(this.ophProperties.url("henkilo-ui.rekisteroidy", queryParams));
            } catch (NotFoundException e) {
                response.sendRedirect(this.ophProperties.url("henkilo-ui.vahvatunnistus.virhe", kielisyys, "vanhakutsu"));
            }
        }
        // Kirjataan henkilön vahva tunnistautuminen järjestelmään, vaihe 1
        else if (StringUtils.hasLength(loginToken)) {
            try {
                // otetaan hetu talteen jotta se on vielä tiedossa seuraavassa vaiheessa
                TunnistusToken tunnistusToken = identificationService.updateLoginToken(loginToken, hetu);
                HenkiloDto henkiloByLoginToken = oppijanumerorekisteriClient.getHenkiloByOid(tunnistusToken.getHenkilo().getOidHenkilo());
                if(tunnistusToken.getHenkilo().getKayttajaTyyppi().equals(KayttajaTyyppi.PALVELU)) {
                    throw new PalvelukayttajaLoginException("Palvelukäyttäjänä kirjautuminen on estetty");
                }

                // tarkistetaan että virkailijalla on tämä hetu käytössä
                Optional.ofNullable(henkiloByLoginToken.getHetu()).ifPresent(tallennettuHetu -> {
                    if (!tallennettuHetu.equals(hetu)) {
                        throw new HetuVaaraException(String.format("Vahvan tunnistuksen henkilötunnus %s on eri kuin virkailijan henkilötunnus %s", hetu, tallennettuHetu));
                    }
                });

                boolean sahkopostinAsetus = !HenkiloUtils
                        .getYhteystieto(henkiloByLoginToken, YhteystietojenTyypit.TYOOSOITE, YhteystietoTyyppi.YHTEYSTIETO_SAHKOPOSTI)
                        .isPresent();
                boolean salasananVaihto = Boolean.TRUE.equals(tunnistusToken.getSalasananVaihto());
                if (sahkopostinAsetus || salasananVaihto) {
                    // pyydetään käyttäjää täydentämään tietoja ("uudelleenrekisteröinti")
                    response.sendRedirect(ophProperties.url("henkilo-ui.uudelleenrekisterointi", kielisyys, loginToken, sahkopostinAsetus, salasananVaihto));
                } else {
                    // jos mitään tietoja ei tarvitse täyttää, suoritetaan tunnistautuminen ilman rekisteröintisivua
                    VahvaTunnistusRequestDto vahvaTunnistusRequestDto = new VahvaTunnistusRequestDto();
                    VahvaTunnistusResponseDto vahvaTunnistusResponseDto = tunnistauduVahvasti(kielisyys, loginToken, vahvaTunnistusRequestDto);
                    response.sendRedirect(ophProperties.url("cas.login", vahvaTunnistusResponseDto.asMap()));
                }
            } catch (LoginTokenNotFoundException e) {
                response.sendRedirect(this.ophProperties.url("henkilo-ui.vahvatunnistus.virhe", kielisyys, "vanha"));
            } catch (HetuVaaraException e) {
                response.sendRedirect(this.ophProperties.url("henkilo-ui.vahvatunnistus.virhe", kielisyys, "vaara"));
            } catch(PalvelukayttajaLoginException e) {
                response.sendRedirect(this.ophProperties.url("henkilo-ui.vahvatunnistus.virhe", kielisyys, "palvelukayttaja"));
            } catch (Exception e) {
                log.warn("User failed strong identification", e);
                response.sendRedirect(this.ophProperties.url("henkilo-ui.vahvatunnistus.virhe", kielisyys, loginToken));
            }
        }
        // Tarkista että vaaditut tokenit ja tiedot löytyvät (riippuen casesta) -> Error sivu
        else {
            throw new UnsupportedOperationException("Provide loginToken or kutsuToken");
        }
    }

    @PostMapping("/uudelleenrekisterointi")
    @ApiOperation(value = "Virkailijan uudelleenrekisteröinti")
    public VahvaTunnistusResponseDto tunnistauduVahvasti(
            @RequestParam(value = "kielisyys") String kielisyys,
            @RequestParam(value = "loginToken") String loginToken,
            @RequestBody @Valid VahvaTunnistusRequestDto dto) throws IOException {
        // Kirjataan henkilön vahva tunnistautuminen järjestelmään, vaihe 2
        return vahvaTunnistusService.tunnistauduIlmanTransaktiota(loginToken, dto);
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
    @RequestMapping(value = "/prequel", method = RequestMethod.POST)
    public ResponseEntity<String> requestPost() {
        return new ResponseEntity<>("ok", HttpStatus.OK);
    }
}
