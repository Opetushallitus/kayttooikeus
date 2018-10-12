package fi.vm.sade.kayttooikeus.controller;

import fi.vm.sade.kayttooikeus.dto.KayttajatiedotReadDto;
import fi.vm.sade.kayttooikeus.dto.LoginDto;
import fi.vm.sade.kayttooikeus.service.KayttajatiedotService;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/userDetails")
@RequiredArgsConstructor
public class UserDetailsController {

    private final UserDetailsService userDetailsService;
    private final KayttajatiedotService kayttajatiedotService;

    // Palomuurilla rajoitettu pääsy vain verkon sisältä
    @GetMapping("/{username}")
    public UserDetails getUserDetails(@PathVariable String username) {
        try {
            return userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    // Palomuurilla rajoitettu pääsy vain verkon sisältä
    @PostMapping
    public KayttajatiedotReadDto getByUsernameAndPassword(@Valid @RequestBody LoginDto dto) {
        return kayttajatiedotService.getByUsernameAndPassword(dto.getUsername(), dto.getPassword());
    }

}
