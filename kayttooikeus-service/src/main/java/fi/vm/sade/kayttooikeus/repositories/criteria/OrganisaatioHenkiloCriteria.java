package fi.vm.sade.kayttooikeus.repositories.criteria;

import fi.vm.sade.kayttooikeus.dto.KayttajaTyyppi;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Collection;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Getter
@Setter
@ToString
public class OrganisaatioHenkiloCriteria {

    private KayttajaTyyppi kayttajaTyyppi;
    private Boolean passivoitu;
    private Set<String> organisaatioOids;
    private Set<String> kayttoOikeusRyhmaNimet;
    private Collection<String> kayttooikeudet;

    public boolean setOrRetainOrganisaatioOids(Collection<String> oids) {
        if (organisaatioOids == null) {
            organisaatioOids = oids.stream().collect(toSet());
            return true;
        }
        return organisaatioOids.retainAll(oids);
    }

}
