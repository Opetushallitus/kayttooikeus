package fi.vm.sade.kayttooikeus.controller;

import fi.vm.sade.kayttooikeus.service.OrganisaatioService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Api(tags = "Kehityksen apuna toimivat ja käsin käynnistettävät toiminnot")
@RestController
@RequestMapping(value = "/dev", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@RequiredArgsConstructor
public class DevController {

    private final OrganisaatioService organisaatioService;

    @PostMapping(value = "/organisaatioCache", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation("Päivittää organisaatiovälimuistin. (db + memory)")
    public synchronized void updateCache() {
        organisaatioService.updateOrganisaatioCache();
    }

    @GetMapping("/organisaatioCache")
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation("Palauttaa organisaatioiden ja ryhmien lukumäärän välimuistisssa.")
    public synchronized void getCacheStatus() {
        organisaatioService.getClientCacheState();
    }

}
