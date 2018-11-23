package fi.vm.sade.kayttooikeus.util;

import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.dto.KutsuReadDto;
import fi.vm.sade.kayttooikeus.dto.enumeration.KutsuView;
import fi.vm.sade.kayttooikeus.enumeration.KutsuOrganisaatioOrder;
import fi.vm.sade.kayttooikeus.model.KayttoOikeusRyhma;
import fi.vm.sade.kayttooikeus.model.MyonnettyKayttoOikeusRyhmaTapahtuma;
import fi.vm.sade.kayttooikeus.repositories.KutsuRepository;
import fi.vm.sade.kayttooikeus.repositories.MyonnettyKayttoOikeusRyhmaTapahtumaRepository;
import fi.vm.sade.kayttooikeus.repositories.OrganisaatioHenkiloRepository;
import fi.vm.sade.kayttooikeus.repositories.criteria.KutsuCriteria;
import fi.vm.sade.kayttooikeus.service.LocalizationService;
import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.*;
import static java.util.Collections.singletonList;

@RequiredArgsConstructor
public class KutsuHakuBuilder {
    private final PermissionCheckerService permissionCheckerService;
    private final LocalizationService localizationService;

    private final CommonProperties commonProperties;

    private final MyonnettyKayttoOikeusRyhmaTapahtumaRepository myonnettyKayttoOikeusRyhmaTapahtumaRepository;
    private final KutsuRepository kutsuRepository;
    private final OrganisaatioHenkiloRepository organisaatioHenkiloRepository;

    private final OrikaBeanMapper mapper;

    private final OrganisaatioClient organisaatioClient;

    private final KutsuCriteria kutsuCriteria;

    private List<KutsuReadDto> result;

    public KutsuHakuBuilder prepareByAuthority() {
        this.prepareCommon();
        if (this.permissionCheckerService.isCurrentUserAdmin()) {
            return this.prepareForAdmin();
        }
        else if (this.permissionCheckerService.isCurrentUserMiniAdmin(PALVELU_HENKILONHALLINTA, ROLE_CRUD)
                || permissionCheckerService.isCurrentUserMiniAdmin(PALVELU_KAYTTOOIKEUS, ROLE_CRUD)) {
            return prepareForMiniAdmin();
        }
        return prepareForNormalUser();
    }

    private KutsuHakuBuilder prepareForAdmin() {

        return this;
    }

    private KutsuHakuBuilder prepareForMiniAdmin() {
        // Force OPH-view
        this.kutsuCriteria.setKutsujaOrganisaatioOid(this.commonProperties.getRootOrganizationOid());

        return this;
    }

    private KutsuHakuBuilder prepareForNormalUser() {

        // Limit organisaatio search for non-admin users
        Set<String> organisaatioOidLimit;

        Map<String, List<String>> palveluRoolit = new HashMap<>();
        palveluRoolit.put(PALVELU_HENKILONHALLINTA, singletonList(ROLE_CRUD));
        palveluRoolit.put(PALVELU_KAYTTOOIKEUS, singletonList(ROLE_CRUD));

        if (!CollectionUtils.isEmpty(this.kutsuCriteria.getOrganisaatioOids())) {
            organisaatioOidLimit = this.permissionCheckerService
                    .hasOrganisaatioInHierarchy(this.kutsuCriteria.getOrganisaatioOids(), palveluRoolit);
        }
        else {
            organisaatioOidLimit = this.permissionCheckerService.getCurrentUserOrgnisationsWithPalveluRole(palveluRoolit);
        }
        if (BooleanUtils.isTrue(this.kutsuCriteria.getSubOrganisations())) {
            organisaatioOidLimit = organisaatioOidLimit.stream()
                    .flatMap(organisaatioOid -> this.organisaatioClient.listWithChildOids(organisaatioOid,
                            new OrganisaatioMyontoPredicate()).stream())
                    .collect(Collectors.toSet());
        }
        this.kutsuCriteria.setOrganisaatioOids(organisaatioOidLimit);

        return this;
    }

    private void prepareCommon() {
        if (KutsuView.OPH.equals(this.kutsuCriteria.getView())) {
            this.kutsuCriteria.setKutsujaOrganisaatioOid(this.commonProperties.getRootOrganizationOid());
            this.kutsuCriteria.setSubOrganisations(false);
        }
        if (KutsuView.ONLY_OWN_KUTSUS.equals(this.kutsuCriteria.getView())) {
            this.kutsuCriteria.setKutsujaOid(this.permissionCheckerService.getCurrentUserOid());
        }
        if (KutsuView.KAYTTOOIKEUSRYHMA.equals(this.kutsuCriteria.getView())) {
            this.kutsuCriteria.setKutsujaKayttooikeusryhmaIds(this.myonnettyKayttoOikeusRyhmaTapahtumaRepository
                    .findValidMyonnettyKayttooikeus(this.permissionCheckerService.getCurrentUserOid()).stream()
                    .map(MyonnettyKayttoOikeusRyhmaTapahtuma::getKayttoOikeusRyhma)
                    .map(KayttoOikeusRyhma::getId)
                    .collect(Collectors.toSet()));
        }
    }


    public KutsuHakuBuilder doSearch(KutsuOrganisaatioOrder sortBy, Sort.Direction direction, Long offset, Long amount) {
        this.result = this.mapper.mapAsList(this.kutsuRepository.listKutsuListDtos(this.kutsuCriteria,
                sortBy.getSortWithDirection(direction), offset, amount), KutsuReadDto.class);
        return this;
    }

    public KutsuHakuBuilder localise() {
        this.result.forEach(kutsuReadDto -> this.localizationService.localizeOrgs(kutsuReadDto.getOrganisaatiot()));
        return this;
    }

    public List<KutsuReadDto> build() {
        return this.result;
    }
}
