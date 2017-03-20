package fi.vm.sade.kayttooikeus.controller;

import com.fasterxml.jackson.annotation.JsonView;
import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.ExternalPermissionService;
import fi.vm.sade.kayttooikeus.model.Identification;
import fi.vm.sade.kayttooikeus.service.*;
import fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/henkilo")
@Api(value = "/henkilo", description = "Henkilön organisaatiohenkilöihin liittyvät operaatiot.")
public class HenkiloController {
    private final OrganisaatioHenkiloService organisaatioHenkiloService;
    private final HenkiloService henkiloService;
    private final KayttajatiedotService kayttajatiedotService;
    private final IdentificationService identificationService;

    @Autowired
    public HenkiloController(OrganisaatioHenkiloService organisaatioHenkiloService,
                             HenkiloService henkiloService,
                             KayttajatiedotService kayttajatiedotService,
                             IdentificationService identificationService) {
        this.organisaatioHenkiloService = organisaatioHenkiloService;
        this.henkiloService = henkiloService;
        this.kayttajatiedotService = kayttajatiedotService;
        this.identificationService = identificationService;
    }

    @PreAuthorize("@permissionCheckerServiceImpl.isAllowedToAccessPersonOrSelf(#oid, {'READ', 'READ_UPDATE', 'CRUD'}, #permissionService)")
    @ApiOperation(value = "Listaa henkilön aktiiviset organisaatiot (organisaatiohenkilöt) organisaatioiden tiedoilla rekursiiisesti.",
            notes = "Hakee annetun henkilön aktiiviset organisaatiohenkilöt organisaation tiedoilla siten, että roganisaatio sisältää myös lapsiroganisaationsa rekursiivisesti.")
    @RequestMapping(value = "/{oid}/organisaatio", method = RequestMethod.GET)
    public List<OrganisaatioHenkiloWithOrganisaatioDto> listOrganisatioHenkilos(
            @PathVariable @ApiParam("Henkilö-OID") String oid,
            @RequestParam(required = false, defaultValue = "fi") @ApiParam("Organisaatioiden järjestyksen kielikoodi (oletus fi)")
                    String comparisonLangCode,
            @RequestHeader(value = "External-Permission-Service", required = false)
                    ExternalPermissionService permissionService) {
        return organisaatioHenkiloService.listOrganisaatioHenkilos(oid, comparisonLangCode);
    }
    
    @PreAuthorize("@permissionCheckerServiceImpl.isAllowedToAccessPerson(#henkiloOid, {'READ', 'READ_UPDATE', 'CRUD'}, #permissionService)")
    @ApiOperation(value = "Listaa henkilön organisaatiot.",
            notes = "Hakee annetun henkilön kaikki organisaatiohenkilöt.")
    @RequestMapping(value = "/{oid}/organisaatiohenkilo", method = RequestMethod.GET)
    public List<OrganisaatioHenkiloDto> listOrganisaatioHenkilos(@PathVariable("oid") String henkiloOid,
                                                                 @RequestHeader(value = "External-Permission-Service", required = false)
                                                                         ExternalPermissionService permissionService) {
        return organisaatioHenkiloService.findOrganisaatioByHenkilo(henkiloOid);
    }

    @PreAuthorize("@permissionCheckerServiceImpl.isAllowedToAccessPerson(#henkiloOid, {'READ', 'READ_UPDATE', 'CRUD'}, null)")
    @ApiOperation(value = "Hakee henkilön yhden organisaation tiedot.",
            notes = "Hakee henkilön yhden organisaatiohenkilön tiedot.")
    @RequestMapping(value = "/{oid}/organisaatiohenkilo/{organisaatioOid}", method = RequestMethod.GET)
    public OrganisaatioHenkiloDto findByOrganisaatioOid(@PathVariable("oid") String henkiloOid,
                                                        @PathVariable("organisaatioOid") String organisaatioOid) {
        return organisaatioHenkiloService.findOrganisaatioHenkiloByHenkiloAndOrganisaatio(henkiloOid, organisaatioOid);
    }

    @PreAuthorize("@permissionCheckerServiceImpl.isAllowedToAccessPerson(#henkiloOid, {'CRUD'}, null)")
    @ApiOperation(value = "Luo henkilön käyttäjätiedot.",
            notes = "Luo henkilön käyttäjätiedot.")
    @RequestMapping(value = "/{oid}/kayttajatiedot", method = RequestMethod.POST)
    public KayttajatiedotReadDto createKayttajatiedot(@PathVariable("oid") String henkiloOid,
            @RequestBody @Validated KayttajatiedotCreateDto kayttajatiedot) {
        return kayttajatiedotService.create(henkiloOid, kayttajatiedot);
    }

    @PreAuthorize("@permissionCheckerServiceImpl.isAllowedToAccessPerson(#henkiloOid, {'READ', 'READ_UPDATE', 'CRUD'}, null)")
    @ApiOperation(value = "Hakee henkilön käyttäjätiedot.",
            notes = "Hakee henkilön käyttäjätiedot.")
    @RequestMapping(value = "/{oid}/kayttajatiedot", method = RequestMethod.GET)
    public KayttajatiedotReadDto getKayttajatiedot(@PathVariable("oid") String henkiloOid) {
        return kayttajatiedotService.getByHenkiloOid(henkiloOid);
    }

    @ApiOperation(value = "Hakee henkilöitä organisaatioiden ja käyttöoikeuksien mukaan.",
            notes = "Tämä toteutus on tehty Osoitepalvelua varten, jonka pitää pystyä "
                    + "hakemaan henkilöitä henkilötyyppien, organisaatioiden sekä"
                    + "käyttöoikeusryhmien mukaan, tämä toteutus ei ole UI:n käytössä.")
    @PreAuthorize("hasAnyRole('ROLE_APP_HENKILONHALLINTA_READ',"
            + "'ROLE_APP_HENKILONHALLINTA_READ_UPDATE',"
            + "'ROLE_APP_HENKILONHALLINTA_CRUD',"
            + "'ROLE_APP_HENKILONHALLINTA_OPHREKISTERI')")
    @RequestMapping(value = "/byOoids", method = RequestMethod.POST)
    public List<String> findHenkilosByOrganisaatioOids(@RequestBody @Valid OrganisaatioOidsSearchDto organisaatioOidsSearchDto) {
        return henkiloService.findHenkilos(organisaatioOidsSearchDto);
    }

    @PreAuthorize("@permissionChecker.isAllowedToAccessPerson(#oid, {'CRUD', 'KKVASTUU'}, #permissionService)")
    @RequestMapping(value = "/{oid}/hakatunnus", method = RequestMethod.GET)
    @ApiOperation(value = "Hakee henkilön Haka-tunnisteet. DEPRECATE.",
            notes = "Hakee annetun henkilön Haka-tunnisteet.",
            authorizations = @Authorization("ROLE_APP_HENKILONHALLINTA_CRUD, " +
                    "ROLE_APP_HENKILONHALLINTA_KKVASTUU, " +
                    "ROLE_APP_HENKILONHALLINTA_OPHREKISTERI"),
            response = List.class)
    public List<HenkiloHakaDto> getHenkilosHakaTunnisteet(@P("oid") @ApiParam("Henkilön OID") String oid,
                                                          @P("permissionService") @HeaderParam("External-Permission-Service")
                                                                  ExternalPermissionService permissionService) {
       return identificationService.getHenkiloHakaDTOsByHenkiloAndIdp(oid, "haka");
    }

}
