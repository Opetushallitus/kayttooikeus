package fi.vm.sade.kayttooikeus.controller;

import fi.vm.sade.kayttooikeus.CasUserAttributes;
import fi.vm.sade.kayttooikeus.dto.LoginDto;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;
import fi.vm.sade.kayttooikeus.service.KayttajatiedotService;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
    @PostMapping(consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Rajapinta vain CAS:n käyttöön")
    public CasUserAttributes getByUsernameAndPassword(@Valid @RequestBody LoginDto dto) {
        Kayttajatiedot kayttajatiedot = kayttajatiedotService.getByUsernameAndPassword(dto.getUsername(), dto.getPassword());
        return CasUserAttributes.fromKayttajatiedot(kayttajatiedot);
    }
}