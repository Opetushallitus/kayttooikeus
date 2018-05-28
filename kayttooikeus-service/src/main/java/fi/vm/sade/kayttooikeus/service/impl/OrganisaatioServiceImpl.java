package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.model.OrganisaatioCache;
import fi.vm.sade.kayttooikeus.repositories.OrganisaatioCacheRepository;
import fi.vm.sade.kayttooikeus.service.OrganisaatioService;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

import fi.vm.sade.kayttooikeus.dto.enumeration.OrganisaatioStatus;
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
    private static final int BATCH_SIZE = 30;

    private final OrganisaatioClient organisaatioClient;
    private final OrganisaatioCacheRepository organisaatioCacheRepository;
    private final CommonProperties commonProperties;

    @Override
    public void updateOrganisaatioCache() {
        LOGGER.info("Organisaatiocachen päivitys aloitetaan");
        List<OrganisaatioPerustieto> activeOrganisaatiotWithoutRootOrg = organisaatioClient.refreshCache().stream()
                .filter(organisaatioPerustieto -> OrganisaatioStatus.AKTIIVINEN.equals(organisaatioPerustieto.getStatus()))
                .collect(Collectors.toList());

        // The only reason keeping this is if old authentication-service still uses this.
        List<OrganisaatioCache> entities = toEntities(
                commonProperties.getRootOrganizationOid(), activeOrganisaatiotWithoutRootOrg, new ArrayDeque<>());

        organisaatioCacheRepository.deleteAllInBatch();
        organisaatioCacheRepository.persistInBatch(entities, BATCH_SIZE);
        LOGGER.info("Organisaatiocachen päivitys päättyy: tallennettiin {} organisaatiota", entities.size());
    }

    @Override
    public Long getClientCacheState() {
        return this.organisaatioClient.getCacheOrganisationCount();
    }

    private static List<OrganisaatioCache> toEntities(String oid, List<OrganisaatioPerustieto> children, Deque<String> parentOidPath) {
        parentOidPath.addFirst(oid);
        String oidPath = parentOidPath.stream().collect(joining("/"));

        OrganisaatioCache entity = new OrganisaatioCache();
        entity.setOrganisaatioOid(oid);
        entity.setOrganisaatioOidPath(oidPath);

        List<OrganisaatioCache> entities = new ArrayList<>();
        entities.add(entity);
        children.stream()
                .filter(organisaatioPerustieto -> OrganisaatioStatus.AKTIIVINEN.equals(organisaatioPerustieto.getStatus()))
                .forEach(child -> entities.addAll(toEntities(
                child.getOid(), child.getChildren(), new ArrayDeque<>(parentOidPath))));
        return entities;
    }

    @Override
    public void throwIfActiveNotFound(String organisaatioOid) {
        if(!this.organisaatioClient.activeExists(organisaatioOid)) {
            throw new ValidationException("Active organisation not found with oid " + organisaatioOid);
        }
    }

}
