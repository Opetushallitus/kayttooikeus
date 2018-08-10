package fi.vm.sade.kayttooikeus.repositories.criteria;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import fi.vm.sade.kayttooikeus.dto.KayttajaTyyppi;
import fi.vm.sade.kayttooikeus.model.QHenkilo;
import fi.vm.sade.kayttooikeus.model.QMyonnettyKayttoOikeusRyhmaTapahtuma;
import fi.vm.sade.kayttooikeus.model.QOrganisaatioHenkilo;
import lombok.*;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Henkilöiden hakemiseen oppijanumerorekisteristä henkilön perustietojen
 * perusteella.
 */
@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class HenkiloCriteria {
    // Henkilo
    private String hetu;
    private Boolean passivoitu;
    private Boolean duplikaatti;
    private String nameQuery;
    private String sukunimi;
    private String kayttajatunnus;
    private KayttajaTyyppi kayttajaTyyppi;

    // Organisaatiohenkilo
    private Boolean noOrganisation;
    private Set<String> organisaatioOids;
    private Long kayttooikeusryhmaId;

    public Predicate condition(QHenkilo henkilo,
                                QOrganisaatioHenkilo organisaatioHenkilo,
                                QMyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma) {
        BooleanBuilder builder = new BooleanBuilder();
        // Henkilo
        if (this.passivoitu != null && !this.passivoitu) {
            builder.and(henkilo.passivoituCached.eq(false));
        }
        if (this.duplikaatti != null && !this.duplikaatti) {
            builder.and(henkilo.duplicateCached.eq(false));
        }
        if (StringUtils.hasLength(this.nameQuery)) {
            String trimmedQuery = this.nameQuery.trim();
            List<String> queryParts = Arrays.asList(trimmedQuery.split(" "));

            if (queryParts.size() > 1) {
                // expect sukunimi to be first or last of queryParts
                // use startsWithIgnoreCase to get use of index

                BooleanBuilder SukunimiEtunimiPredicate = new BooleanBuilder();
                SukunimiEtunimiPredicate.and(henkilo.sukunimiCached.startsWithIgnoreCase(queryParts.get(0)));
                String etunimiLast = String.join(" ", queryParts.subList(1, queryParts.size()));
                SukunimiEtunimiPredicate.and(henkilo.etunimetCached.startsWithIgnoreCase(etunimiLast)
                        .or(henkilo.kutsumanimiCached.startsWithIgnoreCase(etunimiLast)));

                BooleanBuilder etunimiSukunimiPredicate = new BooleanBuilder();
                etunimiSukunimiPredicate.and(henkilo.sukunimiCached.startsWithIgnoreCase(queryParts.get(queryParts.size() - 1)));
                String etunimiFirst = String.join(" ", queryParts.subList(0, queryParts.size() - 1));
                etunimiSukunimiPredicate.and(henkilo.etunimetCached.startsWithIgnoreCase(etunimiFirst)
                        .or(henkilo.kutsumanimiCached.startsWithIgnoreCase(etunimiFirst)));

                builder.and(SukunimiEtunimiPredicate
                        .or(etunimiSukunimiPredicate)
                        .or(henkilo.etunimetCached.startsWithIgnoreCase(trimmedQuery)));
            }
            else {
                builder.and(
                        Expressions.anyOf(
                                henkilo.oidHenkilo.eq(trimmedQuery),
                                henkilo.etunimetCached.startsWithIgnoreCase(trimmedQuery),
                                henkilo.sukunimiCached.startsWithIgnoreCase(trimmedQuery),
                                henkilo.kutsumanimiCached.startsWithIgnoreCase(trimmedQuery),
                                henkilo.hetuCached.eq(trimmedQuery)
                        )
                );
            }
        }
        if (StringUtils.hasLength(kayttajatunnus)) {
            builder.and(henkilo.kayttajatiedot.username.eq(kayttajatunnus));
        }
        if (sukunimi != null) {
            builder.and(henkilo.sukunimiCached.startsWithIgnoreCase(sukunimi));
        }
        if (kayttajaTyyppi != null) {
            builder.and(henkilo.kayttajaTyyppi.eq(kayttajaTyyppi));
        }
        // Kayttooikeus
        if (this.kayttooikeusryhmaId != null) {
            builder.and(myonnettyKayttoOikeusRyhmaTapahtuma.kayttoOikeusRyhma.id.eq(this.kayttooikeusryhmaId));
        }

        return builder;
    }
}
