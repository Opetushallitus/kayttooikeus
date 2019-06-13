package fi.vm.sade.kayttooikeus.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import fi.vm.sade.kayttooikeus.service.MyonnettyKayttoOikeusService;

@RestController
@RequestMapping(value = "/myonnettyKayttoOikeus", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@RequiredArgsConstructor
public class MyonnettyKayttoOikeusController {

    private final MyonnettyKayttoOikeusService myonnettyKayttooikeusService;

    @DeleteMapping("/vanhentuneet")
    @PreAuthorize("hasRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation("Poistaa vanhentuneet käyttöoikeudet.")
    public synchronized void poistaVanhentuneet() {
        myonnettyKayttooikeusService.poistaVanhentuneet();
    }

}
