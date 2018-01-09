package fi.vm.sade.kayttooikeus.repositories.criteria;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.sun.org.apache.xpath.internal.operations.Bool;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.QHenkilo;
import fi.vm.sade.kayttooikeus.model.QMyonnettyKayttoOikeusRyhmaTapahtuma;
import fi.vm.sade.kayttooikeus.model.QOrganisaatioHenkilo;
import lombok.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
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
    private Boolean passivoitu;
    private Boolean duplikaatti;
    private String nameQuery;

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
            BooleanBuilder predicate = new BooleanBuilder();
            String trimmedQuery = this.nameQuery.trim();
            List<String> queryParts = Arrays.asList(trimmedQuery.split(" "));

            if (queryParts.size() > 1) {
                // expect sukunimi to be first or last of queryParts
                // use startsWithIgnoreCase to get use of index

                BooleanBuilder SukunimiEtunimiPredicate = new BooleanBuilder();
                SukunimiEtunimiPredicate.and(henkilo.sukunimiCached.startsWithIgnoreCase(queryParts.get(0)));
                String etunimiLast = String.join(" ", queryParts.subList(1, queryParts.size()));
                SukunimiEtunimiPredicate.and(henkilo.etunimetCached.startsWithIgnoreCase(etunimiLast));

                BooleanBuilder etunimiSukunimiPredicate = new BooleanBuilder();
                etunimiSukunimiPredicate.and(henkilo.sukunimiCached.startsWithIgnoreCase(queryParts.get(queryParts.size() - 1)));
                String etunimiFirst = String.join(" ", queryParts.subList(0, queryParts.size() - 1));
                etunimiSukunimiPredicate.and(henkilo.etunimetCached.startsWithIgnoreCase(etunimiFirst));

                predicate.or(SukunimiEtunimiPredicate).or(etunimiSukunimiPredicate);

            }
            else {
                predicate.or(
                        Expressions.anyOf(
                                henkilo.oidHenkilo.eq(trimmedQuery),
                                henkilo.etunimetCached.startsWithIgnoreCase(trimmedQuery),
                                henkilo.sukunimiCached.startsWithIgnoreCase(trimmedQuery),
                                henkilo.hetuCached.eq(trimmedQuery)
                        )
                );
            }
            builder.and(predicate);
        }
        // Organisaatiohenkilo
        if (this.noOrganisation != null && !this.noOrganisation) {
            QOrganisaatioHenkilo subOrganisaatioHenkilo = new QOrganisaatioHenkilo("subOrganisaatioHenkilo");
            JPQLQuery<Henkilo> subquery = JPAExpressions.select(subOrganisaatioHenkilo.henkilo)
                    .from(subOrganisaatioHenkilo)
                    .where(subOrganisaatioHenkilo.passivoitu.isFalse()
                            .and(subOrganisaatioHenkilo.henkilo.eq(henkilo)));
            builder.and(subquery.exists());
        }
        if (!CollectionUtils.isEmpty(this.organisaatioOids)) {
            builder.and(organisaatioHenkilo.organisaatioOid.in(this.organisaatioOids));
            builder.and(organisaatioHenkilo.passivoitu.isFalse());
        }
        // Kayttooikeus
        if (this.kayttooikeusryhmaId != null) {
            builder.and(myonnettyKayttoOikeusRyhmaTapahtuma.kayttoOikeusRyhma.id.eq(this.kayttooikeusryhmaId));
        }

        return builder;
    }
}
