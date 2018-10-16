package fi.vm.sade.kayttooikeus.controller;

import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.ExternalPermissionService;
import fi.vm.sade.kayttooikeus.enumeration.OrderByHenkilohaku;
import fi.vm.sade.kayttooikeus.repositories.criteria.OrganisaatioHenkiloCriteria;
import fi.vm.sade.kayttooikeus.repositories.dto.HenkilohakuResultDto;
import fi.vm.sade.kayttooikeus.service.HenkiloService;
import fi.vm.sade.kayttooikeus.service.IdentificationService;
import fi.vm.sade.kayttooikeus.service.KayttajatiedotService;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService.LdapSynchronizationType;
import fi.vm.sade.kayttooikeus.service.OrganisaatioHenkiloService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/henkilo")
@Api(tags = "Henkilöön liittyvät operaatiot")
@RequiredArgsConstructor
public class HenkiloController {

    private final OrganisaatioHenkiloService organisaatioHenkiloService;
    private final HenkiloService henkiloService;
    private final KayttajatiedotService kayttajatiedotService;
    private final IdentificationService identificationService;

    @GetMapping("/{oid}")
    @PreAuthorize("@permissionCheckerServiceImpl.isAllowedToAccessPersonOrSelf(#oid, {'HENKILONHALLINTA': {'READ', 'READ_UPDATE', 'CRUD'}, 'KAYTTOOIKEUS': {'PALVELUKAYTTAJA_CRUD'}}, null)")
    @ApiOperation("Hakee henkilön OID:n perusteella")
    public HenkiloReadDto getByOid(@PathVariable String oid) {
        return henkiloService.getByOid(oid);
    }

    @GetMapping("/kayttajatunnus={kayttajatunnus}")
    @PostAuthorize("@permissionCheckerServiceImpl.isAllowedToAccessPersonOrSelf(returnObject.oid, {'HENKILONHALLINTA': {'READ', 'READ_UPDATE', 'CRUD'}, 'KAYTTOOIKEUS': {'READ', 'CRUD', 'PALVELUKAYTTAJA_CRUD'}}, null)")
    @ApiOperation("Hakee henkilön käyttäjätunnuksen perusteella")
    public HenkiloReadDto getByKayttajatunnus(@PathVariable String kayttajatunnus) {
        return henkiloService.getByKayttajatunnus(kayttajatunnus);
    }

    @GetMapping("/{oid}/linkitykset")
    @PreAuthorize("@permissionCheckerServiceImpl.isAllowedToAccessPersonOrSelf(#oid, {'HENKILONHALLINTA': {'READ', 'READ_UPDATE', 'CRUD'}, 'KAYTTOOIKEUS': {'PALVELUKAYTTAJA_CRUD'}}, null)")
    @ApiOperation("Henkilön linkitystiedot")
    public HenkiloLinkitysDto getLinkitykset(@PathVariable String oid, @RequestParam(defaultValue = "false") boolean showPassive) {
        return this.henkiloService.getLinkitykset(oid, showPassive);
    }

    @PreAuthorize("@permissionCheckerServiceImpl.isAllowedToAccessPersonOrSelf(#oid, {'HENKILONHALLINTA': {'READ', 'READ_UPDATE', 'CRUD'}, 'KAYTTOOIKEUS': {'READ', 'CRUD', 'PALVELUKAYTTAJA_CRUD'}}, #permissionService)")
    @ApiOperation(value = "Listaa henkilön aktiiviset organisaatiot (organisaatiohenkilöt) organisaatioiden tai " +
            "ryhmien tiedoilla rekursiivisesti.",
            notes = "Hakee annetun henkilön aktiiviset ja suunnitellut organisaatiohenkilöt organisaation tai ryhmän tiedoilla siten, " +
                    "että organisaatio sisältää myös lapsiorganisaationsa rekursiivisesti. Oletuksena haetaan myös ne organisaatiot, joihin ei ole voimassa olevia käyttöoikeuksia. ")
    @RequestMapping(value = "/{oid}/organisaatio", method = RequestMethod.GET)
    public List<OrganisaatioHenkiloWithOrganisaatioDto> listOrganisatioHenkilos(
            @PathVariable @ApiParam(value = "Henkilö-OID", required = true) String oid,
            @RequestParam(required = false, defaultValue = "fi") @ApiParam("Organisaatioiden järjestyksen kielikoodi (oletus fi)") String comparisonLangCode,
            @RequestParam(required = false) @ApiParam("Ylimääräinen suodatus, jolla mahdollisten organisaatiohenkilöiden tuloslistaa rajataan.") PalveluRooliGroup requiredRoles,
            @RequestHeader(value = "External-Permission-Service", required = false)
                    ExternalPermissionService permissionService) {
        return organisaatioHenkiloService.listOrganisaatioHenkilos(oid, comparisonLangCode, requiredRoles);
    }

    @PreAuthorize("@permissionCheckerServiceImpl.isAllowedToAccessPersonOrSelf(#henkiloOid, {'HENKILONHALLINTA': {'READ', 'READ_UPDATE', 'CRUD'}, 'KAYTTOOIKEUS': {'READ', 'CRUD', 'PALVELUKAYTTAJA_CRUD'}}, #permissionService)")
    @ApiOperation(value = "Listaa henkilön organisaatiot.",
            notes = "Hakee annetun henkilön kaikki organisaatiohenkilöt.")
    @RequestMapping(value = "/{oid}/organisaatiohenkilo", method = RequestMethod.GET)
    public List<OrganisaatioHenkiloDto> listOrganisaatioHenkilos(@PathVariable("oid") String henkiloOid,
                                                                 @RequestHeader(value = "External-Permission-Service", required = false)
                                                                         ExternalPermissionService permissionService) {
        return organisaatioHenkiloService.findOrganisaatioByHenkilo(henkiloOid);
    }

    @PreAuthorize("@permissionCheckerServiceImpl.isAllowedToAccessPerson(#henkiloOid, {'HENKILONHALLINTA': {'READ', 'READ_UPDATE', 'CRUD'}, 'KAYTTOOIKEUS': {'READ', 'CRUD', 'PALVELUKAYTTAJA_CRUD'}}, null)")
    @ApiOperation(value = "Hakee henkilön yhden organisaation tiedot.",
            notes = "Hakee henkilön yhden organisaatiohenkilön tiedot.")
    @RequestMapping(value = "/{oid}/organisaatiohenkilo/{organisaatioOid}", method = RequestMethod.GET)
    public OrganisaatioHenkiloDto findByOrganisaatioOid(@PathVariable("oid") String henkiloOid,
                                                        @PathVariable("organisaatioOid") String organisaatioOid) {
        return organisaatioHenkiloService.findOrganisaatioHenkiloByHenkiloAndOrganisaatio(henkiloOid, organisaatioOid);
    }

    @PreAuthorize("@permissionCheckerServiceImpl.isAllowedToAccessPerson(#henkiloOid, {'HENKILONHALLINTA': {'CRUD'}, 'KAYTTOOIKEUS': {'CRUD', 'PALVELUKAYTTAJA_CRUD'}}, null)")
    @ApiOperation(value = "Luo henkilön käyttäjätiedot.",
            notes = "Luo henkilön käyttäjätiedot.")
    @RequestMapping(value = "/{oid}/kayttajatiedot", method = RequestMethod.POST)
    public KayttajatiedotReadDto createKayttajatiedot(@PathVariable("oid") String henkiloOid,
                                                      @RequestBody @Validated KayttajatiedotCreateDto kayttajatiedot,
                                                      @RequestParam(required = false, defaultValue = "ASAP") LdapSynchronizationType ldapSynchronization) {
        return kayttajatiedotService.create(henkiloOid, kayttajatiedot, ldapSynchronization);
    }

    @PreAuthorize("@permissionCheckerServiceImpl.isAllowedToAccessPersonOrSelf(#henkiloOid, {'HENKILONHALLINTA': {'READ', 'READ_UPDATE', 'CRUD'}, 'KAYTTOOIKEUS': {'READ', 'CRUD', 'PALVELUKAYTTAJA_CRUD'}}, null)")
    @ApiOperation(value = "Hakee henkilön käyttäjätiedot.",
            notes = "Hakee henkilön käyttäjätiedot.")
    @RequestMapping(value = "/{oid}/kayttajatiedot", method = RequestMethod.GET)
    public KayttajatiedotReadDto getKayttajatiedot(@PathVariable("oid") String henkiloOid) {
        return kayttajatiedotService.getByHenkiloOid(henkiloOid);
    }

    @PreAuthorize("@permissionCheckerServiceImpl.isAllowedToAccessPerson(#henkiloOid, {'HENKILONHALLINTA': {'READ_UPDATE', 'CRUD'}, 'KAYTTOOIKEUS': {'CRUD', 'PALVELUKAYTTAJA_CRUD'}}, null)")
    @ApiOperation(value = "Päivittää henkilön käyttäjätiedot.", notes = "Päivittää henkilön käyttäjätiedot. Virkailija voi itse vaihtaa käyttäjätietojaan, "
    + "rekisterinpitäjä ei.")
    @RequestMapping(value = "/{oid}/kayttajatiedot", method = RequestMethod.PUT)
    public KayttajatiedotReadDto updateKayttajatiedot(@PathVariable("oid") String henkiloOid,
                                                        @RequestBody @Validated KayttajatiedotUpdateDto kayttajatiedot) {
        return kayttajatiedotService.updateKayttajatiedot(henkiloOid, kayttajatiedot);
    }

    @PreAuthorize("@permissionCheckerServiceImpl.isAllowedToAccessPersonOrSelf(#henkiloOid, {'HENKILONHALLINTA': {'CRUD'}, 'KAYTTOOIKEUS': {'CRUD', 'PALVELUKAYTTAJA_CRUD'}}, null)")
    @RequestMapping(value = "/{henkiloOid}/password", method = RequestMethod.POST)
    @ApiOperation(value = "Asettaa henkilön salasanan.",
            notes = "Asettaa henkilölle uuden salasanan virkailijan "
                    + "toimesta, ei tee tarkistusta vanhalle salasanalle "
                    + "vaan yliajaa suoraan uudella.",
            authorizations = {@Authorization("ROLE_APP_HENKILONHALLINTA_CRUD"),
                    @Authorization("ROLE_APP_HENKILONHALLINTA_OPHREKISTERI")})
    public void setPassword( @ApiParam(value = "Henkilön OID", required = true) @PathVariable("henkiloOid") String henkiloOid,
                                 @ApiParam(value = "Format: \"password\"", required = true) @RequestBody String password) {
            this.kayttajatiedotService.changePasswordAsAdmin(henkiloOid, password);
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA', 'ROLE_APP_HENKILONHALLINTA_OPHREKISTERI')")
    @RequestMapping(value = "/{henkiloOid}/passivoi", method = RequestMethod.DELETE)
    @ApiOperation(value = "Passivoi henkilön kaikki organisaatiot ja käyttöoikeudet.",
            notes = "Passivoi henkilön kaikki organisaatiot ja käyttöoikeudet. Kutsutaan oppijanumerorekisterin henkilön" +
                    "passivoinnin yhteydessä automaattisesti.",
            authorizations = {@Authorization("ROLE_APP_HENKILONHALLINTA_OPHREKISTERI")})
    public void passivoi(@ApiParam(value = "Henkilön OID", required = true) @PathVariable(value = "henkiloOid") String henkiloOid,
                         @ApiParam(value = "Jos ei annettu käytetään kirjautunutta")
                         @RequestParam(value = "kasittelijaOid", required = false) String kasittelijaOid) {
        this.henkiloService.passivoi(henkiloOid, kasittelijaOid);
    }

    @PreAuthorize("@permissionCheckerServiceImpl.isAllowedToAccessPerson(#oid, {'HENKILONHALLINTA': {'CRUD', 'KKVASTUU'}, 'KAYTTOOIKEUS': {'CRUD', 'PALVELUKAYTTAJA_CRUD'}}, #permissionService)")
    @RequestMapping(value = "/{oid}/hakatunnus", method = RequestMethod.GET)
    @ApiOperation(value = "Hakee henkilön Haka-tunnisteet.",
            notes = "Hakee annetun henkilön Haka-tunnisteet.",
            authorizations = @Authorization("ROLE_APP_HENKILONHALLINTA_CRUD, " +
                    "ROLE_APP_HENKILONHALLINTA_KKVASTUU, " +
                    "ROLE_APP_HENKILONHALLINTA_OPHREKISTERI"),
            response = Set.class)
    public Set<String> getHenkilosHakaTunnisteet(@PathVariable("oid") @ApiParam("Henkilön OID") String oid,
                                                 @RequestHeader(value = "External-Permission-Service", required = false)
                                                                  ExternalPermissionService permissionService) {
        return identificationService.getHakatunnuksetByHenkiloAndIdp(oid);
    }

    @PreAuthorize("@permissionCheckerServiceImpl.isAllowedToAccessPerson(#oid, {'HENKILONHALLINTA': {'CRUD', 'KKVASTUU'}, 'KAYTTOOIKEUS': {'CRUD', 'PALVELUKAYTTAJA_CRUD'}}, #permissionService)")
    @RequestMapping(value = "/{oid}/hakatunnus", method = RequestMethod.PUT)
    @ApiOperation(value = "Päivittää henkilön Haka-tunnisteet. ",
            notes = "Päivittää annetun henkilön Haka-tunnisteet.",
            authorizations = @Authorization("ROLE_APP_HENKILONHALLINTA_CRUD, "
                    + "ROLE_APP_HENKILONHALLINTA_KKVASTUU, "
                    + "ROLE_APP_HENKILONHALLINTA_OPHREKISTERI"),
            response = Set.class)
    public Set<String> updateHenkilosHakaTunnisteet(@PathVariable("oid") @ApiParam("Henkilön OID") String oid,
                                                 @RequestBody Set<String> hakatunnisteet,
                                                 @RequestHeader(value = "External-Permission-Service", required = false)
                                                                  ExternalPermissionService permissionService) {
        return identificationService.updateHakatunnuksetByHenkiloAndIdp(oid, hakatunnisteet);

    }

    @GetMapping("/{oid}/kayttooikeudet")
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA', 'ROLE_APP_HENKILONHALLINTA_OPHREKISTERI')")
    @ApiOperation("Palauttaa henkilöiden oid:t joiden tietoihin annetulla henkilöllä on oikeutus")
    public KayttooikeudetDto getKayttooikeudet(@PathVariable String oid, OrganisaatioHenkiloCriteria criteria) {
        return henkiloService.getKayttooikeudet(oid, criteria);
    }

    @PostMapping("/{oid}/kayttooikeudet")
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA', 'ROLE_APP_HENKILONHALLINTA_OPHREKISTERI')")
    @ApiOperation("Palauttaa henkilöiden oid:t joiden tietoihin annetulla henkilöllä on oikeutus")
    public KayttooikeudetDto postKayttooikeudet(@PathVariable String oid, @RequestBody OrganisaatioHenkiloCriteria criteria) {
        return henkiloService.getKayttooikeudet(oid, criteria);
    }

    @PutMapping("/{oid}/ldap")
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_SCHEDULE',"
            + "'ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA',"
            + "'ROLE_APP_HENKILONHALLINTA_OPHREKISTERI')")
    @ApiOperation("Lisää henkilön LDAP-synkronointijonoon")
    public void updateHenkiloToLdap(@PathVariable String oid,
            @RequestParam(required = false, defaultValue = "NORMAL") LdapSynchronizationType ldapSynchronization) {
        henkiloService.updateHenkiloToLdap(oid, ldapSynchronization);
    }

    @PostMapping("/henkilohaku")
    @PreAuthorize("isAuthenticated()")
    @ApiOperation(value = "UI:ta varten tehty mahdollisesti HIDAS hakurajapinta. EI tarkoitettu palveluiden käyttöön. Muutosaltis.",
            notes = "Palauttaa suppean setin henkilöiden tietoja annetuilla hakukriteereillä. Toimii eri tavalla eri käyttäjäryhmille! " +
                    "(rekisterinpitäjä, OPH:n virkaiilja, muu virkailija) Hakua rajoitetaan näille ryhmille joten ei tarvitse " +
                    "erillisiä käyttöoikeuksia. Hakutuloksen maksimikoko saattaa olla 100 tai 101 käyttäjätunnuksella " +
                    "haun takia.")
    public Collection<HenkilohakuResultDto> henkilohaku(@Validated @RequestBody HenkilohakuCriteriaDto henkilohakuCriteriaDto,
                                                        @RequestParam(defaultValue = "0") Long offset,
                                                        @RequestParam(required = false) OrderByHenkilohaku orderBy) {
        return this.henkiloService.henkilohaku(henkilohakuCriteriaDto, offset, orderBy);
    }

    @PostMapping("/henkilohakucount")
    @PreAuthorize("isAuthenticated()")
    @ApiOperation(value = "UI:ta varten tehty mahdollisesti HIDAS hakurajapinta palauttaa henkilohaun tulosten lukumäärän ilman sivutusrajoitusta",
            notes = "Palauttaa annetuilla rajoitteilla löytyvän henkilöjoukon koon")
    public Long henkilohakuCount(@Validated @RequestBody HenkilohakuCriteriaDto henkilohakuCriteriaDto ) {
        return this.henkiloService.henkilohakuCount(henkilohakuCriteriaDto);
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/current/omattiedot", method = RequestMethod.GET)
    @ApiOperation("Palauttaa henkilön tiedot käyttöoikeuspalvelun näkökulmasta")
    public OmatTiedotDto omatTiedot() {
        return this.henkiloService.getOmatTiedot();
    }

    @PutMapping("/{oid}/anomusilmoitus")
    @ApiOperation("Anomusilmoitus asetuksen muuttaminen")
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA',"
            + "'ROLE_APP_HENKILONHALLINTA_OPHREKISTERI')")
    public void updateAnomusilmoitus(@PathVariable String oid, @RequestBody boolean anomusilmoitus) {
        this.henkiloService.updateAnomusilmoitus(oid, anomusilmoitus);
    }

}