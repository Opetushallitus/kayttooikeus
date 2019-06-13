package fi.vm.sade.kayttooikeus.controller;

import fi.vm.sade.kayttooikeus.service.PalveluService;
import fi.vm.sade.kayttooikeus.dto.PalveluDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/palvelu", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@Api(value = "/palvelu")
public class PalveluController {
    private PalveluService palveluService;

    @Autowired
    public PalveluController(PalveluService palveluService) {
        this.palveluService = palveluService;
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_READ',"
            + "'ROLE_APP_KAYTTOOIKEUS_CRUD',"
            + "'ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation(value = "Hakee kaikki palvelut.",
            notes = "Listaa kaikki palvelut, jotka löytyvät henkilöhallinnan kannasta.")
    @RequestMapping(method = RequestMethod.GET)
    public List<PalveluDto> listPalvelus() {
        return palveluService.listPalvelus();
    }
}