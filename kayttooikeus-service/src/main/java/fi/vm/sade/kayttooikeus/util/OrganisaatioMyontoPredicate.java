package fi.vm.sade.kayttooikeus.util;

import fi.vm.sade.kayttooikeus.dto.enumeration.OrganisaatioStatus;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Voiko organisaatioon myöntää käyttöoikeuksia.
 */
public class OrganisaatioMyontoPredicate implements Predicate<OrganisaatioPerustieto> {

    private final static Set<OrganisaatioStatus> SALLITUT_TILAT = EnumSet.of(
            OrganisaatioStatus.AKTIIVINEN,
            OrganisaatioStatus.SUUNNITELTU);

    @Override
    public boolean test(OrganisaatioPerustieto organisaatio) {
        return SALLITUT_TILAT.contains(organisaatio.getStatus());
    }

}
