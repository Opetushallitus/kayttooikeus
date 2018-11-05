package fi.vm.sade.kayttooikeus.service.impl;

import com.google.common.collect.Sets;
import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.dto.enumeration.LogInRedirectType;
import fi.vm.sade.kayttooikeus.enumeration.OrderByHenkilohaku;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.*;
import fi.vm.sade.kayttooikeus.repositories.criteria.KayttooikeusCriteria;
import fi.vm.sade.kayttooikeus.repositories.criteria.OrganisaatioHenkiloCriteria;
import fi.vm.sade.kayttooikeus.repositories.dto.HenkilohakuResultDto;
import fi.vm.sade.kayttooikeus.service.*;
import fi.vm.sade.kayttooikeus.service.exception.DataInconsistencyException;
import fi.vm.sade.kayttooikeus.service.exception.ForbiddenException;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.exception.ValidationException;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.util.HenkilohakuBuilder;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloUpdateDto;
import fi.vm.sade.properties.OphProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

import static fi.vm.sade.kayttooikeus.model.Identification.CAS_AUTHENTICATION_IDP;

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
    private final KayttajatiedotRepository kayttajatiedotRepository;
    private final CommonProperties commonProperties;
    private final TunnistusTokenDataRepository tunnistusTokenDataRepository;
    private final OppijanumerorekisteriClient oppijanumerorekisteriClient;
    private final IdentificationService identificationService;
    private final OphProperties ophProperties;

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
    public void passivoi(String henkiloOid, String kasittelijaOid) {
        henkiloDataRepository.findByOidHenkilo(henkiloOid).ifPresent(henkilo -> passivoi(henkilo, kasittelijaOid));
    }

    private void passivoi(Henkilo henkilo, String kasittelijaOid) {
        henkilo.setKayttajatiedot(null);
        kayttajatiedotRepository.deleteByHenkilo(henkilo);

        if (StringUtils.isEmpty(kasittelijaOid)) {
            kasittelijaOid = getCurrentUserOid();
        }
        final String kasittelijaOidFinal = kasittelijaOid;
        List<OrganisaatioHenkilo> orgHenkilos = this.organisaatioHenkiloRepository.findByHenkilo(henkilo);
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

        henkilo.getHenkiloVarmennettavas().forEach(henkiloVarmentaja -> henkiloVarmentaja.setTila(false));

        ldapSynchronizationService.updateHenkiloAsap(henkilo.getOidHenkilo());
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
        Henkilo henkilo = this.henkiloDataRepository.findByOidHenkilo(oidHenkilo)
                .orElseThrow(() -> new NotFoundException("Henkilo not found with oid " + oidHenkilo));
        return this.isVahvastiTunnistettu(henkilo);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isVahvastiTunnistettuByUsername(String username) {
        Henkilo henkilo = this.henkiloDataRepository.findByKayttajatiedotUsername(username)
                .orElseThrow(() -> new NotFoundException("Henkilo not found with username " + username));
        return this.isVahvastiTunnistettu(henkilo);
    }

    private boolean isVahvastiTunnistettu(Henkilo henkilo) {
        return Boolean.TRUE.equals(henkilo.getVahvastiTunnistettu())
                || henkilo.getHenkiloVarmentajas().stream().anyMatch(HenkiloVarmentaja::isTila);
    }

    @Override
    @Transactional(readOnly = true)
    public LogInRedirectType logInRedirect(String username) {
        Henkilo henkilo = this.henkiloDataRepository.findByKayttajatiedotUsername(username)
                .orElseThrow(() -> new NotFoundException("Henkilo not found with username " + username));
        boolean isVahvastiTunnistettu = this.isVahvastiTunnistettu(henkilo);
        if(Boolean.FALSE.equals(isVahvastiTunnistettu)) {
            return LogInRedirectType.STRONG_IDENTIFICATION;
        }

        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        if(henkilo.getSahkopostivarmennusAikaleima() == null || henkilo.getSahkopostivarmennusAikaleima().isBefore(sixMonthsAgo)) {
            return LogInRedirectType.EMAIL_VERIFICATION;
        }

        return null;
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
        String currentUserOid = this.permissionCheckerService.getCurrentUserOid();
        OmatTiedotDto omatTiedotDto = this.mapper.map(this.kayttoOikeusService
                        .listMyonnettyKayttoOikeusForUser(KayttooikeusCriteria.builder()
                                        .oidHenkilo(currentUserOid)
                                        .build(),
                                null,
                                null).stream()
                        .findFirst()
                        .orElseGet(() -> KayttooikeusPerustiedotDto.builder()
                                .oidHenkilo(currentUserOid)
                                .organisaatiot(Sets.newHashSet())
                                .build()),
                OmatTiedotDto.class);
        omatTiedotDto.setIsAdmin(this.permissionCheckerService.isCurrentUserAdmin());
        omatTiedotDto.setIsMiniAdmin(this.permissionCheckerService.isCurrentUserMiniAdmin());
        omatTiedotDto.setAnomusilmoitus(false);

        Optional<Henkilo> currentUser = henkiloDataRepository.findByOidHenkilo(currentUserOid);
        currentUser.ifPresent(h -> omatTiedotDto.setAnomusilmoitus(h.getAnomusilmoitus()));

        return omatTiedotDto;
    }

    @Override
    @Transactional
    public void updateAnomusilmoitus(String oid, boolean anomusilmoitus) {
        if (!this.permissionCheckerService.getCurrentUserOid().equals(oid)) {
            throw new ForbiddenException("Henkilo can only update his own anomusilmoitus -setting");
        }
        Henkilo henkilo = henkiloDataRepository.findByOidHenkilo(oid).orElseThrow(
                () -> new NotFoundException(String.format("Henkilöä ei löytynyt OID:lla %s", oid)));
        henkilo.setAnomusilmoitus(anomusilmoitus);
    }

    @Override
    @Transactional(readOnly = true)
    public HenkiloLinkitysDto getLinkitykset(String oid, boolean showPassive) {
        return this.henkiloDataRepository.findLinkityksetByOid(oid, showPassive)
                .orElseThrow(() -> new NotFoundException("Henkilo not found with oid " + oid));
    }

    @Override
    @Transactional
    public String emailVerification(HenkiloUpdateDto henkiloUpdateDto, String loginToken) {
        TunnistusToken tunnistusToken = tunnistusTokenDataRepository.findByLoginToken(loginToken)
                .orElseThrow(() -> new NotFoundException(String.format("Login tokenia %s ei löytynyt", loginToken)));
        Henkilo henkilo = tunnistusToken.getHenkilo();

        if(!henkilo.getOidHenkilo().equals(henkiloUpdateDto.getOidHenkilo())) {
            throw new ValidationException(String.format("Login token %s doesn't match henkilo oid %s", loginToken, henkiloUpdateDto.getOidHenkilo()));
        }

        oppijanumerorekisteriClient.updateHenkilo(henkiloUpdateDto);
        henkilo.setSahkopostivarmennusAikaleima(LocalDateTime.now());

        String authToken = identificationService.consumeLoginToken(tunnistusToken.getLoginToken(), CAS_AUTHENTICATION_IDP);
        Map<String, Object> redirectMapping = this.redirectMapping(authToken, ophProperties.url("virkailijan-tyopoyta"));
        return ophProperties.url("cas.login", redirectMapping);

    }

    private Map<String, Object> redirectMapping(String authToken, String service) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("authToken", authToken);
        map.put("service", service);
        return map;
    }

    @Override
    @Transactional(readOnly = true)
    public HenkiloDto getHenkiloByLoginToken(String loginToken) {
        TunnistusToken tunnistusToken = tunnistusTokenDataRepository.findByLoginToken(loginToken)
                .orElseThrow(() -> new NotFoundException(String.format("Login tokenia %s ei löytynyt", loginToken)));
        String oid = tunnistusToken.getHenkilo().getOidHenkilo();
        return this.oppijanumerorekisteriClient.getHenkiloByOid(oid);
    }

}
