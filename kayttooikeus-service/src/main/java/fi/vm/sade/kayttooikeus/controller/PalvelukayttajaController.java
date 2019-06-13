package fi.vm.sade.kayttooikeus.controller;

import fi.vm.sade.kayttooikeus.dto.PalvelukayttajaCreateDto;
import fi.vm.sade.kayttooikeus.dto.PalvelukayttajaCriteriaDto;
import fi.vm.sade.kayttooikeus.dto.PalvelukayttajaReadDto;
import fi.vm.sade.kayttooikeus.service.PalvelukayttajaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping(value = "/palvelukayttaja", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@RequiredArgsConstructor
public class PalvelukayttajaController {

    private final PalvelukayttajaService palvelukayttajaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_PALVELUKAYTTAJA_CRUD',"
            + "'ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    public Iterable<PalvelukayttajaReadDto> list(PalvelukayttajaCriteriaDto criteria) {
        return palvelukayttajaService.list(criteria);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_PALVELUKAYTTAJA_CRUD',"
            + "'ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    public PalvelukayttajaReadDto create(@RequestBody @Valid PalvelukayttajaCreateDto dto) {
        return palvelukayttajaService.create(dto);
    }

}
