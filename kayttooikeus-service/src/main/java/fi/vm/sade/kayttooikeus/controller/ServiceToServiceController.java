package fi.vm.sade.kayttooikeus.controller;

import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloCreateDto;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioHenkiloDto;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.PermissionCheckDto;
import fi.vm.sade.kayttooikeus.service.OrganisaatioHenkiloService;
import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/s2s", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@Api(tags = "Service to Service")
public class ServiceToServiceController {

    private PermissionCheckerService permissionCheckerService;
    private OrganisaatioHenkiloService organisaatioHenkiloService;

    @Autowired
    public ServiceToServiceController(PermissionCheckerService permissionCheckerService,
            OrganisaatioHenkiloService organisaatioHenkiloService) {
        this.permissionCheckerService = permissionCheckerService;
        this.organisaatioHenkiloService = organisaatioHenkiloService;
    }

    @ApiOperation("Palauttaa tiedon, onko käyttäjällä oikeus toiseen käyttäjään")
    @PreAuthorize("hasAnyRole('APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @PostMapping(value = "/canUserAccessUser", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public boolean checkUserPermissionToUser(@RequestBody PermissionCheckDto permissionCheckDto) {
        return permissionCheckerService.isAllowedToAccessPerson(permissionCheckDto);
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation(value = "Lisää henkilölle organisaatiot.",
            notes = "Lisää uudet organisaatiot henkilölle. Ei päivitä tai poista vanhoja organisaatiotietoja. Palauttaa henkilön kaikki nykyiset organisaatiot.")
    @PostMapping(value = "/henkilo/{oid}/organisaatio/findOrCreate", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<OrganisaatioHenkiloDto> addOrganisaatioHenkilot(@PathVariable("oid") String henkiloOid,
            @RequestBody @Validated List<OrganisaatioHenkiloCreateDto> organisaatioHenkilot) {
        return organisaatioHenkiloService.addOrganisaatioHenkilot(henkiloOid, organisaatioHenkilot);
    }
}
