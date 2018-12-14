package fi.vm.sade.kayttooikeus.repositories.criteria;

import fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.PALVELU_KAYTTOOIKEUS;
import java.util.Set;
import static java.util.stream.Collectors.toSet;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
public class MyontooikeusCriteria {

    private Set<String> palvelut;
    private String rooli;

    public static MyontooikeusCriteria oletus() {
        // käyttöoikeusanomuksien käsittely on sallittu vain KAYTTOOIKEUS_CRUD -käyttöoikeuksilla
        return MyontooikeusCriteria.builder()
                .palvelut(Stream.of(PALVELU_KAYTTOOIKEUS).collect(toSet()))
                .rooli(PermissionCheckerServiceImpl.ROLE_CRUD)
                .build();
    }

}
