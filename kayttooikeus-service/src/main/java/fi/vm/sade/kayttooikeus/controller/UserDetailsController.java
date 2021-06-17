package fi.vm.sade.kayttooikeus.controller;

import fi.vm.sade.kayttooikeus.dto.KayttajatiedotReadDto;
import fi.vm.sade.kayttooikeus.dto.LoginDto;
import fi.vm.sade.kayttooikeus.service.KayttajatiedotService;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping(value = "/userDetails", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@RequiredArgsConstructor
public class UserDetailsController {

    private final UserDetailsService userDetailsService;
    private final KayttajatiedotService kayttajatiedotService;

    @GetMapping("/{username}")
    @PreAuthorize("hasAnyRole(" +
            "'ROLE_APP_KAYTTOOIKEUS_PALVELUKAYTTAJA_READ', " +
            "'ROLE_APP_KAYTTOOIKEUS_PALVELUKAYTTAJA_CRUD')")
    public UserDetails getUserDetails(@PathVariable String username) {
        try {
            return userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @PreAuthorize("hasAnyRole(" +
            "'ROLE_APP_KAYTTOOIKEUS_PALVELUKAYTTAJA_READ', " +
            "'ROLE_APP_KAYTTOOIKEUS_PALVELUKAYTTAJA_CRUD')")
    public KayttajatiedotReadDto getByUsernameAndPassword(@Valid @RequestBody LoginDto dto) {
        return kayttajatiedotService.getByUsernameAndPassword(dto.getUsername(), dto.getPassword());
    }

}
