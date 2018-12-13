package fi.vm.sade.kayttooikeus.controller;

import fi.vm.sade.kayttooikeus.dto.PalvelukayttajaCreateDto;
import fi.vm.sade.kayttooikeus.dto.PalvelukayttajaCriteriaDto;
import fi.vm.sade.kayttooikeus.dto.PalvelukayttajaReadDto;
import fi.vm.sade.kayttooikeus.service.PalvelukayttajaService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/palvelukayttaja")
@RequiredArgsConstructor
public class PalvelukayttajaController {

    private final PalvelukayttajaService palvelukayttajaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_PALVELUKAYTTAJA_CRUD',"
            + "'ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    public Iterable<PalvelukayttajaReadDto> list(PalvelukayttajaCriteriaDto criteria) {
        return palvelukayttajaService.list(criteria);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_APP_KAYTTOOIKEUS_PALVELUKAYTTAJA_CRUD',"
            + "'ROLE_APP_KAYTTOOIKEUS_REKISTERINPITAJA')")
    public PalvelukayttajaReadDto create(@RequestBody @Valid PalvelukayttajaCreateDto dto) {
        return palvelukayttajaService.create(dto);
    }

}
