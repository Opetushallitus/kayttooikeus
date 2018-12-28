package fi.vm.sade.kayttooikeus.service.external.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.sade.javautils.http.OphHttpClient;
import fi.vm.sade.javautils.http.OphHttpEntity;
import fi.vm.sade.javautils.http.OphHttpRequest;
import fi.vm.sade.kayttooikeus.config.properties.ServiceUsersProperties;
import fi.vm.sade.kayttooikeus.service.external.RyhmasahkopostiClient;
import fi.vm.sade.properties.OphProperties;
import fi.vm.sade.ryhmasahkoposti.api.dto.EmailData;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

import static fi.vm.sade.kayttooikeus.config.HttpClientConfiguration.HTTP_CLIENT_VIESTINTA;
import static fi.vm.sade.kayttooikeus.service.external.ExternalServiceException.mapper;
import static fi.vm.sade.kayttooikeus.service.external.impl.HttpClientUtil.noContentOrNotFoundException;
import static fi.vm.sade.kayttooikeus.util.FunctionalUtils.io;
import static fi.vm.sade.kayttooikeus.util.FunctionalUtils.retrying;
import static java.util.function.Function.identity;

@Component
public class RyhmasahkopostiClientImpl implements RyhmasahkopostiClient {
    
    private final ObjectMapper objectMapper;
    private final OphHttpClient httpClient;
    private final OphProperties urlProperties;

    @Autowired
    public RyhmasahkopostiClientImpl(ObjectMapper objectMapper,
                                     @Qualifier(HTTP_CLIENT_VIESTINTA) OphHttpClient httpClient,
                                     OphProperties urlProperties,
                                     ServiceUsersProperties serviceUsersProperties) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.urlProperties = urlProperties;
    }
    
    public String sendRyhmasahkoposti(EmailData emailData) {
        String url = urlProperties.url("ryhmasahkoposti-service.email");

        OphHttpRequest request = OphHttpRequest.Builder
                .post(url)
                .setEntity(new OphHttpEntity.Builder()
                        .content(io(() -> objectMapper.writerFor(EmailData.class).writeValueAsString(emailData)).get())
                        .contentType(ContentType.APPLICATION_JSON)
                        .build())
                .build();
        Supplier<String> action = () -> httpClient.<String>execute(request)
                .expectedStatus(200)
                .mapWith(identity())
                .orElseThrow(() -> noContentOrNotFoundException(url));
        return retrying(action, 2).get().orFail(mapper(url));
    }
}