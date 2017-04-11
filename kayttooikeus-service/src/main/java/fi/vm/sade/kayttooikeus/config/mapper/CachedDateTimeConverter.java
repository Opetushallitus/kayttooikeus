package fi.vm.sade.kayttooikeus.config.mapper;

import ma.glasnost.orika.converter.builtin.PassThroughConverter;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

@Component
public class CachedDateTimeConverter extends PassThroughConverter {

    public CachedDateTimeConverter() {
        super(DateTime.class);
    }

}
