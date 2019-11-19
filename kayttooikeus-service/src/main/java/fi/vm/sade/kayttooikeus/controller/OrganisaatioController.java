package fi.vm.sade.kayttooikeus.controller;

import fi.vm.sade.kayttooikeus.dto.OrganisaatioCriteriaDto;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioDto;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioWithChildrenDto;
import fi.vm.sade.kayttooikeus.service.OrganisaatioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping(value = "/organisaatio", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@RequiredArgsConstructor
public class OrganisaatioController {

    private final OrganisaatioService organisaatioService;

    @GetMapping
    public Collection<OrganisaatioDto> listBy(OrganisaatioCriteriaDto criteria) {
        return organisaatioService.listBy(criteria);
    }

    @GetMapping("/root")
    public OrganisaatioWithChildrenDto getRootWithChildrenBy(OrganisaatioCriteriaDto criteria) {
        return organisaatioService.getRootWithChildrenBy(criteria);
    }

    @GetMapping("/{oid}")
    public OrganisaatioWithChildrenDto getByOid(@PathVariable String oid) {
        return organisaatioService.getByOid(oid);
    }

}
