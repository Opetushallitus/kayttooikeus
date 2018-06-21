package fi.vm.sade.kayttooikeus.repositories.criteria;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import fi.vm.sade.kayttooikeus.model.QHenkilo;
import fi.vm.sade.kayttooikeus.model.QKayttajatiedot;
import fi.vm.sade.kayttooikeus.model.QKayttoOikeusRyhma;
import lombok.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KayttooikeusCriteria {
    String username;
    String oidHenkilo;
    Set<String> palvelu = new HashSet<>();
    String hetu;

    public Predicate condition(QKayttajatiedot kayttajatiedot, QHenkilo henkilo, QKayttoOikeusRyhma kayttoOikeusRyhma) {
        BooleanBuilder builder = new BooleanBuilder();
        if(StringUtils.hasLength(this.username)) {
            builder.and(kayttajatiedot.username.equalsIgnoreCase(this.username));
        }
        if(StringUtils.hasLength(this.oidHenkilo)) {
            builder.and(henkilo.oidHenkilo.eq(this.oidHenkilo));
        }
        if(!CollectionUtils.isEmpty(this.palvelu)) {
            builder.and(kayttoOikeusRyhma.kayttoOikeus.any().palvelu.name.in(this.palvelu));
        }
        if(StringUtils.hasLength(this.hetu)) {
            builder.and(henkilo.hetuCached.eq(this.hetu));
        }
        return builder;
    }
}
