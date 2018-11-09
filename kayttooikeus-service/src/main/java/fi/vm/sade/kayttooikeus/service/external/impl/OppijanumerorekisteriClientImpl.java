package fi.vm.sade.kayttooikeus.service.external.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.sade.generic.rest.CachingRestClient;
import fi.vm.sade.kayttooikeus.config.properties.ServiceUsersProperties;
import fi.vm.sade.kayttooikeus.service.dto.HenkiloVahvaTunnistusDto;
import fi.vm.sade.kayttooikeus.service.dto.HenkiloYhteystiedotDto;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.external.ExternalServiceException;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.kayttooikeus.util.FunctionalUtils;
import fi.vm.sade.oppijanumerorekisteri.dto.*;
import fi.vm.sade.properties.OphProperties;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static fi.vm.sade.kayttooikeus.service.external.ExternalServiceException.mapper;
import static fi.vm.sade.kayttooikeus.util.FunctionalUtils.retrying;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;

@Component
public class OppijanumerorekisteriClientImpl implements OppijanumerorekisteriClient {
    private static final String SERVICE_CODE = "kayttooikeus.kayttooikeuspalvelu-service";
    private final ObjectMapper objectMapper;
    private final OphProperties urlProperties;
    private final CachingRestClient proxyRestClient;
    private final CachingRestClient serviceAccountClient;

    @Autowired
    public OppijanumerorekisteriClientImpl(ObjectMapper objectMapper, OphProperties urlProperties,
                                           ServiceUsersProperties serviceUsersProperties) {
        this.objectMapper = objectMapper;
        this.urlProperties = urlProperties;
        this.proxyRestClient = new CachingRestClient().setClientSubSystemCode(SERVICE_CODE);
        this.proxyRestClient.setWebCasUrl(urlProperties.url("cas.url"));
        this.proxyRestClient.setCasService(urlProperties.url("oppijanumerorekisteri-service.security-check"));
        this.proxyRestClient.setUseProxyAuthentication(true);
        
        this.serviceAccountClient = new CachingRestClient().setClientSubSystemCode(SERVICE_CODE);
        this.serviceAccountClient.setWebCasUrl(urlProperties.url("cas.url"));
        this.serviceAccountClient.setCasService(urlProperties.url("oppijanumerorekisteri-service.security-check"));
        this.serviceAccountClient.setUsername(serviceUsersProperties.getOppijanumerorekisteri().getUsername());
        this.serviceAccountClient.setPassword(serviceUsersProperties.getOppijanumerorekisteri().getPassword());
    }

    @Override
    public List<HenkiloPerustietoDto> getHenkilonPerustiedot(Collection<String> henkiloOid) {
        if (henkiloOid.isEmpty()) {
            return new ArrayList<>();
        }
        String url = urlProperties.url("oppijanumerorekisteri-service.henkilo.henkiloPerustietosByHenkiloOidList");
        return retrying(FunctionalUtils.<List<HenkiloPerustietoDto>>io(
            () -> objectMapper.readerFor(new TypeReference<List<HenkiloPerustietoDto>>() {})
                    .readValue(IOUtils.toString(serviceAccountClient.post(url, MediaType.APPLICATION_JSON_VALUE,
                            objectMapper.writer().writeValueAsString(henkiloOid)).getEntity().getContent()))), 2).get()
                .orFail(mapper(url));
    }

    @Override
    public Set<String> getAllOidsForSamePerson(String personOid) {
        String url = urlProperties.url("oppijanumerorekisteri-service.s2s.duplicateHenkilos");
        Map<String,Object> criteria = new HashMap<>();
        criteria.put("henkiloOids", singletonList(personOid));
        return Stream.concat(Stream.of(personOid),
            retrying(FunctionalUtils.<List<HenkiloViiteDto>>io(
                () ->  objectMapper.readerFor(new TypeReference<List<HenkiloViiteDto>>() {})
                    .readValue(IOUtils.toString(this.serviceAccountClient.post(url, MediaType.APPLICATION_JSON_VALUE,
                        objectMapper.writeValueAsString(criteria)).getEntity().getContent()))), 2).get()
            .orFail(mapper(url)).stream().flatMap(viite -> Stream.of(viite.getHenkiloOid(), viite.getMasterOid()))
        ).collect(toSet());
    }

    @Override
    public String getOidByHetu(String hetu) {
        String url = urlProperties.url("oppijanumerorekisteri-service.s2s.oidByHetu", hetu);
        return retrying(FunctionalUtils.io(
                () -> IOUtils.toString(serviceAccountClient.get(url))), 2).get()
                .orFail((RuntimeException e) -> {
                    if (e.getCause() instanceof CachingRestClient.HttpException) {
                        if (((CachingRestClient.HttpException) e.getCause()).getStatusCode() == 404) {
                            throw new NotFoundException("could not find oid with hetu: " + hetu);
                        }
                    }
                    return new ExternalServiceException(url, e.getMessage(), e);
                });
    }

    @Override
    public List<HenkiloHakuPerustietoDto> getAllByOids(long page, long limit, List<String> oidHenkiloList) {
        Map<String, String> params = new HashMap<String, String>() {{
            put("offset", Long.toString(page));
            put("limit", Long.toString(limit));
        }};
        String data;
        try {
            data = oidHenkiloList == null || oidHenkiloList.isEmpty()
                    ? "{}"
                    : this.objectMapper.writeValueAsString(new HashMap<String, List<String>>() {{
                        put("henkiloOids", oidHenkiloList);
                    }});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unexpected error during json processing");
        }

        String url = this.urlProperties.url("oppijanumerorekisteri-service.s2s.henkilohaku-list-as-admin", params);
        return retrying(FunctionalUtils.<List<HenkiloHakuPerustietoDto>>io(
                () -> objectMapper.readerFor(new TypeReference<List<HenkiloHakuPerustietoDto>>() {})
                        .readValue(this.serviceAccountClient.post(url, MediaType.APPLICATION_JSON_VALUE,
                                data).getEntity().getContent())), 2).get()
                .orFail(mapper(url));
    }

    @Override
    public List<String> getModifiedSince(LocalDateTime dateTime, long offset, long amount) {
        Map<String, String> params = new HashMap<String, String>() {{
            put("offset", Long.toString(offset));
            put("amount", Long.toString(amount));
        }};
        String url = this.urlProperties.url("oppijanumerorekisteri-service.s2s.modified-since", dateTime, params);
        return retrying(FunctionalUtils.<List<String>>io(
                () -> this.objectMapper.readerFor(new TypeReference<List<String>>() {})
                        .readValue(this.serviceAccountClient.get(url))), 2).get()
                .orFail((RuntimeException e) -> new ExternalServiceException(url, e.getMessage(), e));
    }


    @Override
    public HenkiloPerustietoDto getPerustietoByOid(String oid) {
        String url = urlProperties.url("oppijanumerorekisteri-service.s2s.henkiloPerustieto");
        Map<String,Object> data = new HashMap<>();
        data.put("oidHenkilo", oid);

        return retrying(FunctionalUtils.<HenkiloPerustiedotDto>io(
                () -> objectMapper.readerFor(HenkiloPerustiedotDto.class)
                        .readValue(this.serviceAccountClient.post(url, MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsString(data)).getEntity().getContent())), 2).get()
                .orFail(mapper(url));
    }

    @Override
    public HenkiloDto getHenkiloByOid(String oid) {
        String url = this.urlProperties.url("oppijanumerorekisteri-service.henkilo.henkiloByOid", oid);

        return retrying(FunctionalUtils.<HenkiloDto>io(
                () -> this.objectMapper.readerFor(HenkiloDto.class)
                        .readValue(this.serviceAccountClient.get(url))), 2).get()
                .orFail((RuntimeException e) -> {
                    if (e.getCause() instanceof CachingRestClient.HttpException) {
                        if (((CachingRestClient.HttpException) e.getCause()).getStatusCode() == 404) {
                            throw new NotFoundException("Could not find henkilo by oid: " + oid);
                        }
                    }
                    return new ExternalServiceException(url, e.getMessage(), e);
                });
    }

    @Override
    public Optional<HenkiloDto> findHenkiloByOid(String oid) {
        try {
            return Optional.of(getHenkiloByOid(oid));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<HenkiloDto> getHenkiloByHetu(String hetu) {
        String url = this.urlProperties.url("oppijanumerorekisteri-service.henkilo.henkiloByHetu", hetu);

        return retrying(FunctionalUtils.<HenkiloDto>io(
                () -> this.objectMapper.readerFor(HenkiloDto.class)
                        .readValue(this.serviceAccountClient.get(url))), 2)
                .get().as(new ResponseToOptional<>(url));
    }

    @Override
    public Collection<HenkiloYhteystiedotDto> listYhteystiedot(HenkiloHakuCriteria criteria) {
        String url = urlProperties.url("oppijanumerorekisteri-service.henkilo.yhteystiedot");

        return retrying(FunctionalUtils.<Collection<HenkiloYhteystiedotDto>>io(
                () -> objectMapper.readerFor(new TypeReference<Collection<HenkiloYhteystiedotDto>>() {})
                        .readValue(IOUtils.toString(serviceAccountClient.post(url, MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsString(criteria)).getEntity().getContent()))), 2).get()
                .orFail(mapper(url));
    }

    @Override
    public Optional<String> createHenkiloForKutsu(HenkiloCreateDto henkiloCreateDto) {
        try {
            return Optional.ofNullable(this.createHenkilo(henkiloCreateDto));
        } catch (ExternalServiceException e) {
            if(e.getCause() instanceof CachingRestClient.HttpException
                    && ((CachingRestClient.HttpException)e.getCause()).getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
                return Optional.empty();
            }
            throw e;
        }

    }

    @Override
    public String createHenkilo(HenkiloCreateDto henkiloCreateDto) {
        String url = this.urlProperties.url("oppijanumerorekisteri-service.henkilo");
        return retrying(
                FunctionalUtils.<String>io(() -> {
                    try {
                        return IOUtils.toString(this.serviceAccountClient.post(url, MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsString(henkiloCreateDto)).getEntity().getContent());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Unexpected error during json processing");
                    }
                }),
                2).get().orFail(mapper(url));
    }

    @Override
    public void setStrongIdentifiedHetu(String oidHenkilo, HenkiloVahvaTunnistusDto henkiloVahvaTunnistusDto) {
        String url = this.urlProperties.url("oppijanumerorekisteri-service.cas.vahva-tunnistus", oidHenkilo);
        retrying(
                FunctionalUtils.io(() -> this.serviceAccountClient.put(url,
                        MediaType.APPLICATION_JSON_VALUE,
                        this.objectMapper.writeValueAsString(henkiloVahvaTunnistusDto))), 2)
                .get()
                .orFail(mapper(url));
    }

    @Override
    public void updateHenkilo(HenkiloUpdateDto henkiloUpdateDto) {
        String url = this.urlProperties.url("oppijanumerorekisteri-service.henkilo");
        retrying(
                FunctionalUtils.io(() -> this.serviceAccountClient.put(url,
                        MediaType.APPLICATION_JSON_VALUE,
                        this.objectMapper.writeValueAsString(henkiloUpdateDto))), 2)
                .get()
                .orFail(mapper(url));
    }

    @Override
    public void yhdistaHenkilot(String oid, Collection<String> duplicateOids) {
        String url = urlProperties.url("oppijanumerorekisteri-service.henkilo.byOid.yhdistaHenkilot", oid);
        retrying(FunctionalUtils.io(() -> serviceAccountClient.post(url, MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsString(duplicateOids))), 2).get().orFail(mapper(url));
    }

    @Override
    public HenkiloOmattiedotDto getOmatTiedot(String oidHenkilo) {
        String url = this.urlProperties.url("oppijanumerorekisteri.henkilo.omattiedot-by-oid", oidHenkilo);
        return retrying(FunctionalUtils.<HenkiloOmattiedotDto>io(
                () -> this.objectMapper.readerFor(HenkiloOmattiedotDto.class)
                        .readValue(this.serviceAccountClient.get(url))), 2)
                .get()
                .orFail(mapper(url));
    }

    //ONR uses java.time.LocalDate
    public static class HenkiloPerustiedotDto extends HenkiloPerustietoDto {
        public void setSyntymaaika(String localDate) {
            if (!StringUtils.isEmpty(localDate)) {
                this.setSyntymaaika(LocalDate.parse(localDate));
            }
        }
    }

    @Getter @Setter
    public static class HenkiloViiteDto {
        private String henkiloOid;
        private String masterOid;
    }

    private static class ResponseToOptional<T, E extends RuntimeException> implements BiFunction<T, E, Optional<T>> {

        private final String url;

        public ResponseToOptional(String url) {
            this.url = url;
        }

        @Override
        public Optional<T> apply(T result, E failure) {
            if (failure != null) {
                if (failure.getCause() instanceof CachingRestClient.HttpException) {
                    if (((CachingRestClient.HttpException) failure.getCause()).getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                        return Optional.empty();
                    }
                }
                throw mapper(url).apply(failure);
            }
            return Optional.of(result);
        }

    }
}
