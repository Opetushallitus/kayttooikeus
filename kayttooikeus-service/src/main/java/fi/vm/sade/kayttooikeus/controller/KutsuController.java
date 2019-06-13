package fi.vm.sade.kayttooikeus.controller;

import fi.vm.sade.kayttooikeus.dto.KutsuCreateDto;
import fi.vm.sade.kayttooikeus.dto.KutsuReadDto;
import fi.vm.sade.kayttooikeus.dto.KutsuUpdateDto;
import fi.vm.sade.kayttooikeus.enumeration.KutsuOrganisaatioOrder;
import fi.vm.sade.kayttooikeus.repositories.criteria.KutsuCriteria;
import fi.vm.sade.kayttooikeus.repositories.dto.HenkiloCreateByKutsuDto;
import fi.vm.sade.kayttooikeus.service.IdentificationService;
import fi.vm.sade.kayttooikeus.service.KutsuService;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloUpdateDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequestUri;

@RestController
@RequestMapping(value = "/kutsu", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@RequiredArgsConstructor
@Api(tags = "Virkailijan kutsumiseen liittyvät toiminnot")
public class KutsuController {
    private final KutsuService kutsuService;
    private final OppijanumerorekisteriClient oppijanumerorekisteriClient;
    private final IdentificationService identificationService;

    @RequestMapping(value = "", method = RequestMethod.GET)
    @ApiOperation(value = "Hakee kutsut annettujen hakuehtojen perusteella",
            notes = "Haun tulos riippuu käyttäjän oikeuksista (rekisterinpitäjä, Oph-virkailija, normaali käyttäjä)")
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_READ',"
            + "'ROLE_APP_KAYTTOOIKEUS_CRUD',"
            + "'ROLE_APP_KAYTTOOIKEUS_KUTSU_CRUD',"
            + "'ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    public List<KutsuReadDto> listKutsus(
            KutsuCriteria kutsuCriteria,
            @ApiParam("Järjestysperuste") @RequestParam(required = false, defaultValue = "AIKALEIMA") KutsuOrganisaatioOrder sortBy,
            @ApiParam("Järjestyksen suunta") @RequestParam(required = false, defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) Long offset,
            @RequestParam(required = false, defaultValue = "20") Long amount) {
        return this.kutsuService.listKutsus(sortBy, direction, kutsuCriteria, offset, amount);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation("Uuden kutsun luominen. Vaatii samat oikeudet kuin uuden käyttöoikeuden myöntäminen.")
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_CRUD',"
            + "'ROLE_APP_KAYTTOOIKEUS_KUTSU_CRUD',"
            + "'ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    public ResponseEntity<Long> create(@Validated @RequestBody KutsuCreateDto kutsu) {
        long id = kutsuService.createKutsu(kutsu);
        URI location = fromCurrentRequestUri().pathSegment(String.valueOf(id)).build().toUri();
        return ResponseEntity.created(location).body(id);
    }
    
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_CRUD',"
            + "'ROLE_APP_KAYTTOOIKEUS_KUTSU_CRUD',"
            + "'ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    public KutsuReadDto read(@PathVariable Long id) {
        return kutsuService.getKutsu(id);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_CRUD',"
            + "'ROLE_APP_KAYTTOOIKEUS_KUTSU_CRUD',"
            + "'ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    public void delete(@PathVariable Long id) {
        kutsuService.deleteKutsu(id);
    }

    @PutMapping(value = "/{id}/renew", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation("Kutsun uusiminen muuttamatta kutsun sisältöä eikä uusimisesta jää tietoa")
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_CRUD',"
            + "'ROLE_APP_KAYTTOOIKEUS_KUTSU_CRUD',"
            + "'ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    public void renew(@PathVariable Long id) {
        this.kutsuService.renewKutsu(id);
    }

    @PutMapping(value = "/{temporaryToken}/token/identifier", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation("Kutsun päivittäminen väliaikaisella tokenilla. Sallii osittaisen päivittämisen.")
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    public void updateIdentifierByToken(@PathVariable String temporaryToken,
                                        @RequestBody KutsuUpdateDto kutsuUpdateDto) {
        this.kutsuService.updateHakaIdentifierToKutsu(temporaryToken, kutsuUpdateDto);
    }


    /**
     *  /kutsu is open to non-authenticated use.
     */

    // Uses temporary tokens so not authenticated
    @ApiOperation("Get kutsu by temporary token")
    @RequestMapping(value = "/token/{temporaryToken}", method = RequestMethod.GET)
    public KutsuReadDto getByToken(@PathVariable String temporaryToken) {
        return this.kutsuService.getByTemporaryToken(temporaryToken);
    }

    // Consumes single use temporary tokens so not authenticated
    @ApiOperation("Luo henkilön väliaikaisella tokenilla. Palauttaa authTokenin kirjautumista varten.")
    @PostMapping(value = "/token/{temporaryToken}", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String createByToken(@PathVariable String temporaryToken,
                                @Validated @RequestBody HenkiloCreateByKutsuDto henkiloCreateByKutsuDto) {
        // This needs to be done like this since otherwice KO locks the table row for this henkilo and ONR can't update
        // it until the transaction finishes when ONR request timeouts.
        HenkiloUpdateDto henkiloUpdateDto = this.kutsuService.createHenkilo(temporaryToken, henkiloCreateByKutsuDto);
        this.oppijanumerorekisteriClient.updateHenkilo(henkiloUpdateDto);
        return this.identificationService.updateIdentificationAndGenerateTokenForHenkiloByOid(henkiloUpdateDto.getOidHenkilo());
    }

}
