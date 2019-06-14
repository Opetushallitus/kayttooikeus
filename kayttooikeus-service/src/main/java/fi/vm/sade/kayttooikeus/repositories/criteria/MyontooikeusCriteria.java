package fi.vm.sade.kayttooikeus.repositories.criteria;

import fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Collection;
import java.util.Map;

import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.PALVELU_KAYTTOOIKEUS;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;

@Getter
@Setter
@Builder
@ToString
public class MyontooikeusCriteria {

    private Map<String, Collection<String>> kayttooikeudet;

    public static MyontooikeusCriteria oletus() {
        // käyttöoikeusanomuksien käsittely on sallittu vain KAYTTOOIKEUS_CRUD -käyttöoikeuksilla
        return MyontooikeusCriteria.builder()
                .kayttooikeudet(singletonMap(PALVELU_KAYTTOOIKEUS, singleton(PermissionCheckerServiceImpl.ROLE_CRUD)))
                .build();
    }

    public static MyontooikeusCriteria kutsu() {
        // kutsun kautta käyttöoikeuksia voidaan myöntää myös erillisellä roolilla
        return MyontooikeusCriteria.builder()
                .kayttooikeudet(singletonMap(PALVELU_KAYTTOOIKEUS,
                        asList(PermissionCheckerServiceImpl.ROLE_CRUD, PermissionCheckerServiceImpl.ROLE_KUTSU_CRUD)))
                .build();
    }

}
