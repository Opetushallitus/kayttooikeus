package fi.vm.sade.kayttooikeus.util;

import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.dto.HenkilohakuCriteriaDto;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioMinimalDto;
import fi.vm.sade.kayttooikeus.enumeration.OrderByHenkilohaku;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.OrganisaatioHenkilo;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.repositories.HenkiloHibernateRepository;
import fi.vm.sade.kayttooikeus.repositories.OrganisaatioHenkiloRepository;
import fi.vm.sade.kayttooikeus.repositories.criteria.HenkiloCriteria;
import fi.vm.sade.kayttooikeus.repositories.dto.HenkilohakuResultDto;
import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class HenkilohakuBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(HenkilohakuBuilder.class);

    private HenkilohakuCriteriaDto henkilohakuCriteriaDto;
    private List<HenkilohakuResultDto> henkilohakuResultDtoList = new ArrayList<>();

    private final HenkiloHibernateRepository henkiloHibernateRepository;
    private final OrikaBeanMapper mapper;
    private final PermissionCheckerService permissionCheckerService;
    private final HenkiloDataRepository henkiloDataRepository;
    private final OrganisaatioClient organisaatioClient;
    private final OrganisaatioHenkiloRepository organisaatioHenkiloRepository;
    private final CommonProperties commonProperties;

    public HenkilohakuBuilder builder(HenkilohakuCriteriaDto henkilohakuCriteriaDto) {
        this.henkilohakuCriteriaDto = henkilohakuCriteriaDto;
        this.henkilohakuResultDtoList = new ArrayList<>();
        return this;
    }

    public List<HenkilohakuResultDto> build() {
        return this.henkilohakuResultDtoList;
    }

    // Find nimi, kayttajatunnus and oidHenkilo
    public HenkilohakuBuilder search(Long offset, OrderByHenkilohaku orderBy) {
        // Because jpaquery limitations this can't be done with subqueries and union all.
        // This needs to be done in 2 queries because postgres query planner can't optimise it correctly because of
        // kayttajatiedot outer join and where or combination.
        HenkiloCriteria henkiloCriteria = this.mapper.map(this.henkilohakuCriteriaDto, HenkiloCriteria.class);
        this.henkilohakuResultDtoList = this.henkiloHibernateRepository.findByUsername(henkiloCriteria, offset);
        this.henkilohakuResultDtoList.addAll(this.henkiloHibernateRepository
                .findByCriteria(henkiloCriteria,
                        offset,
                        orderBy != null ? orderBy.getValue() : null));
        return this;
    }

    // Remove henkilos the user has no access (and who have organisation)
    public HenkilohakuBuilder exclusion() {
        // vain rekisterinpitäjä saa hakea henkilöitä, joilla ei ole organisaatiota
        if (!permissionCheckerService.isCurrentUserAdmin()
                && Boolean.TRUE.equals(henkilohakuCriteriaDto.getNoOrganisation())) {
            LOGGER.warn(String.format("Käyttäjällä %s ei ole oikeuksia hakea henkilöitä, joilla ei ole organisaatiota", permissionCheckerService.getCurrentUserOid()));
            henkilohakuCriteriaDto.setNoOrganisation(null);
        }

        List<String> currentUserOrganisaatioOids = this.organisaatioHenkiloRepository
                .findDistinctOrganisaatiosForHenkiloOid(this.permissionCheckerService.getCurrentUserOid());

        // Normal user
        if (!this.permissionCheckerService.isCurrentUserMiniAdmin()) {
            Set<String> allCurrentUserOrganisaatioOids = new HashSet<>(currentUserOrganisaatioOids);

            // haetaan myös aliorganisaatioista
            if (Boolean.TRUE.equals(henkilohakuCriteriaDto.getSubOrganisation())) {
                currentUserOrganisaatioOids.stream()
                        .flatMap(currentUserOrganisaatioOid ->
                                this.organisaatioClient.getChildOids(currentUserOrganisaatioOid).stream())
                        .forEach(allCurrentUserOrganisaatioOids::add);
            }

            // suodatetaan annetuilla organisaatioilla
            if (henkilohakuCriteriaDto.getOrganisaatioOids() != null) {
                allCurrentUserOrganisaatioOids.retainAll(henkilohakuCriteriaDto.getOrganisaatioOids());
            }

            henkilohakuCriteriaDto.setOrganisaatioOids(allCurrentUserOrganisaatioOids);
        }
        else {
            // root-virkailija hakee ilman aliorganisaatioita
            if (!Boolean.TRUE.equals(henkilohakuCriteriaDto.getSubOrganisation())
                    && henkilohakuCriteriaDto.getOrganisaatioOids() == null) {
                henkilohakuCriteriaDto.setOrganisaatioOids(new HashSet<>(currentUserOrganisaatioOids));
            }
        }
        return this;
    }

    // Find organisaatioNimiList
    public HenkilohakuBuilder enrichment() {
        List<String> oidList = this.henkilohakuResultDtoList.stream().map(HenkilohakuResultDto::getOidHenkilo).collect(Collectors.toList());
        List<Henkilo> henkiloList = this.henkiloDataRepository.readByOidHenkiloIn(oidList);
        this.henkilohakuResultDtoList = this.henkilohakuResultDtoList.stream().map(henkilohakuResultDto -> {
            Henkilo henkilo = henkiloList.stream().filter(henkilo1 -> Objects.equals(henkilo1.getOidHenkilo(), henkilohakuResultDto.getOidHenkilo()))
                    .findFirst()
                    .orElseThrow(IllegalStateException::new);
            henkilohakuResultDto.setOrganisaatioNimiList(henkilo.getOrganisaatioHenkilos().stream()
                    .filter(((Predicate<OrganisaatioHenkilo>) OrganisaatioHenkilo::isPassivoitu).negate())
                    .map(OrganisaatioHenkilo::getOrganisaatioOid)
                    .map(organisaatioOid -> {
                        OrganisaatioPerustieto organisaatio = this.organisaatioClient.getOrganisaatioPerustiedotCached(organisaatioOid)
                                .orElseGet(() -> UserDetailsUtil.createUnknownOrganisation(organisaatioOid));
                        return new OrganisaatioMinimalDto(organisaatioOid, organisaatio.getTyypit(), organisaatio.getNimi());
                    })
                    .collect(Collectors.toList()));
            return henkilohakuResultDto;
        }).collect(Collectors.toList());

        return this;
    }

}
