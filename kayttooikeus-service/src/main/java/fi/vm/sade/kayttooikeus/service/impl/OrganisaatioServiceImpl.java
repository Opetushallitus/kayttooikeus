package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.dto.OrganisaatioCriteriaDto;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioDto;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioWithChildrenDto;
import fi.vm.sade.kayttooikeus.dto.TextGroupMapDto;
import fi.vm.sade.kayttooikeus.dto.enumeration.OrganisaatioStatus;
import fi.vm.sade.kayttooikeus.dto.enumeration.OrganisaatioTyyppi;
import fi.vm.sade.kayttooikeus.service.OrganisaatioService;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

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
    public Collection<OrganisaatioDto> listBy(OrganisaatioCriteriaDto criteria) {
        return organisaatioClient.stream()
                .filter(getPredicate(criteria, Predicate::and))
                .map(this::mapToDto)
                .collect(toList());
    }

    private OrganisaatioDto mapToDto(OrganisaatioPerustieto from) {
        OrganisaatioDto to = new OrganisaatioDto();
        to.setOid(from.getOid());
        to.setParentOidPath(from.getParentOidPath());
        to.setNimi(new TextGroupMapDto(null, from.getNimi()));
        to.setTyypit(from.getTyypit());
        to.setStatus(from.getStatus());
        return to;
    }

    @Override
    public OrganisaatioWithChildrenDto getRootWithChildrenBy(OrganisaatioCriteriaDto criteria) {
        OrganisaatioPerustieto root = organisaatioClient.getRoot();
        Predicate<OrganisaatioPerustieto> predicate = getPredicate(criteria, Predicate::and);
        return mapToWithChildrenDto(root, predicate);
    }

    private OrganisaatioWithChildrenDto mapToWithChildrenDto(OrganisaatioPerustieto from) {
        return mapToWithChildrenDto(from, organisaatio -> true);
    }

    private OrganisaatioWithChildrenDto mapToWithChildrenDto(OrganisaatioPerustieto from, Predicate<OrganisaatioPerustieto> predicate) {
        OrganisaatioWithChildrenDto to = new OrganisaatioWithChildrenDto();
        to.setOid(from.getOid());
        to.setParentOidPath(from.getParentOidPath());
        to.setNimi(new TextGroupMapDto(null, from.getNimi()));
        to.setTyypit(from.getTyypit());
        to.setChildren(from.getChildren()
                .stream()
                .filter(predicate)
                .map(child -> mapToWithChildrenDto(child, predicate))
                .collect(toList()));
        to.setStatus(from.getStatus());
        return to;
    }

    @Override
    public OrganisaatioWithChildrenDto getByOid(String oid) {
        return organisaatioClient.getOrganisaatioPerustiedotCached(oid)
                .map(this::mapToWithChildrenDto)
                .orElseThrow(() -> new NotFoundException(oid));
    }

    private static Predicate<OrganisaatioPerustieto> getPredicate(OrganisaatioCriteriaDto criteria,
                                                                  BinaryOperator<Predicate<OrganisaatioPerustieto>> accumulator) {
        return getPredicates(criteria).stream().reduce(organisaatio -> true, accumulator);
    }

    private static Collection<Predicate<OrganisaatioPerustieto>> getPredicates(OrganisaatioCriteriaDto criteria) {
        Collection<Predicate<OrganisaatioPerustieto>> predicates = new ArrayList<>();
        Optional.ofNullable(criteria.getTyyppi()).ifPresent(tyyppi -> predicates.add(tyyppiPredicate(tyyppi)));
        Optional.ofNullable(criteria.getTila()).ifPresent(tila -> predicates.add(tilaPredicate(tila)));
        return predicates;
    }

    private static Predicate<OrganisaatioPerustieto> tyyppiPredicate(OrganisaatioTyyppi tyyppi) {
        return (OrganisaatioPerustieto organisaatio) -> {
            switch (tyyppi) {
                case ORGANISAATIO:
                    return !organisaatio.getTyypit().contains("Ryhma");
                case RYHMA:
                    return organisaatio.getTyypit().contains("Ryhma");
                default:
                    throw new IllegalArgumentException("Tuntematon tyyppi: " + tyyppi);
            }
        };
    }

    private static Predicate<OrganisaatioPerustieto> tilaPredicate(Set<OrganisaatioStatus> tila) {
        return (OrganisaatioPerustieto organisaatio) -> tila.contains(organisaatio.getStatus());
    }

}
