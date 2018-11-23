package fi.vm.sade.kayttooikeus.service.impl.anomus;

import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.repositories.criteria.AnomusCriteria;
import fi.vm.sade.kayttooikeus.repositories.criteria.AnomusCriteria.Myontooikeus;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.util.OrganisaatioMyontoPredicate;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@RequiredArgsConstructor
public class MyontooikeusMapper implements Function<Map.Entry<String, Set<Long>>, Myontooikeus> {

    private final CommonProperties commonProperties;
    private final OrganisaatioClient organisaatioClient;
    private final AnomusCriteria criteria;

    @Override
    public Myontooikeus apply(Map.Entry<String, Set<Long>> entry) {
        String organisaatioOid = entry.getKey();
        Set<Long> kayttooikeusryhmaIds = entry.getValue();
        boolean rootOrganisaatio = commonProperties.getRootOrganizationOid().equals(organisaatioOid);

        Set<String> organisaatioOids = new HashSet<>();
        organisaatioOids.add(organisaatioOid);
        if (!rootOrganisaatio) {
            organisaatioOids.addAll(organisaatioClient.listWithChildOids(organisaatioOid,
                    new OrganisaatioMyontoPredicate()));
        }

        if (criteria.getOrganisaatioOids() != null) {
            // suodatetaan käyttäjän valitsemat organisaatiot
            if (rootOrganisaatio) {
                organisaatioOids.clear();
                organisaatioOids.addAll(criteria.getOrganisaatioOids());
                rootOrganisaatio = false;
            } else {
                organisaatioOids.retainAll(criteria.getOrganisaatioOids());
            }
        }
        if (criteria.getKayttooikeusRyhmaIds() != null) {
            // suodatetaan käyttäjän valitsemat käyttöoikeusryhmät
            kayttooikeusryhmaIds.retainAll(criteria.getKayttooikeusRyhmaIds());
        }
        return new Myontooikeus(rootOrganisaatio, organisaatioOids, kayttooikeusryhmaIds);
    }

}
