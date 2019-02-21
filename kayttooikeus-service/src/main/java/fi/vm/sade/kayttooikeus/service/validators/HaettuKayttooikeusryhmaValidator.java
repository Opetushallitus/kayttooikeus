package fi.vm.sade.kayttooikeus.service.validators;

import fi.vm.sade.kayttooikeus.model.AnomuksenTila;
import fi.vm.sade.kayttooikeus.model.HaettuKayttoOikeusRyhma;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class HaettuKayttooikeusryhmaValidator implements Validator {

    public boolean supports(@NotNull Class clazz) {
        return HaettuKayttoOikeusRyhma.class.equals(clazz);
    }

    public void validate(@NotNull Object object, @NotNull Errors errors) {
        HaettuKayttoOikeusRyhma haettuKayttooikeusryhma = (HaettuKayttoOikeusRyhma) object;

        if (haettuKayttooikeusryhma.getAnomus().getAnomuksenTila() != AnomuksenTila.ANOTTU) {
            errors.reject("Anomus already handled");
        }

    }

}
