package fi.vm.sade.kayttooikeus.service.external.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.sade.javautils.http.OphHttpClient;
import fi.vm.sade.javautils.http.OphHttpEntity;
import fi.vm.sade.javautils.http.OphHttpRequest;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.ExternalPermissionService;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.PermissionCheckRequestDto;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.PermissionCheckResponseDto;
import fi.vm.sade.kayttooikeus.service.external.ExternalPermissionClient;
import fi.vm.sade.properties.OphProperties;
import org.apache.http.entity.ContentType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static fi.vm.sade.kayttooikeus.service.external.impl.HttpClientUtil.noContentOrNotFoundException;
import static fi.vm.sade.kayttooikeus.util.FunctionalUtils.io;
import static java.util.Objects.requireNonNull;

@Component
public class ExternalPermissionClientImpl implements ExternalPermissionClient {

    private final OphHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<ExternalPermissionService, String> SERVICE_URIS = new HashMap<>();

    public ExternalPermissionClientImpl(OphHttpClient httpClient, OphProperties properties, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;

        SERVICE_URIS.put(ExternalPermissionService.HAKU_APP, properties.url("haku-app.external-permission-check"));
        SERVICE_URIS.put(ExternalPermissionService.SURE, properties.url("suoritusrekisteri.external-permission-check"));
        SERVICE_URIS.put(ExternalPermissionService.ATARU, properties.url("ataru-editori.external-permission-check"));
        SERVICE_URIS.put(ExternalPermissionService.KOSKI, properties.url("koski.external-permission-check"));
    }

    @Override
    public PermissionCheckResponseDto getPermission(ExternalPermissionService service, PermissionCheckRequestDto requestDto) {
        String url = requireNonNull(SERVICE_URIS.get(service), "service uri puuttuu: " + service);
        OphHttpRequest request = OphHttpRequest.Builder
                .post(url)
                .setEntity(new OphHttpEntity.Builder()
                        .content(io(() -> objectMapper.writeValueAsString(requestDto)).get())
                        .contentType(ContentType.APPLICATION_JSON)
                        .build())
                .build();
        return httpClient.<PermissionCheckResponseDto>execute(request)
                .expectedStatus(200)
                .mapWith(json -> io(() -> objectMapper.readValue(json, PermissionCheckResponseDto.class)).get())
                .orElseThrow(() -> noContentOrNotFoundException(url));
    }

}
