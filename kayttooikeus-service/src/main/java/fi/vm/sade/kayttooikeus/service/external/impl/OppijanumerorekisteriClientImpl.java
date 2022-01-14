package fi.vm.sade.kayttooikeus.service.external.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.sade.javautils.http.OphHttpClient;
import fi.vm.sade.javautils.http.OphHttpEntity;
import fi.vm.sade.javautils.http.OphHttpRequest;
import fi.vm.sade.kayttooikeus.service.dto.HenkiloVahvaTunnistusDto;
import fi.vm.sade.kayttooikeus.service.dto.HenkiloYhteystiedotDto;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.external.ExternalServiceException;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.kayttooikeus.service.impl.KayttoOikeusServiceImpl;
import fi.vm.sade.kayttooikeus.util.UserDetailsUtil;
import fi.vm.sade.oppijanumerorekisteri.dto.*;
import fi.vm.sade.properties.OphProperties;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static fi.vm.sade.kayttooikeus.config.HttpClientConfiguration.HTTP_CLIENT_OPPIJANUMEROREKISTERI;
import static fi.vm.sade.kayttooikeus.service.external.ExternalServiceException.mapper;
import static fi.vm.sade.kayttooikeus.service.external.impl.HttpClientUtil.noContentOrNotFoundException;
import static fi.vm.sade.kayttooikeus.util.FunctionalUtils.io;
import static fi.vm.sade.kayttooikeus.util.FunctionalUtils.retrying;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Component
public class OppijanumerorekisteriClientImpl implements OppijanumerorekisteriClient {

    private final ObjectMapper objectMapper;
    private final OphHttpClient httpClient;
    private final OphProperties urlProperties;

    @Autowired
    public OppijanumerorekisteriClientImpl(ObjectMapper objectMapper,
                                           @Qualifier(HTTP_CLIENT_OPPIJANUMEROREKISTERI) OphHttpClient httpClient,
                                           OphProperties urlProperties) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.urlProperties = urlProperties;
    }

    @Override
    public List<HenkiloPerustietoDto> getHenkilonPerustiedot(Collection<String> henkiloOid) {
        if (henkiloOid.isEmpty()) {
            return new ArrayList<>();
        }
        String url = urlProperties.url("oppijanumerorekisteri-service.henkilo.henkiloPerustietosByHenkiloOidList");
        OphHttpRequest request = OphHttpRequest.Builder
                .post(url)
                .setEntity(new OphHttpEntity.Builder()
                        .content(io(() -> objectMapper.writeValueAsString(henkiloOid)).get())
                        .contentType(ContentType.APPLICATION_JSON)
                        .build())
                .build();
        Supplier<List<HenkiloPerustietoDto>> action = () -> httpClient.<HenkiloPerustietoDto[]>execute(request)
                .expectedStatus(200)
                .mapWith(json -> io(() -> objectMapper.readValue(json, HenkiloPerustietoDto[].class)).get())
                .map(array -> Arrays.stream(array).collect(toList()))
                .orElseThrow(() -> noContentOrNotFoundException(url));
        return retrying(action, 2).get().orFail(mapper(url));
    }

    @Override
    public Set<String> getAllOidsForSamePerson(String personOid) {
        String url = urlProperties.url("oppijanumerorekisteri-service.s2s.duplicateHenkilos");
        Map<String,Object> criteria = new HashMap<>();
        criteria.put("henkiloOids", singletonList(personOid));
        OphHttpRequest request = OphHttpRequest.Builder
                .post(url)
                .setEntity(new OphHttpEntity.Builder()
                        .content(io(() -> objectMapper.writeValueAsString(criteria)).get())
                        .contentType(ContentType.APPLICATION_JSON)
                        .build())
                .build();
        Supplier<List<HenkiloViiteDto>> action = () -> httpClient.<HenkiloViiteDto[]>execute(request)
                .expectedStatus(200)
                .mapWith(json -> io(() -> objectMapper.readValue(json, HenkiloViiteDto[].class)).get())
                .map(array -> Arrays.stream(array).collect(toList()))
                .orElseThrow(() -> noContentOrNotFoundException(url));
        return Stream.concat(Stream.of(personOid), retrying(action, 2).get().orFail(mapper(url))
                .stream().flatMap(viite -> Stream.of(viite.getHenkiloOid(), viite.getMasterOid()))).collect(toSet());
    }

    @Override
    public String getOidByHetu(String hetu) {
        String url = urlProperties.url("oppijanumerorekisteri-service.s2s.oidByHetu", hetu);
        Supplier<String> action = () -> httpClient.<String>execute(OphHttpRequest.Builder.get(url).build())
                .expectedStatus(200)
                .mapWith(identity())
                .orElseThrow(() -> new NotFoundException("could not find oid with hetu: " + hetu));
        return retrying(action, 2).get().orFail(mapper(url));
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
        OphHttpRequest request = OphHttpRequest.Builder
                .post(url)
                .setEntity(new OphHttpEntity.Builder()
                        .content(data)
                        .contentType(ContentType.APPLICATION_JSON)
                        .build())
                .build();
        Supplier<List<HenkiloHakuPerustietoDto>> action = () -> httpClient.<HenkiloHakuPerustietoDto[]>execute(request)
                .expectedStatus(200)
                .mapWith(json -> io(() -> objectMapper.readValue(json, HenkiloHakuPerustietoDto[].class)).get())
                .map(array -> Arrays.stream(array).collect(toList()))
                .orElseThrow(() -> noContentOrNotFoundException(url));
        return retrying(action, 2).get().orFail(mapper(url));
    }

    @Override
    public List<String> getModifiedSince(LocalDateTime dateTime, long offset, long amount) {
        Map<String, String> params = new HashMap<String, String>() {{
            put("offset", Long.toString(offset));
            put("amount", Long.toString(amount));
        }};
        String url = this.urlProperties.url("oppijanumerorekisteri-service.s2s.modified-since", dateTime, params);
        Supplier<List<String>> action = () -> httpClient.<String[]>execute(OphHttpRequest.Builder.get(url).build())
                .expectedStatus(200)
                .mapWith(json -> io(() -> objectMapper.readValue(json, String[].class)).get())
                .map(array -> Arrays.stream(array).collect(toList()))
                .orElseThrow(() -> noContentOrNotFoundException(url));
        return retrying(action, 2).get().orFail(e -> new ExternalServiceException(url, e.getMessage(), e));
    }

    @Override
    public HenkiloDto getHenkiloByOid(String oid) {
        String url = this.urlProperties.url("oppijanumerorekisteri-service.henkilo.henkiloByOid", oid);

        Supplier<HenkiloDto> action = () -> httpClient.<HenkiloDto>execute(OphHttpRequest.Builder.get(url).build())
                .expectedStatus(200)
                .mapWith(json -> io(() -> objectMapper.readValue(json, HenkiloDto.class)).get())
                .orElseThrow(() -> noContentOrNotFoundException(url));
        return retrying(action, 2).get().orFail(mapper(url));
    }

    @Override
    public Optional<HenkiloDto> findHenkiloByOid(String oid) {
        String url = this.urlProperties.url("oppijanumerorekisteri-service.henkilo.henkiloByOid", oid);

        Supplier<Optional<HenkiloDto>> action = () -> httpClient.<HenkiloDto>execute(OphHttpRequest.Builder.get(url).build())
                .expectedStatus(200)
                .mapWith(json -> io(() -> objectMapper.readValue(json, HenkiloDto.class)).get());
        return retrying(action, 2).get().orFail(mapper(url));
    }

    @Override
    public Optional<HenkiloDto> getHenkiloByHetu(String hetu) {
        String url = this.urlProperties.url("oppijanumerorekisteri-service.henkilo.henkiloByHetu", hetu);
        return httpClient.<HenkiloDto>execute(OphHttpRequest.Builder.get(url).build())
                .expectedStatus(200)
                .mapWith(json -> io(() -> objectMapper.readValue(json, HenkiloDto.class)).get());
    }

    @Override
    public Collection<HenkiloYhteystiedotDto> listYhteystiedot(HenkiloHakuCriteria criteria) {
        String url = urlProperties.url("oppijanumerorekisteri-service.henkilo.yhteystiedot");
        OphHttpRequest request = OphHttpRequest.Builder
                .post(url)
                .setEntity(new OphHttpEntity.Builder()
                        .content(io(() -> objectMapper.writeValueAsString(criteria)).get())
                        .contentType(ContentType.APPLICATION_JSON)
                        .build())
                .build();
        Supplier<Collection<HenkiloYhteystiedotDto>> action = () -> httpClient.<HenkiloYhteystiedotDto[]>execute(request)
                .expectedStatus(200)
                .mapWith(json -> io(() -> objectMapper.readValue(json, HenkiloYhteystiedotDto[].class)).get())
                .map(array -> Arrays.stream(array).collect(toList()))
                .orElseThrow(() -> noContentOrNotFoundException(url));
        return retrying(action, 2).get().orFail(mapper(url));
    }

    @Override
    public String createHenkilo(HenkiloCreateDto henkiloCreateDto) {
        String url = this.urlProperties.url("oppijanumerorekisteri-service.henkilo");
        OphHttpRequest request = OphHttpRequest.Builder
                .post(url)
                .setEntity(new OphHttpEntity.Builder()
                        .content(io(() -> objectMapper.writeValueAsString(henkiloCreateDto)).get())
                        .contentType(ContentType.APPLICATION_JSON)
                        .build())
                .build();
        Supplier<String> action = () -> httpClient.<String>execute(request)
                .expectedStatus(201)
                .mapWith(identity())
                .orElseThrow(() -> noContentOrNotFoundException(url));
        return retrying(action, 2).get().orFail(mapper(url));
    }

    @Override
    public void setStrongIdentifiedHetu(String oidHenkilo, HenkiloVahvaTunnistusDto henkiloVahvaTunnistusDto) {
        String url = this.urlProperties.url("oppijanumerorekisteri-service.cas.vahva-tunnistus", oidHenkilo);
        OphHttpRequest request = OphHttpRequest.Builder
                .put(url)
                .setEntity(new OphHttpEntity.Builder()
                        .content(io(() -> objectMapper.writeValueAsString(henkiloVahvaTunnistusDto)).get())
                        .contentType(ContentType.APPLICATION_JSON)
                        .build())
                .build();
        Supplier<String> action = () -> httpClient.<String>execute(request)
                .expectedStatus(200)
                .mapWith(identity())
                .orElseThrow(() -> noContentOrNotFoundException(url));
        retrying(action, 2).get().orFail(mapper(url));
    }

    @Override
    public void updateHenkilo(HenkiloUpdateDto henkiloUpdateDto) {
        String url = this.urlProperties.url("oppijanumerorekisteri-service.henkilo");
        OphHttpRequest request = OphHttpRequest.Builder
                .put(url)
                .setEntity(new OphHttpEntity.Builder()
                        .content(io(() -> objectMapper.writeValueAsString(henkiloUpdateDto)).get())
                        .contentType(ContentType.APPLICATION_JSON)
                        .build())
                .build();
        Supplier<String> action = () -> httpClient.<String>execute(request)
                .expectedStatus(200)
                .mapWith(identity())
                .orElseThrow(() -> noContentOrNotFoundException(url));
        retrying(action, 2).get().orFail(mapper(url));
    }

    @Override
    public void yhdistaHenkilot(String oid, Collection<String> duplicateOids) {
        String url = urlProperties.url("oppijanumerorekisteri-service.henkilo.byOid.yhdistaHenkilot", oid);
        OphHttpRequest request = OphHttpRequest.Builder
                .post(url)
                .setEntity(new OphHttpEntity.Builder()
                        .content(io(() -> objectMapper.writeValueAsString(duplicateOids)).get())
                        .contentType(ContentType.APPLICATION_JSON)
                        .build())
                .build();
        Supplier<String> action = () -> httpClient.<String>execute(request)
                .expectedStatus(200)
                .mapWith(identity())
                .orElseThrow(() -> noContentOrNotFoundException(url));
        retrying(action, 2).get().orFail(mapper(url));
    }

    @Override
    public HenkiloOmattiedotDto getOmatTiedot(String oidHenkilo) {
        String url = this.urlProperties.url("oppijanumerorekisteri.henkilo.omattiedot-by-oid", oidHenkilo);
        Supplier<HenkiloOmattiedotDto> action = () -> httpClient.<HenkiloOmattiedotDto>execute(OphHttpRequest.Builder.get(url).build())
                .expectedStatus(200)
                .mapWith(json -> io(() -> objectMapper.readValue(json, HenkiloOmattiedotDto.class)).get())
                .orElseThrow(() -> noContentOrNotFoundException(url));
        return retrying(action, 2).get().orFail(mapper(url));
    }

    @Override
    public String resolveLanguageCodeForCurrentUser() {
        try {
            String currentUserOid = UserDetailsUtil.getCurrentUserOid();
            HenkiloDto currentUser = getHenkiloByOid(currentUserOid);
            String languageCode = UserDetailsUtil.getLanguageCode(currentUser);
            return languageCode.toUpperCase();
        } catch ( Exception e ) {
            return KayttoOikeusServiceImpl.FI;
        }
    }

    @Getter @Setter
    public static class HenkiloViiteDto {
        private String henkiloOid;
        private String masterOid;
    }

}
