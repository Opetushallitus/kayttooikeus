package fi.vm.sade.kayttooikeus.service.impl;

import com.google.common.collect.Sets;
import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.enumeration.OrderByHenkilohaku;
import fi.vm.sade.kayttooikeus.repositories.criteria.KayttooikeusCriteria;
import fi.vm.sade.kayttooikeus.repositories.criteria.OrganisaatioHenkiloCriteria;
import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.dto.KayttoOikeudenTila;
import fi.vm.sade.kayttooikeus.dto.KayttooikeudetDto;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.*;
import fi.vm.sade.kayttooikeus.repositories.dto.HenkilohakuResultDto;
import fi.vm.sade.kayttooikeus.service.HenkiloService;
import fi.vm.sade.kayttooikeus.service.KayttoOikeusService;
import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.util.HenkilohakuBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HenkiloServiceImpl extends AbstractService implements HenkiloService {


    private final HenkiloHibernateRepository henkiloHibernateRepository;

    private final PermissionCheckerService permissionCheckerService;
    private final KayttoOikeusService kayttoOikeusService;

    private final KayttoOikeusRyhmaTapahtumaHistoriaDataRepository kayttoOikeusRyhmaTapahtumaHistoriaDataRepository;
    private final OrganisaatioHenkiloRepository organisaatioHenkiloRepository;
    private final MyonnettyKayttoOikeusRyhmaTapahtumaRepository myonnettyKayttoOikeusRyhmaTapahtumaRepository;
    private final LdapSynchronizationService ldapSynchronizationService;
    private final HenkiloDataRepository henkiloDataRepository;
    private final CommonProperties commonProperties;

    private final OrikaBeanMapper mapper;

    private final OrganisaatioClient organisaatioClient;

    @Override
    @Transactional(readOnly = true)
    public HenkiloReadDto getByOid(String oid) {
        return henkiloDataRepository.findByOidHenkilo(oid)
                .map(entity -> mapper.map(entity, HenkiloReadDto.class))
                .orElseThrow(() -> new NotFoundException(String.format("Henkilöä ei löytynyt OID:lla %s", oid)));
    }

    @Override
    @Transactional(readOnly = true)
    public HenkiloReadDto getByKayttajatunnus(String kayttajatunnus) {
        return henkiloDataRepository.findByKayttajatiedotUsername(kayttajatunnus)
                .map(entity -> mapper.map(entity, HenkiloReadDto.class))
                .orElseThrow(() -> new NotFoundException("Henkilöä ei löytynyt käyttäjätunnuksella " + kayttajatunnus));
    }

    @Override
    @Transactional(readOnly = true)
    public KayttooikeudetDto getKayttooikeudet(String henkiloOid, OrganisaatioHenkiloCriteria criteria) {
        // juuriorganisaatioon kuuluvalla henkilöllä on oikeus kaikkiin alla oleviin organisaatioihin
        if (this.permissionCheckerService.isCurrentUserMiniAdmin()) {
            if (criteria.getOrganisaatioOids() != null || criteria.getKayttoOikeusRyhmaNimet() != null) {
                // haetaan organisaatioon kuuluvat henkilöt
                return KayttooikeudetDto.admin(henkiloHibernateRepository.findOidsBy(criteria));
            } else {
                // henkilöllä on oikeutus kaikkiin henkilötietoihin
                return KayttooikeudetDto.admin(null);
            }
        }

        // perustapauksena henkilöllä on oikeus omien organisaatioiden henkilötietoihin
        return KayttooikeudetDto.user(henkiloHibernateRepository.findOidsBySamaOrganisaatio(henkiloOid, criteria));
    }

    @Override
    @Transactional
    public void disableHenkiloOrganisationsAndKayttooikeus(String henkiloOid, String kasittelijaOid) {
        if (StringUtils.isEmpty(kasittelijaOid)) {
            kasittelijaOid = getCurrentUserOid();
        }
        final String kasittelijaOidFinal = kasittelijaOid;
        List<OrganisaatioHenkilo> orgHenkilos = this.organisaatioHenkiloRepository.findByHenkiloOidHenkilo(henkiloOid);
        for (OrganisaatioHenkilo oh : orgHenkilos) {
            oh.setPassivoitu(true);
            Set<MyonnettyKayttoOikeusRyhmaTapahtuma> mkorts = oh.getMyonnettyKayttoOikeusRyhmas();
            if (!CollectionUtils.isEmpty(mkorts)) {
                for (Iterator<MyonnettyKayttoOikeusRyhmaTapahtuma> mkortIterator = mkorts.iterator(); mkortIterator.hasNext();) {
                    MyonnettyKayttoOikeusRyhmaTapahtuma mkort = mkortIterator.next();
                    // Create event
                    Henkilo kasittelija = this.henkiloDataRepository.findByOidHenkilo(kasittelijaOid)
                            .orElseThrow(() -> new NotFoundException("Käsittelija not found by oid " + kasittelijaOidFinal));
                    KayttoOikeusRyhmaTapahtumaHistoria deleteEvent = mkort.toHistoria(
                            kasittelija, KayttoOikeudenTila.SULJETTU,
                            LocalDateTime.now(), "Oikeuksien poisto, koko henkilön passivointi");
                    this.kayttoOikeusRyhmaTapahtumaHistoriaDataRepository.save(deleteEvent);

                    // Remove kayttooikeus
                    mkortIterator.remove();
                    this.myonnettyKayttoOikeusRyhmaTapahtumaRepository.deleteById(mkort.getId());
                }
            }
        }
        ldapSynchronizationService.updateHenkiloAsap(henkiloOid);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<HenkilohakuResultDto> henkilohaku(HenkilohakuCriteriaDto henkilohakuCriteriaDto,
                                                        Long offset,
                                                        OrderByHenkilohaku orderBy) {
        return new HenkilohakuBuilder(this.henkiloHibernateRepository, this.mapper, this.permissionCheckerService,
                this.henkiloDataRepository, this.organisaatioClient, this.organisaatioHenkiloRepository, this.commonProperties)
                .builder(henkilohakuCriteriaDto)
                .exclusion()
                .search(offset, orderBy)
                .enrichment()
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Long henkilohakuCount(HenkilohakuCriteriaDto henkiloHakuCriteriaDto) {
        return new HenkilohakuBuilder(this.henkiloHibernateRepository, this.mapper, this.permissionCheckerService,
                this.henkiloDataRepository, this.organisaatioClient, this.organisaatioHenkiloRepository, this.commonProperties)
                .builder(henkiloHakuCriteriaDto)
                .exclusion()
                .searchCount()
                .buildHakuResultCount();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isVahvastiTunnistettu(String oidHenkilo) {
        return BooleanUtils.isTrue(this.henkiloDataRepository.findByOidHenkilo(oidHenkilo)
                .orElseThrow(() -> new NotFoundException("Henkilo not found with oid " + oidHenkilo))
                .getVahvastiTunnistettu());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isVahvastiTunnistettuByUsername(String username) {
        return BooleanUtils.isTrue(this.henkiloDataRepository.findByKayttajatiedotUsername(username)
                .orElseThrow(() -> new NotFoundException("Henkilo not found with username " + username))
                .getVahvastiTunnistettu());
    }

    @Override
    @Transactional
    public void updateHenkiloToLdap(String oid, LdapSynchronizationService.LdapSynchronizationType ldapSynchronization) {
        Henkilo henkilo = henkiloDataRepository.findByOidHenkilo(oid)
                .orElseThrow(() -> new NotFoundException(String.format("Henkilöä ei löytynyt OID:lla %s", oid)));
        ldapSynchronization.getAction().accept(ldapSynchronizationService, henkilo.getOidHenkilo());
    }

    @Override
    @Transactional(readOnly = true)
    public OmatTiedotDto getOmatTiedot() {
        OmatTiedotDto omatTiedotDto = this.mapper.map(this.kayttoOikeusService
                        .listMyonnettyKayttoOikeusForUser(KayttooikeusCriteria.builder()
                                        .oidHenkilo(this.permissionCheckerService.getCurrentUserOid())
                                        .build(),
                                null,
                                null).stream()
                        .findFirst()
                        .orElseGet(() -> KayttooikeusPerustiedotDto.builder()
                                .oidHenkilo(this.permissionCheckerService.getCurrentUserOid())
                                .organisaatiot(Sets.newHashSet())
                                .build()),
                OmatTiedotDto.class);
        omatTiedotDto.setIsAdmin(this.permissionCheckerService.isCurrentUserAdmin());
        omatTiedotDto.setIsMiniAdmin(this.permissionCheckerService.isCurrentUserMiniAdmin());
        return omatTiedotDto;
    }

}
