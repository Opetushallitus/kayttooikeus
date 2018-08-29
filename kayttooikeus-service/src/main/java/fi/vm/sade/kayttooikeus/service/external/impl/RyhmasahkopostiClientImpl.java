package fi.vm.sade.kayttooikeus.service.external.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.sade.generic.rest.CachingRestClient;
import fi.vm.sade.kayttooikeus.config.properties.ServiceUsersProperties;
import fi.vm.sade.kayttooikeus.service.external.RyhmasahkopostiClient;
import fi.vm.sade.properties.OphProperties;
import fi.vm.sade.ryhmasahkoposti.api.dto.EmailData;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static fi.vm.sade.kayttooikeus.service.external.ExternalServiceException.mapper;
import static fi.vm.sade.kayttooikeus.util.FunctionalUtils.io;
import static fi.vm.sade.kayttooikeus.util.FunctionalUtils.retrying;

@Component
public class RyhmasahkopostiClientImpl implements RyhmasahkopostiClient {
    private static final Logger logger = LoggerFactory.getLogger(RyhmasahkopostiClientImpl.class);
    
    private final ObjectMapper objectMapper;
    private final OphProperties urlProperties;
    private final CachingRestClient restClient;

    @Autowired
    public RyhmasahkopostiClientImpl(ObjectMapper objectMapper, OphProperties urlProperties,
                                     ServiceUsersProperties serviceUsersProperties) {
        this.objectMapper = objectMapper;
        this.urlProperties = urlProperties;
        this.restClient = new CachingRestClient().setClientSubSystemCode("kayttooikeus.kayttooikeuspalvelu-service");
        this.restClient.setWebCasUrl(urlProperties.url("cas.url"));
        this.restClient.setCasService(urlProperties.url("ryhmasahkoposti-service.security-check"));
        this.restClient.setUsername(serviceUsersProperties.getViestinta().getUsername());
        this.restClient.setPassword(serviceUsersProperties.getViestinta().getPassword());
    }
    
    public HttpResponse sendRyhmasahkoposti(EmailData emailData) {
        String url = urlProperties.url("ryhmasahkoposti-service.email");
        return retrying(io(() -> restClient.post(url, "application/json",
                    objectMapper.writerFor(EmailData.class).writeValueAsString(emailData))), 2)
            .get().orFail(mapper(url));
    }
}