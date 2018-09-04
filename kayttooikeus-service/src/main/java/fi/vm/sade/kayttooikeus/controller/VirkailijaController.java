package fi.vm.sade.kayttooikeus.controller;

import fi.vm.sade.kayttooikeus.dto.VirkailijaCreateDto;
import fi.vm.sade.kayttooikeus.dto.VirkailijaCriteriaDto;
import fi.vm.sade.kayttooikeus.dto.KayttajaReadDto;
import fi.vm.sade.kayttooikeus.service.VirkailijaService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/virkailija")
@RequiredArgsConstructor
public class VirkailijaController {

    private final VirkailijaService virkailijaService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_APP_KAYTTOOIKEUS_VIRKAILIJANLUONTI')")
    @ApiOperation(value = "Virkailijan luonti",
            notes = "Tarkoitettu vain testikäyttöön, tuotannossa virkailijat luodaan kutsun kautta.")
    public String create(@Valid @RequestBody VirkailijaCreateDto dto) {
        return virkailijaService.create(dto);
    }

    @PostMapping("/haku")
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_READ'," +
            "'ROLE_APP_KAYTTOOIKEUS_CRUD'," +
            "'ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    @ApiOperation("Virkailijoiden haku")
    public Iterable<KayttajaReadDto> list(@RequestBody VirkailijaCriteriaDto criteria) {
        return virkailijaService.list(criteria);
    }

}
