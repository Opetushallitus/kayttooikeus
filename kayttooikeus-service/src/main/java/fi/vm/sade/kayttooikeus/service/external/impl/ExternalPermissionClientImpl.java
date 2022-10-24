package fi.vm.sade.kayttooikeus.service.external.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.sade.javautils.httpclient.OphHttpClient;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.ExternalPermissionService;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.PermissionCheckRequestDto;
import fi.vm.sade.kayttooikeus.dto.permissioncheck.PermissionCheckResponseDto;
import fi.vm.sade.kayttooikeus.service.external.ExternalPermissionClient;
import fi.vm.sade.properties.OphProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static fi.vm.sade.javautils.httpclient.OphHttpClient.JSON;
import static fi.vm.sade.javautils.httpclient.OphHttpClient.UTF8;
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
        SERVICE_URIS.put(ExternalPermissionService.VARDA, properties.url("varda.external-permission-check"));
        SERVICE_URIS.put(ExternalPermissionService.YKI, properties.url("yki.external-permission-check"));
    }

    @Override
    public PermissionCheckResponseDto getPermission(ExternalPermissionService service, PermissionCheckRequestDto requestDto) {
        String url = requireNonNull(SERVICE_URIS.get(service), "service uri puuttuu: " + service);
        return httpClient.post(url)
                .dataWriter(JSON, UTF8, out -> out.write(objectMapper.writeValueAsString(requestDto)))
                .execute(response -> objectMapper.readValue(response.asInputStream(), PermissionCheckResponseDto.class));
    }

}
