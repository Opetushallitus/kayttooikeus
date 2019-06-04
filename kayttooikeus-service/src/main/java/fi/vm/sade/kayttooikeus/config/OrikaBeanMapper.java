package fi.vm.sade.kayttooikeus.config;

import fi.vm.sade.kayttooikeus.dto.HenkiloReadDto;
import fi.vm.sade.kayttooikeus.dto.KutsuCreateDto;
import fi.vm.sade.kayttooikeus.dto.KutsuReadDto;
import fi.vm.sade.kayttooikeus.dto.KutsuUpdateDto;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.Kutsu;
import fi.vm.sade.kayttooikeus.model.KutsuOrganisaatio;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;
import ma.glasnost.orika.Converter;
import ma.glasnost.orika.Mapper;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.converter.builtin.PassThroughConverter;
import ma.glasnost.orika.impl.ConfigurableMapper;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

// example from https://github.com/dlizarra/orika-spring-integration
@Component
public class OrikaBeanMapper extends ConfigurableMapper implements ApplicationContextAware {

    private MapperFactory factory;
    private ApplicationContext applicationContext;

    public OrikaBeanMapper() {
        super(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure(MapperFactory factory) {
        this.factory = factory;
        addAllSpringBeans(applicationContext);

        factory.classMap(KutsuCreateDto.class, Kutsu.class)
                .fieldAToB("asiointikieli", "kieliKoodi")
                .byDefault()
                .register();
        factory.classMap(KutsuCreateDto.KutsuOrganisaatioCreateDto.class, KutsuOrganisaatio.class)
                .fieldAToB("kayttoOikeusRyhmat", "ryhmat")
                .byDefault()
                .register();
        factory.classMap(KutsuOrganisaatio.class, KutsuReadDto.KutsuOrganisaatioReadDto.class)
                .fieldAToB("ryhmat", "kayttoOikeusRyhmat")
                .byDefault()
                .register();
        factory.classMap(OrganisaatioRDTO.class, OrganisaatioPerustieto.class)
                .fieldAToB("tyypit", "organisaatiotyypit")
                .fieldAToB("oppilaitosTyyppiUri", "oppilaitostyyppi")
                .byDefault()
                .register();
        factory.classMap(Henkilo.class, HenkiloReadDto.class)
                .fieldAToB("oidHenkilo", "oid")
                .byDefault()
                .register();
        factory.classMap(KutsuUpdateDto.class, Kutsu.class)
                .mapNulls(false)
                .byDefault()
                .register();
        // PassThroughConverter is fine since these are immutable
        factory.getConverterFactory().registerConverter(new PassThroughConverter(LocalDate.class));
        factory.getConverterFactory().registerConverter(new PassThroughConverter(LocalDateTime.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configureFactoryBuilder(final DefaultMapperFactory.Builder factoryBuilder) {
        // Nothing to configure for now
    }

    /**
     * Constructs and registers a {@link ClassMapBuilder} into the {@link MapperFactory} using a {@link Mapper}.
     *
     * @param mapper
     */
    @SuppressWarnings("rawtypes")
    public void addMapper(Mapper<?, ?> mapper) {
        factory.classMap(mapper.getAType(), mapper.getBType())
                .byDefault()
                .customize((Mapper) mapper)
                .register();
    }

    /**
     * Registers a {@link Converter} into the {@link ConverterFactory}.
     *
     * @param converter
     */
    public void addConverter(Converter<?, ?> converter) {
        factory.getConverterFactory().registerConverter(converter);
    }

    /**
     * Scans the appliaction context and registers all Mappers and Converters found in it.
     *
     * @param applicationContext
     */
    @SuppressWarnings("rawtypes")
    private void addAllSpringBeans(final ApplicationContext applicationContext) {
        Map<String, Mapper> mappers = applicationContext.getBeansOfType(Mapper.class);
        for (Mapper mapper : mappers.values()) {
            addMapper(mapper);
        }
        Map<String, Converter> converters = applicationContext.getBeansOfType(Converter.class);
        for (Converter converter : converters.values()) {
            addConverter(converter);
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        init();
    }

}