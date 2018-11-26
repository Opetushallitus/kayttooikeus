package fi.vm.sade.kayttooikeus.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.dto.types.AnomusTyyppi;
import fi.vm.sade.kayttooikeus.enumeration.OrderByAnomus;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.*;
import fi.vm.sade.kayttooikeus.repositories.criteria.AnomusCriteria;
import fi.vm.sade.kayttooikeus.repositories.criteria.AnomusCriteria.Myontooikeus;
import fi.vm.sade.kayttooikeus.repositories.criteria.MyontooikeusCriteria;
import fi.vm.sade.kayttooikeus.service.*;
import fi.vm.sade.kayttooikeus.service.exception.ForbiddenException;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.exception.UnprocessableEntityException;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.service.impl.anomus.MyontooikeusMapper;
import fi.vm.sade.kayttooikeus.service.validators.HaettuKayttooikeusryhmaValidator;
import fi.vm.sade.kayttooikeus.util.OrganisaatioMyontoPredicate;
import fi.vm.sade.kayttooikeus.util.UserDetailsUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

import javax.validation.ValidationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.PALVELU_ANOMUSTENHALLINTA;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.PALVELU_KAYTTOOIKEUS;
import static fi.vm.sade.kayttooikeus.util.FunctionalUtils.appending;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

@Service
@RequiredArgsConstructor
public class KayttooikeusAnomusServiceImpl extends AbstractService implements KayttooikeusAnomusService {

    private final HaettuKayttooikeusRyhmaRepository haettuKayttooikeusRyhmaRepository;
    private final HenkiloDataRepository henkiloDataRepository;
    private final HenkiloHibernateRepository henkiloHibernateRepository;
    private final MyonnettyKayttoOikeusRyhmaTapahtumaRepository myonnettyKayttoOikeusRyhmaTapahtumaRepository;
    private final KayttoOikeusRyhmaMyontoViiteRepository kayttoOikeusRyhmaMyontoViiteRepository;
    private final KayttoOikeusRyhmaTapahtumaHistoriaDataRepository kayttoOikeusRyhmaTapahtumaHistoriaDataRepository;
    private final KayttooikeusryhmaDataRepository kayttooikeusryhmaDataRepository;
    private final AnomusRepository anomusRepository;
    private final OrganisaatioHenkiloRepository organisaatioHenkiloRepository;

    private final OrikaBeanMapper mapper;
    private final LocalizationService localizationService;
    private final EmailService emailService;
    private final LdapSynchronizationService ldapSynchronizationService;
    private final OrganisaatioService organisaatioService;

    private final HaettuKayttooikeusryhmaValidator haettuKayttooikeusryhmaValidator;
    private final PermissionCheckerService permissionCheckerService;

    private final CommonProperties commonProperties;

    private final OrganisaatioClient organisaatioClient;

    @Override
    @Transactional(readOnly = true)
    public List<HaettuKayttooikeusryhmaDto> listHaetutKayttoOikeusRyhmat(String oidHenkilo, boolean activeOnly) {
        AnomusCriteria criteria = new AnomusCriteria();
        criteria.setAnojaOid(oidHenkilo);
        if(activeOnly) {
            criteria.setAnomuksenTilat(EnumSet.of(AnomuksenTila.ANOTTU));
            criteria.setKayttoOikeudenTilas(EnumSet.of(KayttoOikeudenTila.ANOTTU));
        }
        List<HaettuKayttoOikeusRyhma> haettuKayttoOikeusRyhmas = this.haettuKayttooikeusRyhmaRepository
                .findBy(criteria.createAnomusSearchCondition(this.organisaatioClient));
        return localizeKayttooikeusryhma(mapper.mapAsList(haettuKayttoOikeusRyhmas, HaettuKayttooikeusryhmaDto.class));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HaettuKayttooikeusryhmaDto> listHaetutKayttoOikeusRyhmat(AnomusCriteria criteria,
                                                                         Long limit,
                                                                         Long offset,
                                                                         OrderByAnomus orderBy) {
        String kayttajaOid = permissionCheckerService.getCurrentUserOid();

        // Do not show own anomus
        criteria.addHenkiloOidRestriction(kayttajaOid);

        if (!this.permissionCheckerService.isCurrentUserAdmin()) {
            // organisaatio & myöntöviite suodatus
            MyontooikeusCriteria myontooikeusCriteria = MyontooikeusCriteria.oletus();
            Map<String, Set<Long>> myontooikeusByOrganisaatio = kayttoOikeusRyhmaMyontoViiteRepository.getSlaveIdsByMasterHenkiloOid(kayttajaOid, myontooikeusCriteria);
            if (logger.isDebugEnabled()) {
                logger.debug("Käyttäjän {} käyttöoikeuksien myöntöoikeudet {}", kayttajaOid, myontooikeusByOrganisaatio);
            }
            List<Myontooikeus> myontooikeudet = getMyontooikeudet(myontooikeusByOrganisaatio, criteria);
            if (myontooikeudet.isEmpty()) {
                logger.info("Käyttäjällä {} ei ole yhtään käyttöoikeuden myöntöoikeutta hakukriteereillä {} (kaikki myöntöoikeudet: {})", kayttajaOid, criteria, myontooikeudet);
                return emptyList();
            }
            criteria.setMyontooikeudet(myontooikeudet);
            criteria.setOrganisaatioOids(null); // myöntöoikeudet suodattaa organisaatioiden mukaan
            criteria.setKayttooikeusRyhmaIds(null); // myöntöoikeudet suodattaa käyttöoikeusryhmien mukaan
        }

        List<HaettuKayttoOikeusRyhma> haettuKayttoOikeusRyhmas = this.haettuKayttooikeusRyhmaRepository
                .findBy(criteria.createAnomusSearchCondition(this.organisaatioClient), limit, offset, orderBy);
        return localizeKayttooikeusryhma(mapper.mapAsList(haettuKayttoOikeusRyhmas, HaettuKayttooikeusryhmaDto.class));
    }

    private List<Myontooikeus> getMyontooikeudet(Map<String, Set<Long>> myontooikeusByOrganisaatio, AnomusCriteria criteria) {
        MyontooikeusMapper myontooikeusMapper = new MyontooikeusMapper(commonProperties, organisaatioClient, criteria);
        return myontooikeusByOrganisaatio.entrySet().stream()
                .map(myontooikeusMapper)
                .filter(Myontooikeus::isNotEmpty)
                .collect(toList());
    }

    private List<HaettuKayttooikeusryhmaDto> localizeKayttooikeusryhma(List<HaettuKayttooikeusryhmaDto> unlocalizedDtos) {
        unlocalizedDtos
                .forEach(haettuKayttooikeusryhmaDto -> haettuKayttooikeusryhmaDto
                        .setKayttoOikeusRyhma(localizationService.localize(haettuKayttooikeusryhmaDto.getKayttoOikeusRyhma())));
        return unlocalizedDtos;
    }

    @Transactional
    @Override
    public void updateHaettuKayttooikeusryhma(UpdateHaettuKayttooikeusryhmaDto updateHaettuKayttooikeusryhmaDto) {
        HaettuKayttoOikeusRyhma haettuKayttoOikeusRyhma = this.haettuKayttooikeusRyhmaRepository
                .findById(updateHaettuKayttooikeusryhmaDto.getId())
                .orElseThrow(() -> new NotFoundException("Haettua kayttooikeusryhmaa ei löytynyt id:llä "
                        + updateHaettuKayttooikeusryhmaDto.getId()));
        Anomus anojanAnomus = haettuKayttoOikeusRyhma.getAnomus();
        KayttoOikeusRyhma anottuKayttoOikeusRyhma = haettuKayttoOikeusRyhma.getKayttoOikeusRyhma();
        Long kayttooikeusryhmaId = anottuKayttoOikeusRyhma.getId();

        // Permission checks for declining requisition (there are separate checks for granting)
        this.notEditingOwnData(anojanAnomus.getHenkilo().getOidHenkilo());
        this.inSameOrParentOrganisation(anojanAnomus.getOrganisaatioOid());
        this.organisaatioViiteLimitationsAreValidThrows(anottuKayttoOikeusRyhma.getId());
        this.kayttooikeusryhmaLimitationsAreValid(anottuKayttoOikeusRyhma.getId());

        // Post validation
        BindException errors = new BindException(haettuKayttoOikeusRyhma, "haettuKayttoOikeusRyhma");
        this.haettuKayttooikeusryhmaValidator.validate(haettuKayttoOikeusRyhma, errors);
        if (errors.hasErrors()) {
            throw new UnprocessableEntityException(errors);
        }

        Henkilo kasittelija = this.henkiloDataRepository.findByOidHenkilo(this.permissionCheckerService.getCurrentUserOid())
                .orElseThrow(() -> new NotFoundException("Kasittelija not found with oid " + this.permissionCheckerService.getCurrentUserOid()));
        anojanAnomus.setKasittelija(kasittelija);

        Henkilo anoja = this.henkiloDataRepository.findByOidHenkilo(anojanAnomus.getHenkilo().getOidHenkilo())
                .orElseThrow(() -> new NotFoundException("Anoja not found with oid "
                        + anojanAnomus.getHenkilo().getOidHenkilo()));


        this.updateAnomusAndRemoveHaettuKayttooikeusRyhma(haettuKayttoOikeusRyhma,
                KayttoOikeudenTila.valueOf(updateHaettuKayttooikeusryhmaDto.getKayttoOikeudenTila()));

        // Event is created only when kayttooikeus has been granted.
        if (KayttoOikeudenTila.valueOf(updateHaettuKayttooikeusryhmaDto.getKayttoOikeudenTila()) == KayttoOikeudenTila.MYONNETTY) {
            MyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma = this.grantKayttooikeusryhma(
                    updateHaettuKayttooikeusryhmaDto.getAlkupvm(),
                    updateHaettuKayttooikeusryhmaDto.getLoppupvm(),
                    anoja.getOidHenkilo(),
                    anojanAnomus.getOrganisaatioOid(),
                    anottuKayttoOikeusRyhma,
                    anojanAnomus.getTehtavanimike(),
                    this.permissionCheckerService.getCurrentUserOid());
            anojanAnomus.addMyonnettyKayttooikeusRyhma(myonnettyKayttoOikeusRyhmaTapahtuma);
        }

        this.emailService.sendEmailAnomusKasitelty(anojanAnomus, updateHaettuKayttooikeusryhmaDto, kayttooikeusryhmaId);

    }

    private void notEditingOwnData(String oidHenkilo) {
        // Cant edit own data
        if (!this.permissionCheckerService.notOwnData(oidHenkilo)) {
            throw new ForbiddenException("User can't edit his own data.");
        }
    }

    private void inSameOrParentOrganisation(String organisaatioOid) {
        // User has to be in the same or one of the parent organisations
        Map<String, List<String>> allowedRoles = new LinkedHashMap<>();
        allowedRoles.put(PALVELU_ANOMUSTENHALLINTA, asList("READ_UPDATE", "CRUD"));
        allowedRoles.put(PALVELU_KAYTTOOIKEUS, asList("CRUD"));
        if (!this.permissionCheckerService.checkRoleForOrganisation(
                Lists.newArrayList(organisaatioOid),
                allowedRoles)) {
            throw new ForbiddenException("No access through organisation hierarchy.");
        }
    }

    private void organisaatioViiteLimitationsAreValidThrows(Long kayttooikeusryhmaId) {
        if (!this.permissionCheckerService.organisaatioViiteLimitationsAreValid(kayttooikeusryhmaId)) {
            throw new ForbiddenException("Target organization has invalid organization type");
        }
    }

    private void kayttooikeusryhmaLimitationsAreValid(Long kayttooikeusryhmaId) {
        // The granting person's limitations must be checked always since there there shouldn't be a situation where the
        // the granting person doesn't have access rights limitations (except admin users who have full access)
        if (!this.permissionCheckerService.kayttooikeusMyontoviiteLimitationCheck(kayttooikeusryhmaId)) {
            throw new ForbiddenException("User doesn't have access rights to grant this group");
        }
    }

    private void updateAnomusAndRemoveHaettuKayttooikeusRyhma(HaettuKayttoOikeusRyhma haettuKayttoOikeusRyhma,
                                                              KayttoOikeudenTila newKayttooikeudenTila) {
        if (haettuKayttoOikeusRyhma.getAnomus().getHaettuKayttoOikeusRyhmas().size() == 1) {
            // This is the last kayttooikeus on anomus so we mark anomus as KASITELTY or HYLATTY
            haettuKayttoOikeusRyhma.getAnomus().setAnomuksenTila(
                    newKayttooikeudenTila == KayttoOikeudenTila.MYONNETTY || newKayttooikeudenTila == KayttoOikeudenTila.UUSITTU
                            ? AnomuksenTila.KASITELTY
                            : AnomuksenTila.HYLATTY);
        }
        haettuKayttoOikeusRyhma.getAnomus().setAnomusTilaTapahtumaPvm(LocalDateTime.now());

        // Remove the currently handled kayttooikeus from anomus
        haettuKayttoOikeusRyhma.getAnomus().getHaettuKayttoOikeusRyhmas().remove(haettuKayttoOikeusRyhma);
        this.haettuKayttooikeusRyhmaRepository.delete(haettuKayttoOikeusRyhma);
    }

    // Sets organisaatiohenkilo active since it might be passive
    private OrganisaatioHenkilo findOrCreateHaettuOrganisaatioHenkilo(String organisaatioOid, Henkilo anoja, String tehtavanimike) {
        Henkilo savedAnoja = this.henkiloDataRepository.save(anoja);

        organisaatioClient.getOrganisaatioPerustiedotCached(organisaatioOid)
                .filter(new OrganisaatioMyontoPredicate())
                .orElseThrow(() -> new ValidationException("Active or suunniteltu organisation not found with oid " + organisaatioOid));

        OrganisaatioHenkilo foundOrCreatedOrganisaatioHenkilo = organisaatioHenkiloRepository.findByHenkilo(savedAnoja).stream()
                .filter(organisaatioHenkilo ->
                        Objects.equals(organisaatioHenkilo.getOrganisaatioOid(), organisaatioOid))
                .findFirst().orElseGet(() ->
                        this.organisaatioHenkiloRepository.save(OrganisaatioHenkilo.builder()
                                .organisaatioOid(organisaatioOid)
                                .tehtavanimike(tehtavanimike)
                                .henkilo(savedAnoja)
                                .build()));
        foundOrCreatedOrganisaatioHenkilo.setPassivoitu(false);
        return foundOrCreatedOrganisaatioHenkilo;
    }

    @Override
    @Transactional
    public void grantKayttooikeusryhma(String anojaOid,
                                       String organisaatioOid,
                                       List<GrantKayttooikeusryhmaDto> updateHaettuKayttooikeusryhmaDtoList) {
        // Permission checks
        this.notEditingOwnData(anojaOid);
        this.inSameOrParentOrganisation(organisaatioOid);
        updateHaettuKayttooikeusryhmaDtoList.forEach(updateHaettuKayttooikeusryhmaDto -> {
            this.organisaatioViiteLimitationsAreValidThrows(updateHaettuKayttooikeusryhmaDto.getId());
            this.kayttooikeusryhmaLimitationsAreValid(updateHaettuKayttooikeusryhmaDto.getId());
        });

        updateHaettuKayttooikeusryhmaDtoList.forEach(haettuKayttooikeusryhmaDto ->
                this.grantKayttooikeusryhma(haettuKayttooikeusryhmaDto.getAlkupvm(),
                        haettuKayttooikeusryhmaDto.getLoppupvm(),
                        anojaOid,
                        organisaatioOid,
                        this.kayttooikeusryhmaDataRepository.findById(haettuKayttooikeusryhmaDto.getId())
                                .orElseThrow(() -> new NotFoundException("Kayttooikeusryhma not found with id " + haettuKayttooikeusryhmaDto.getId())),
                        "",
                        this.permissionCheckerService.getCurrentUserOid()));
    }

    // For internal calls when permission checks are redundant.
    @Override
    @Transactional
    public void grantKayttooikeusryhmaAsAdminWithoutPermissionCheck(String anoja,
                                                                    String organisaatioOid,
                                                                    Collection<KayttoOikeusRyhma> kayttooikeusryhmas) {
        this.grantKayttooikeusryhmaAsAdminWithoutPermissionCheck(anoja,
                organisaatioOid,
                LocalDate.now().plusYears(1),
                kayttooikeusryhmas,
                this.permissionCheckerService.getCurrentUserOid());
    }

    @Override
    public void grantKayttooikeusryhmaAsAdminWithoutPermissionCheck(String anoja, String organisaatioOid, LocalDate voimassaLoppuPvm, Collection<KayttoOikeusRyhma> kayttooikeusryhmas, String myontajaOid) {
        kayttooikeusryhmas.forEach(kayttooikeusryhma -> this.grantKayttooikeusryhma(
                LocalDate.now(),
                voimassaLoppuPvm,
                anoja,
                organisaatioOid,
                kayttooikeusryhma,
                "",
                myontajaOid)); // anoja == kasittelija in this case
    }

    @Override
    @Transactional
    public Long createKayttooikeusAnomus(String anojaOid, KayttooikeusAnomusDto kayttooikeusAnomusDto) {
        Henkilo anoja = this.henkiloDataRepository.findByOidHenkilo(anojaOid)
                .orElseThrow(() -> new NotFoundException("Anoja not found with oid " + anojaOid));

        Anomus anomus = new Anomus();
        anomus.setHenkilo(anoja);
        anomus.setAnomuksenTila(AnomuksenTila.ANOTTU);
        anomus.setSahkopostiosoite(kayttooikeusAnomusDto.getEmail());
        anomus.setPerustelut(kayttooikeusAnomusDto.getPerustelut());
        anomus.setAnomusTyyppi(AnomusTyyppi.UUSI);
        anomus.setAnomusTilaTapahtumaPvm(LocalDateTime.now());
        anomus.setAnottuPvm(LocalDateTime.now());
        anomus.setOrganisaatioOid(kayttooikeusAnomusDto.getOrganisaatioOrRyhmaOid());


        Iterable<KayttoOikeusRyhma> kayttoOikeusRyhmas = this.kayttooikeusryhmaDataRepository
                .findAllById(kayttooikeusAnomusDto.getKayttooikeusRyhmaIds());

        kayttoOikeusRyhmas.forEach(k -> {
            HaettuKayttoOikeusRyhma h = new HaettuKayttoOikeusRyhma();
            h.setKayttoOikeusRyhma(k);
            anomus.addHaettuKayttoOikeusRyhma(h);
        });
        return this.anomusRepository.save(anomus).getId();
    }

    @Override
    @Transactional
    public void lahetaUusienAnomuksienIlmoitukset(LocalDate anottuPvm) {
        LocalDateTime alkuPvm = anottuPvm.atStartOfDay();
        LocalDateTime loppuPvm = anottuPvm.atTime(LocalTime.MIDNIGHT.minusSeconds(1));
        AnomusCriteria criteria = AnomusCriteria.builder()
                .anottuAlku(alkuPvm)
                .anottuLoppu(loppuPvm)
                .anomuksenTilat(EnumSet.of(AnomuksenTila.ANOTTU))
                .build();
        List<Anomus> anomukset = anomusRepository.findBy(criteria.createEmailSendCondition(this.organisaatioClient));

        Set<String> hyvaksyjat = anomukset.stream()
                .map(this::getAnomuksenHyvaksyjat)
                .flatMap(Collection::stream)
                .collect(toSet());

        List<Anomus> ophAdminAnomukset = this.anomusRepository.getOphAdminAnomukset().stream().
                filter(a -> a.getAnottuPvm() != null && a.getAnottuPvm().isAfter(alkuPvm) && a.getAnottuPvm().isBefore(loppuPvm))
            .collect(Collectors.toList());
        if(ophAdminAnomukset.size() > 0) {
            Set<String> ophAdminAnomusVastaanottajat = this.henkiloDataRepository.findByAnomusilmoitusIsTrue()
                    .stream().map(h -> h.getOidHenkilo()).collect(toSet());
            hyvaksyjat.addAll(ophAdminAnomusVastaanottajat);
        }

        emailService.sendNewRequisitionNotificationEmails(hyvaksyjat);
    }

    private Set<String> getAnomuksenHyvaksyjat(Anomus anomus) {
        Set<Long> kayttoOikeusRyhmaIds = getHyvaksyjaKayttoOikeusRyhmat(anomus);
        if (kayttoOikeusRyhmaIds.isEmpty()) {
            logger.info("Ei löytynyt käyttöoikeusryhmiä, jotka voisivat hyväksyä anomuksen {}", anomus.getId());
            return emptySet();
        }
        Set<String> organisaatioOids = getHyvaksyjaOrganisaatiot(anomus);
        if (organisaatioOids.isEmpty()) {
            logger.info("Ei löytynyt organisaatioita, jotka voisivat hyväksyä anomuksen {}", anomus.getId());
            return emptySet();
        }
        Set<String> henkiloOids = henkiloHibernateRepository.findByKayttoOikeusRyhmatAndOrganisaatiot(kayttoOikeusRyhmaIds, organisaatioOids)
                .stream()
                .map(Henkilo::getOidHenkilo)
                // Henkilö ei saa hyväksyä omaa käyttöoikeusanomusta
                .filter(t -> !t.equals(anomus.getHenkilo().getOidHenkilo()))
                .collect(toSet());
        if (henkiloOids.isEmpty()) {
            logger.info("Anomuksella {} ei ole hyväksyjiä", anomus.getId());
        }
        return henkiloOids;
    }

    private Set<Long> getHyvaksyjaKayttoOikeusRyhmat(Anomus anomus) {
        Set<Long> slaveIds = anomus.getHaettuKayttoOikeusRyhmas().stream()
                .map(t -> t.getKayttoOikeusRyhma().getId())
                .collect(toSet());
        return kayttoOikeusRyhmaMyontoViiteRepository.getMasterIdsBySlaveIds(slaveIds);
    }

    private Set<String> getHyvaksyjaOrganisaatiot(Anomus anomus) {
        if (commonProperties.getRootOrganizationOid().equals(anomus.getOrganisaatioOid())) {
            return singleton(commonProperties.getRootOrganizationOid());
        }
        return organisaatioClient.getParentOids(anomus.getOrganisaatioOid()).stream()
                // Ei lähetetä root-organisaation henkilöille jokaisesta anomuksesta ilmoitusta
                .filter(parentOid -> !commonProperties.getRootOrganizationOid().equals(parentOid))
                .collect(toSet());
    }

    // Grant kayttooikeusryhma and create event. DOES NOT CONTAIN PERMISSION CHECKS SO DONT CALL DIRECTLY
    private MyonnettyKayttoOikeusRyhmaTapahtuma grantKayttooikeusryhma(LocalDate alkupvm,
                                                                       LocalDate loppupvm,
                                                                       String anojaOid,
                                                                       String organisaatioOid,
                                                                       KayttoOikeusRyhma myonnettavaKayttoOikeusRyhma,
                                                                       String tehtavanimike,
                                                                       String kasittelijaOid) {
        Henkilo anoja = this.henkiloDataRepository.findByOidHenkilo(anojaOid)
                .orElseThrow(() -> new NotFoundException("Anoja not found with oid " + anojaOid));
        Henkilo kasittelija = this.henkiloDataRepository.findByOidHenkilo(kasittelijaOid)
                .orElseThrow(() -> new NotFoundException("Kasittelija not found with oid " + this.getCurrentUserOid()));

        OrganisaatioHenkilo myonnettavaOrganisaatioHenkilo = this.findOrCreateHaettuOrganisaatioHenkilo(
                organisaatioOid, anoja, tehtavanimike);

        MyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma =
                this.findOrCreateMyonnettyKayttooikeusryhmaTapahtuma(anojaOid, myonnettavaOrganisaatioHenkilo,
                        myonnettavaKayttoOikeusRyhma);

        myonnettyKayttoOikeusRyhmaTapahtuma.setVoimassaAlkuPvm(alkupvm);
        myonnettyKayttoOikeusRyhmaTapahtuma.setVoimassaLoppuPvm(loppupvm);
        myonnettyKayttoOikeusRyhmaTapahtuma.setAikaleima(LocalDateTime.now());
        myonnettyKayttoOikeusRyhmaTapahtuma.setKasittelija(kasittelija);
        myonnettyKayttoOikeusRyhmaTapahtuma.setTila(myonnettyKayttoOikeusRyhmaTapahtuma.getId() == null
                ? KayttoOikeudenTila.MYONNETTY
                : KayttoOikeudenTila.UUSITTU);

        this.createGrantedHistoryEvent(myonnettyKayttoOikeusRyhmaTapahtuma,
                myonnettyKayttoOikeusRyhmaTapahtuma.getId() == null
                        ? "Oikeuksien lisäys"
                        : "Oikeuksien päivitys");

        ldapSynchronizationService.updateHenkiloAsap(anojaOid);

        return myonnettyKayttoOikeusRyhmaTapahtuma;
    }

    // New history event for a change on kayttooikeusryhmatapahtuma.
    private void createGrantedHistoryEvent(MyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma, String reason) {
        this.kayttoOikeusRyhmaTapahtumaHistoriaDataRepository.save(myonnettyKayttoOikeusRyhmaTapahtuma.toHistoria(LocalDateTime.now(), reason));
    }

    private MyonnettyKayttoOikeusRyhmaTapahtuma findOrCreateMyonnettyKayttooikeusryhmaTapahtuma(String oidHenkilo,
                                                                                                OrganisaatioHenkilo organisaatioHenkilo,
                                                                                                KayttoOikeusRyhma kayttoOikeusRyhma) {
        MyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma =  this.myonnettyKayttoOikeusRyhmaTapahtumaRepository
                .findMyonnettyTapahtuma(kayttoOikeusRyhma.getId(),
                        organisaatioHenkilo.getOrganisaatioOid(), oidHenkilo)
                .orElseGet(() -> this.myonnettyKayttoOikeusRyhmaTapahtumaRepository.save(MyonnettyKayttoOikeusRyhmaTapahtuma.builder()
                        .kayttoOikeusRyhma(kayttoOikeusRyhma)
                        .organisaatioHenkilo(organisaatioHenkilo)
                        .anomus(Sets.newHashSet())
                        .aikaleima(LocalDateTime.now())
                        .tila(KayttoOikeudenTila.ANOTTU)
                        .voimassaAlkuPvm(LocalDate.now()).build()));
        organisaatioHenkilo.addMyonnettyKayttooikeusryhmaTapahtuma(myonnettyKayttoOikeusRyhmaTapahtuma);
        return myonnettyKayttoOikeusRyhmaTapahtuma;
    }

    @Override
    @Transactional
    public void cancelKayttooikeusAnomus(Long kayttooikeusRyhmaId) {
        HaettuKayttoOikeusRyhma haettuKayttoOikeusRyhma = this.haettuKayttooikeusRyhmaRepository.findById(kayttooikeusRyhmaId)
                .orElseThrow(() -> new NotFoundException("HaettuKayttooikeusRyhma not found with id " + kayttooikeusRyhmaId));
        Anomus anomus = haettuKayttoOikeusRyhma.getAnomus();
        String kayttajaOid = getCurrentUserOid();
        if (!kayttajaOid.equals(anomus.getHenkilo().getOidHenkilo())) {
            throw new ForbiddenException(String.format("Käyttäjällä '%s' ei ole oikeuksia perua haettua käyttöoikeusryhmää '%s'", kayttajaOid, haettuKayttoOikeusRyhma.getId()));
        }
        anomus.getHaettuKayttoOikeusRyhmas().removeIf( h -> h.getId().equals(haettuKayttoOikeusRyhma.getId()) );
        if (anomus.getHaettuKayttoOikeusRyhmas().isEmpty()) {
            anomus.setAnomuksenTila(AnomuksenTila.PERUTTU);
        }
        this.haettuKayttooikeusRyhmaRepository.deleteById(kayttooikeusRyhmaId);
    }

    @Override
    @Transactional
    public void removePrivilege(String oidHenkilo, Long kayttooikeusryhmaId, String organisaatioOid) {
        // Permission checks
        this.notEditingOwnData(oidHenkilo);
        this.inSameOrParentOrganisation(organisaatioOid);
        this.organisaatioViiteLimitationsAreValidThrows(kayttooikeusryhmaId);
        this.kayttooikeusryhmaLimitationsAreValid(kayttooikeusryhmaId);

        Henkilo kasittelija = this.henkiloDataRepository.findByOidHenkilo(UserDetailsUtil.getCurrentUserOid())
                .orElseThrow(() -> new NotFoundException("Current user not found with oid " + UserDetailsUtil.getCurrentUserOid()));

        MyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma =
                this.myonnettyKayttoOikeusRyhmaTapahtumaRepository.findMyonnettyTapahtuma(kayttooikeusryhmaId, organisaatioOid, oidHenkilo)
                        .orElseThrow(() -> new NotFoundException("Myonnettykayttooikeusryhma not found with KayttooikeusryhmaId "
                                + kayttooikeusryhmaId + " organisaatioOid " + organisaatioOid + " oidHenkilo " + oidHenkilo));
        this.kayttoOikeusRyhmaTapahtumaHistoriaDataRepository.save(myonnettyKayttoOikeusRyhmaTapahtuma
                .toHistoria(kasittelija, KayttoOikeudenTila.SULJETTU, LocalDateTime.now(), "Käyttöoikeuden sulkeminen"));
        this.myonnettyKayttoOikeusRyhmaTapahtumaRepository.delete(myonnettyKayttoOikeusRyhmaTapahtuma);

        ldapSynchronizationService.updateHenkiloAsap(oidHenkilo);
    }



    // Käsittelee admin, OPH-virkailija ja virkailija tyyppisiä käyttäjiä. Kaksi ensimmäistä käyttäytyvät tässä samoin.
    // Olettaa, että ennestään olleet ja haetut oikeudet voidaan myöntää uudelleen kyseiseen organisaatioon.
    @Override
    @Transactional(readOnly = true)
    public Map<String, Set<Long>> findCurrentHenkiloCanGrant(String accessedHenkiloOid) {
        final Map<String, Set<Long>> kayttooikeusByOrganisation = new HashMap<>();

        // Haetun henkilön anumukset, voimassa olevat käyttöoikeudet ja joskus voimassa olleet oikeudet
        this.anomusRepository.findByHenkiloOidHenkilo(accessedHenkiloOid)
                .forEach(anomus -> kayttooikeusByOrganisation.merge(anomus.getOrganisaatioOid(),
                        anomus.getHaettuKayttoOikeusRyhmas().stream()
                                .map(HaettuKayttoOikeusRyhma::getKayttoOikeusRyhma)
                                .map(KayttoOikeusRyhma::getId)
                                .collect(toSet()),
                        appending()));
        this.myonnettyKayttoOikeusRyhmaTapahtumaRepository.findByOrganisaatioHenkiloHenkiloOidHenkilo(accessedHenkiloOid)
                .forEach(mkrt -> kayttooikeusByOrganisation.compute(mkrt.getOrganisaatioHenkilo().getOrganisaatioOid(),
                        this.addIfNotExists(mkrt.getKayttoOikeusRyhma().getId())));
        this.kayttoOikeusRyhmaTapahtumaHistoriaDataRepository
                .findByOrganisaatioHenkiloHenkiloOidHenkiloAndTila(accessedHenkiloOid, KayttoOikeudenTila.SULJETTU)
                .forEach(krth -> kayttooikeusByOrganisation.compute(krth.getOrganisaatioHenkilo().getOrganisaatioOid(),
                        this.addIfNotExists(krth.getKayttoOikeusRyhma().getId())));

        String kayttajaOid = UserDetailsUtil.getCurrentUserOid();
        MyontooikeusCriteria myontooikeusCriteria = MyontooikeusCriteria.oletus();
        Map<String, Set<Long>> myontooikeudet = kayttoOikeusRyhmaMyontoViiteRepository.getSlaveIdsByMasterHenkiloOid(kayttajaOid, myontooikeusCriteria);

        // vain rekisterinpitäjä saa myöntää kaikkia käyttöoikeuksia
        if (!permissionCheckerService.isCurrentUserAdmin()) {
            // lisätään myöntöoikeudet aliorganisaatioihin
            myontooikeudet.entrySet().stream()
                    .flatMap(entry -> organisaatioClient.listWithChildOids(entry.getKey(), new OrganisaatioMyontoPredicate()).stream()
                            .map(aliorganisaatioOid -> new SimpleEntry<>(aliorganisaatioOid, entry.getValue())))
                    .collect(toMap(Entry::getKey, Entry::getValue, appending()))
                    .forEach((organisaatioOid, kayttooikeusryhmaIds) -> myontooikeudet.merge(organisaatioOid, kayttooikeusryhmaIds, appending()));

            this.regularUserChecks(kayttooikeusByOrganisation, myontooikeudet);
        }

        // Filter off empty keys
        return kayttooikeusByOrganisation.entrySet().stream()
                .filter(entity -> !entity.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void regularUserChecks(Map<String, Set<Long>> kayttooikeusByOrganisation, Map<String, Set<Long>> myontooikeudet) {
        // Myöntöviite (saako käyttäjä myöntää näitä käyttöoikeusryhmiä)
        kayttooikeusByOrganisation.entrySet().removeIf(entry -> {
            Set<Long> kayttooikeusryhmaIds = myontooikeudet.get(entry.getKey());
            if (kayttooikeusryhmaIds == null) {
                // käyttäjällä ei ole ollenkaan oikeuksia organisaatioon
                return true;
            }
            // käyttäjällä on jotain oikeuksia organisaatioon
            entry.getValue().retainAll(kayttooikeusryhmaIds);
            return false;
        });

        // Organisaatioviite (pystyykö tästä KOR myöntämään tähän organisaatioon)
        kayttooikeusByOrganisation.replaceAll((organisaatioOid, kayttooikeusryhmaIdList) ->
                kayttooikeusryhmaIdList.stream()
                        .filter(this.permissionCheckerService::organisaatioViiteLimitationsAreValid)
                        .collect(Collectors.toSet()));
    }

    private BiFunction<String, Set<Long>, Set<Long>> addIfNotExists(Long ryhmaId) {
        return (key, value) -> {
            if (value == null) {
                value = new HashSet<>();
            }
            if (!value.contains(ryhmaId)) {
                value.add(ryhmaId);
            }
            return value;
        };
    }

}
