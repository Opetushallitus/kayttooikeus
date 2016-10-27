package fi.vm.sade.kayttooikeus.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import fi.vm.sade.kayttooikeus.dto.KayttoOikeusHistoriaDto;
import fi.vm.sade.kayttooikeus.dto.PalveluKayttoOikeusDto;
import fi.vm.sade.kayttooikeus.service.KayttoOikeusService;
import fi.vm.sade.kayttooikeus.service.TaskExecutorService;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

@RestController
@RequestMapping("/kayttooikeus")
@Api(value = "/kayttooikeus", description = "Käyttöoikeuksien käsittelyyn liittyvät operaatiot.")
public class KayttoOikeusController {
    private KayttoOikeusService kayttoOikeusService;
    private TaskExecutorService taskExecutorService;

    @Autowired
    public KayttoOikeusController(KayttoOikeusService kayttoOikeusService, TaskExecutorService taskExecutorService) {
        this.kayttoOikeusService = kayttoOikeusService;
        this.taskExecutorService = taskExecutorService;
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_HENKILONHALLINTA_READ',"
            + "'ROLE_APP_HENKILONHALLINTA_READ_UPDATE',"
            + "'ROLE_APP_HENKILONHALLINTA_CRUD',"
            + "'ROLE_APP_HENKILONHALLINTA_OPHREKISTERI')")
    @ApiOperation(value = "Hakee palveluun liittyvät käyttöoikeudet.",
            notes = "Listaa kaikki palveluun liitetyt käyttöoikeudet "
                    + "palvelu-käyttöoikeus DTO:n avulla, johon on asetettu "
                    + "roolinimi ja sen lokalisoidut tekstit.")
    @RequestMapping(value = "/{name}", method = RequestMethod.GET)
    public List<PalveluKayttoOikeusDto> listKayttoOikeusByPalvelu(
            @ApiParam("Palvelun tunnistenimi") @PathVariable("name") String name) {
        return kayttoOikeusService.listKayttoOikeusByPalvelu(name);
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_HENKILONHALLINTA_READ',"
            + "'ROLE_APP_HENKILONHALLINTA_READ_UPDATE',"
            + "'ROLE_APP_HENKILONHALLINTA_CRUD',"
            + "'ROLE_APP_HENKILONHALLINTA_OPHREKISTERI')")
    @ApiOperation(value = "Hakee kirjautuneen käyttäjän käyttöoikeudet.",
            notes = "Listaa kaikki nykyisen sisäänkirjautuneen käyttäjän käyttöoikeudet, "
                    + "jossa on mukana myös vanhentuneet käyttöoikeudet.")
    @RequestMapping(value = "/kayttaja/current", method = RequestMethod.GET)
    public List<KayttoOikeusHistoriaDto> listKayttoOikeusCurrentUser() {
        return kayttoOikeusService.listMyonnettyKayttoOikeusHistoriaForCurrentUser();
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_HENKILONHALLINTA_OPHREKISTERI')")
    @ApiOperation(value = "Lähettää muistutusviestit henkilöille joilla on käyttöoikeus päättymässä.",
            notes = "Tämä on alustavasti vain automaattisen sähköpostimuistutuksen testausta varten.",
            authorizations = @Authorization("ROLE_APP_HENKILONHALLINTA_OPHREKISTERI"),
            produces = TEXT_PLAIN, response = Integer.class)
    @RequestMapping(value = "/expirationReminders", method = RequestMethod.POST, produces = TEXT_PLAIN)
    public String sendExpirationReminders(@ApiParam("Vuosi") @RequestParam("year") int year,
                                       @ApiParam("Kuukausi") @RequestParam("month") int month,
                                       @ApiParam("Päivä") @RequestParam("day") int day) {
        Period expireThreshold = new Period(LocalDate.now(), new LocalDate(year, month, day));
        return String.format("%d", taskExecutorService.sendExpirationReminders(expireThreshold));
    }
}
