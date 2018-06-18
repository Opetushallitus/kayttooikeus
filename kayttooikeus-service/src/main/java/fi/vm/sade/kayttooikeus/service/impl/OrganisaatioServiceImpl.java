package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.service.OrganisaatioService;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ValidationException;

@Service
@Transactional
@RequiredArgsConstructor
public class OrganisaatioServiceImpl implements OrganisaatioService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrganisaatioServiceImpl.class);

    private final OrganisaatioClient organisaatioClient;

    @Override
    public void updateOrganisaatioCache() {
        LOGGER.info("Organisaatiocachen päivitys aloitetaan");
        long maara = organisaatioClient.refreshCache();
        LOGGER.info("Organisaatiocachen päivitys päättyy: tallennettiin {} organisaatiota", maara);
    }

    @Override
    public Long getClientCacheState() {
        return this.organisaatioClient.getCacheOrganisationCount();
    }

    @Override
    public void throwIfActiveNotFound(String organisaatioOid) {
        if(!this.organisaatioClient.activeExists(organisaatioOid)) {
            throw new ValidationException("Active organisation not found with oid " + organisaatioOid);
        }
    }

}
