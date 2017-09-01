package fi.vm.sade.kayttooikeus.service.external.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.sade.generic.rest.CachingRestClient;
import fi.vm.sade.kayttooikeus.config.properties.ServiceUsersProperties;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
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
                    .readValue(IOUtils.toString(serviceAccountClient.post(url, MediaType.APPLICATION_JSON,
                            objectMapper.writer().writeValueAsString(henkiloOid)).getEntity().getContent()))), 2).get()
                .orFail(mapper(url));
    }

    @Override
    public HenkilonYhteystiedotViewDto getHenkilonYhteystiedot(String henkiloOid) {
        String url = urlProperties.url("oppijanumerorekisteri-service.henkilo.yhteystiedot", henkiloOid);
        return retrying(FunctionalUtils.<HenkilonYhteystiedotViewDto>io(
                    () -> objectMapper.readerFor(HenkilonYhteystiedotViewDto.class)
                .readValue(serviceAccountClient.getAsString(url))), 2).get()
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
                    .readValue(IOUtils.toString(this.serviceAccountClient.post(url, MediaType.APPLICATION_JSON,
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
                        .readValue(this.serviceAccountClient.post(url, MediaType.APPLICATION_JSON,
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
                        .readValue(this.serviceAccountClient.post(url, MediaType.APPLICATION_JSON,
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
    public Set<String> listOidByYhteystieto(String arvo) {
        String url = urlProperties.url("oppijanumerorekisteri-service.henkilo.oidByYhteystieto", arvo);
        return retrying(FunctionalUtils.<Set<String>>io(
                () -> this.objectMapper.readerFor(new TypeReference<Set<String>>() {
                }).readValue(this.serviceAccountClient.get(url))), 2).get()
                .orFail(mapper(url));
    }

    @Override
    public String createHenkilo(HenkiloCreateDto henkiloCreateDto) {
        String url = this.urlProperties.url("oppijanumerorekisteri-service.henkilo");

        return retrying(FunctionalUtils.<String>io(
                () -> objectMapper.readerFor(String.class)
                        .readValue(this.serviceAccountClient.post(url, MediaType.APPLICATION_JSON,
                                objectMapper.writeValueAsString(henkiloCreateDto)).getEntity().getContent())), 2).get()
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
}
