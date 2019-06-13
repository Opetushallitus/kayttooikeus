package fi.vm.sade.kayttooikeus.controller;

import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.service.KayttoOikeusService;
import fi.vm.sade.kayttooikeus.util.UserDetailsUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/kayttooikeusryhma", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@Api(value = "/kayttooikeusryhma", description = "Käyttöoikeusryhmien käsittelyyn liittyvät operaatiot.")
public class KayttoOikeusRyhmaController {
    private KayttoOikeusService kayttoOikeusService;

    private static final Logger logger = LoggerFactory.getLogger(KayttoOikeusRyhmaController.class);

    @Autowired
    public KayttoOikeusRyhmaController(KayttoOikeusService kayttoOikeusService) {
        this.kayttoOikeusService = kayttoOikeusService;
    }

    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    @ApiOperation(value = "Listaa kaikki käyttöoikeusryhmät.",
            notes = "Listaa kaikki käyttöoikeusryhmät, jotka on tallennettu henkilöhallintaan.")
    public List<KayttoOikeusRyhmaDto> listKayttoOikeusRyhma(@RequestParam(required = false) Boolean passiiviset) {
        return kayttoOikeusService.listAllKayttoOikeusRyhmas(passiiviset);
    }

    @ApiOperation(value = "Hakee henkilön käyttöoikeusryhmät organisaatioittain")
    @PreAuthorize("hasAnyRole('APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @RequestMapping(value = "ryhmasByOrganisaatio/{oid}", method = RequestMethod.GET)
    public Map<String, List<Integer>> ryhmasByOrganisation(@PathVariable("oid") String henkiloOid) {
        return this.kayttoOikeusService.findKayttooikeusryhmatAndOrganisaatioByHenkiloOid(henkiloOid);
    }

    @RequestMapping(value = "/organisaatio/{organisaatioOid}", method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    @ApiOperation(value = "Listaa käyttöoikeusryhmät organisaation mukaan.",
            notes = "Listaa käyttöoikeusryhmät, jotka ovat mahdollisia pyynnössä annetulle organisaatiolle.")
    public List<KayttoOikeusRyhmaDto> listKayttoOikeusRyhmasByOrganisaatioOid(@PathVariable("organisaatioOid") String organisaatioOid) {
        return kayttoOikeusService.listPossibleRyhmasByOrganization(organisaatioOid);
    }

    @RequestMapping(value = "/{oid}/{organisaatioOid}", method = RequestMethod.GET)
    @PreAuthorize("@permissionCheckerServiceImpl.isAllowedToAccessPersonOrSelf(#oid, {'KAYTTOOIKEUS': {'READ', 'CRUD', 'PALVELUKAYTTAJA_CRUD'}}, null)")
    @ApiOperation(value = "Hakee henkilön voimassa olevat käyttöoikeusryhmät.",
            notes = "Listaa kaikki annetun henkilön ja tämän annettuun organisaatioon "
                    + "liittyvät voimassaolevat sekä mahdollisesti myönnettävissä olevat "
                    + "käyttöoikeusryhmät DTO:n avulla.")
    public List<MyonnettyKayttoOikeusDto> listKayttoOikeusRyhmasIncludingHenkilos(
            @PathVariable("oid") String oid, @PathVariable("organisaatioOid") String organisaatioOid) {
        return kayttoOikeusService.listMyonnettyKayttoOikeusRyhmasMergedWithHenkilos(oid, organisaatioOid, UserDetailsUtil.getCurrentUserOid());
    }

    @RequestMapping(value = "/henkilo/{oid}", method = RequestMethod.GET)
    @PreAuthorize("@permissionCheckerServiceImpl.isAllowedToAccessPersonOrSelf(#oid, {'KAYTTOOIKEUS': {'READ', 'CRUD', 'PALVELUKAYTTAJA_CRUD'}}, null)")
    @ApiOperation(value = "Hakee henkilön käyttöoikeusryhmät.",
            notes = "Listaa henkilön kaikki käyttöoikeusryhmät sekä rajaa ne "
                    + "tiettyyn organisaatioon, jos kutsussa on annettu organisaatiorajoite.")
    public List<MyonnettyKayttoOikeusDto> listKayttoOikeusRyhmaByHenkilo(
            @PathVariable("oid") String oid,
            @RequestParam(value = "ooid", required = false) String organisaatioOid) {
        return kayttoOikeusService.listMyonnettyKayttoOikeusRyhmasByHenkiloAndOrganisaatio(oid, organisaatioOid);
    }

    @RequestMapping(value = "/henkilo/current", method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    @ApiOperation(value = "Hakee kirjautuneen henkilön käyttöoikeusryhmät.",
            notes = "Listaa nykyisen kirjautuneen henkilön kaikki käyttöoikeusryhmät "
                    + "sekä rajaa ne tiettyyn organisaatioon, jos kutsussa on "
                    + "annettu organisaatiorajoite.")
    public List<MyonnettyKayttoOikeusDto> listKayttoOikeusRyhmaForCurrentUser(
            @RequestParam(value = "ooid", required = false) String organisaatioOid) {
        return kayttoOikeusService.listMyonnettyKayttoOikeusRyhmasByHenkiloAndOrganisaatio(UserDetailsUtil.getCurrentUserOid(), organisaatioOid);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_KAYTTOOIKEUSRYHMIEN_LUKU',"
            + "'ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation(value = "Hakee käyttöoikeusryhmän tiedot.",
            notes = "Hakee yhden käyttöoikeusryhmän kaikki tiedot "
                    + "annetun käyttöoikeusryhmän ID:n avulla.")
    public KayttoOikeusRyhmaDto getKayttoOikeusRyhma(@PathVariable("id") Long id,
                                                     @RequestParam(required = false) Boolean passiiviset) {
        return kayttoOikeusService.findKayttoOikeusRyhma(id, passiiviset);
    }


    @RequestMapping(value = "/{id}/sallitut", method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    @ApiOperation(value = "Hakee käyttöoikeusryhmän rajoiteryhmät.",
            notes = "Listaa kaikki käyttöoikeusryhmälle alistetut käyttöoikeusryhmät "
                    + "eli ne ryhmät jotka tämän ryhmän myöntäminen mahdollistaa.")
    public List<KayttoOikeusRyhmaDto> getSubRyhmasByKayttoOikeusRyhma(@PathVariable("id") Long id) {
        return kayttoOikeusService.findSubRyhmasByMasterRyhma(id);
    }

    @RequestMapping(value = "/{id}/kayttooikeus", method = RequestMethod.GET)
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_KAYTTOOIKEUSRYHMIEN_LUKU',"
            + "'ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation(value = "Hakee käyttöoikeusryhmään kuuluvat palvelut ja roolit.",
            notes = "Listaa kaikki annettuun käyttöoikeusryhmään kuuluvat "
                    + "palvelut ja roolit yhdistelmäpareina DTO:n avulla.")
    public List<PalveluRooliDto> getKayttoOikeusByKayttoOikeusRyhma(@PathVariable("id") Long id) {
        return kayttoOikeusService.findPalveluRoolisByKayttoOikeusRyhma(id);
    }


    @RequestMapping(value = "/{id}/henkilot", method = RequestMethod.GET)
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation(value = "Hakee käyttöoikeusryhmään kuuluvat henkilot",
            notes = "Listaa kaikki annettuun käyttöoikeusryhmään kuuluvat henkilöt joilla voimassaoleva lupa")
    public RyhmanHenkilotDto getHenkilosByKayttoOikeusRyhma(@PathVariable("id") Long id) {
        return kayttoOikeusService.findHenkilotByKayttoOikeusRyhma(id);
    }


    @PostMapping(consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation(value = "Luo uuden käyttöoikeusryhmän.",
            notes = "Tekee uuden käyttöoikeusryhmän annetun DTO:n pohjalta.")
    @ResponseBody
    public Long createKayttoOikeusRyhma(@RequestBody @Validated KayttoOikeusRyhmaModifyDto uusiRyhma) {
        return kayttoOikeusService.createKayttoOikeusRyhma(uusiRyhma);
    }


    @PostMapping(value = "/kayttooikeus", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation(value = "Luo uuden käyttöoikeuden.",
            notes = "Luo uuden käyttöoikeuden annetun käyttöoikeus modelin pohjalta.")
    public KayttoOikeusDto createNewKayttoOikeus(@RequestBody @Validated KayttoOikeusCreateDto kayttoOikeus) {
        long id = kayttoOikeusService.createKayttoOikeus(kayttoOikeus);
        return kayttoOikeusService.findKayttoOikeusById(id);
    }


    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation(value = "Päivittää käyttöoikeusryhmän.",
            notes = "Päivittää käyttöoikeusryhmän tiedot annetun DTO:n avulla.")
    public KayttoOikeusRyhmaDto updateKayttoOikeusRyhma(@PathVariable("id") Long id, @RequestBody @Validated KayttoOikeusRyhmaModifyDto ryhmaData) {
        kayttoOikeusService.updateKayttoOikeusForKayttoOikeusRyhma(id, ryhmaData);
        return kayttoOikeusService.findKayttoOikeusRyhma(id, true);
    }

    @PutMapping(value = "/{id}/passivoi", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation(value = "Passivoi käyttöoikeusryhmän.",
            notes = "Passivoi käyttöoikeusryhmän. ")
    public void passivoiKayttoOikeusRyhma(@PathVariable("id") Long id) {
        kayttoOikeusService.passivoiKayttooikeusryhma(id);
    }

    @PutMapping(value = "/{id}/aktivoi", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation(value = "Aktivoi käyttöoikeusryhmän.",
            notes = "Aktivoi passivoidun käyttöoikeusryhmän.")
    public void aktivoiKayttoOikeusRyhma(@PathVariable("id") Long id) {
        this.kayttoOikeusService.aktivoiKayttooikeusryhma(id);
    }

    @PostMapping(value = "/ryhmasByKayttooikeus", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_KAYTTOOIKEUSRYHMIEN_LUKU',"
            + "'ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation(value = "Listaa käyttöoikeusryhmät käyttooikeuksien mukaan.",
            notes = "Hakee käyttöoikeusryhmät joissa esiintyy jokin annetuista käyttöoikeuksista.")
    public List<KayttoOikeusRyhmaDto> getKayttoOikeusRyhmasByKayttoOikeus(
            @ApiParam("Format: {\"PALVELU\": \"ROOLI\", ...}") @RequestBody Map<String, String> kayttoOikeusList) {
        return kayttoOikeusService.findKayttoOikeusRyhmasByKayttoOikeusList(kayttoOikeusList);
    }

}
