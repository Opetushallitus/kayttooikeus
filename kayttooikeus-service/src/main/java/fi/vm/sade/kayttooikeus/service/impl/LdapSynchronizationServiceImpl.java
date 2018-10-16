package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.config.properties.LdapSynchronizationProperties;
import fi.vm.sade.kayttooikeus.model.LdapPriorityType;
import fi.vm.sade.kayttooikeus.model.LdapStatusType;
import fi.vm.sade.kayttooikeus.model.LdapSynchronizationData;
import fi.vm.sade.kayttooikeus.model.LdapUpdateData;
import fi.vm.sade.kayttooikeus.repositories.LdapSynchronizationDataRepository;
import fi.vm.sade.kayttooikeus.repositories.LdapUpdateDataRepository;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService;
import fi.vm.sade.kayttooikeus.service.TimeService;
import fi.vm.sade.kayttooikeus.service.impl.ldap.LdapSynchronizer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class LdapSynchronizationServiceImpl implements LdapSynchronizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapSynchronizationServiceImpl.class);

    private final LdapUpdateDataRepository ldapUpdateDataRepository;
    private final TimeService timeService;
    private final LdapSynchronizer ldapSynchronizer;
    private final LdapSynchronizationDataRepository ldapSynchronizationDataRepository;
    private final LdapSynchronizationProperties ldapSynchronizationProperties;

    @Override
    @Transactional
    public void updateAllAtNight() {
        LOGGER.info("Lisätään kaikkien henkilöiden päivitys LDAP-synkronointijonoon");
        Iterable<LdapUpdateData> existingData = ldapUpdateDataRepository.findByHenkiloOid(LdapSynchronizer.RUN_ALL_BATCH);
        if (!existingData.iterator().hasNext()) {
            LdapUpdateData newData = new LdapUpdateData();
            newData.setHenkiloOid(LdapSynchronizer.RUN_ALL_BATCH);
            newData.setPriority(LdapPriorityType.NIGHT);
            newData.setStatus(LdapStatusType.IN_QUEUE);
            newData.setModified(timeService.getDateTimeNow());
            ldapUpdateDataRepository.save(newData);
        }
    }

    @Override
    @Transactional
    public void updateKayttoOikeusRyhma(Long kayttoOikeusRyhmaId) {
        LOGGER.info("Lisätään käyttöoikeusryhmä {} LDAP-synkronointijonoon", kayttoOikeusRyhmaId);
        if (ldapUpdateDataRepository.countByKayttoOikeusRyhmaId(kayttoOikeusRyhmaId).compareTo(0L) == 0) {
            LdapUpdateData newData = new LdapUpdateData();
            newData.setKayttoOikeusRyhmaId(kayttoOikeusRyhmaId);
            newData.setPriority(LdapPriorityType.BATCH);
            newData.setStatus(LdapStatusType.IN_QUEUE);
            newData.setModified(timeService.getDateTimeNow());
            ldapUpdateDataRepository.save(newData);
        }
    }

    @Override
    @Transactional
    public void updateHenkilo(String henkiloOid) {
        updateHenkilo(henkiloOid, LdapPriorityType.NORMAL);
    }

    @Override
    @Transactional
    public void updateHenkiloAsap(String henkiloOid) {
        updateHenkilo(henkiloOid, LdapPriorityType.ASAP);
    }

    private void updateHenkilo(String henkiloOid, LdapPriorityType priority) {
        LOGGER.info("Lisätään henkilö {} LDAP-synkronointijonoon prioriteetilla {}", henkiloOid, priority);

        // poistetaan henkilön epäonnistuneet ldap-synkronoinnit
        StreamSupport.stream(ldapUpdateDataRepository.findByHenkiloOid(henkiloOid).spliterator(), false)
                .filter(existingData -> LdapStatusType.FAILED.equals(existingData.getStatus()))
                .peek(existingData -> LOGGER.info("Poistetaan epäonnistunut {}", existingData))
                .forEach(ldapUpdateDataRepository::delete);

        // lisätään aina uusi rivi ldap-synkronointijonoon, koska tällä hetkellä
        // mahdollisesti käynnissä oleva synkronointi ei ota huomioon tässä
        // transaktiossa tulleita henkilön muutoksia
        LdapUpdateData newData = new LdapUpdateData();
        newData.setHenkiloOid(henkiloOid);
        newData.setPriority(priority);
        newData.setStatus(LdapStatusType.IN_QUEUE);
        newData.setModified(timeService.getDateTimeNow());
        ldapUpdateDataRepository.save(newData);
    }

    @Override
    @Transactional
    public void updateHenkiloNow(String henkiloOid) {
        LOGGER.info("LDAP-synkronointi henkilölle {} aloitetaan", henkiloOid);
        long start = timeService.getCurrentTimeMillis();

        ldapSynchronizer.synchronize(henkiloOid);

        LOGGER.info("LDAP-synkronointi henkilölle {} päättyy, kesto: {}ms", henkiloOid, timeService.getCurrentTimeMillis() - start);
    }

    @Override
    @Transactional
    public synchronized void runSynchronizer() {
        LOGGER.info("LDAP-synkronointi aloitetaan");
        long start = timeService.getCurrentTimeMillis();

        LocalDateTime now = timeService.getDateTimeNow();
        boolean nightTime = ldapSynchronizationProperties.isNightTime(now.getHour());
        LdapSynchronizationProperties.Timed properties = ldapSynchronizationProperties.getTimedProperties(nightTime);
        LocalDateTime dateTime = now.minusMinutes(properties.getIntervalInMinutes());

        Optional<LdapSynchronizationData> previous = ldapSynchronizationDataRepository.findFirstByOrderByIdDesc();
        if (previous.map(t -> t.getLastRun().isBefore(dateTime)).orElse(true)) {
            Optional<LdapSynchronizationData> next = ldapSynchronizer.run(previous, nightTime,
                    properties.getBatchSize(), properties.getLoadThresholdInSeconds());
            next.ifPresent(this::saveStatistics);
        }

        LOGGER.info("LDAP-synkronointi päättyy, kesto: {}ms", timeService.getCurrentTimeMillis() - start);
    }

    private void saveStatistics(LdapSynchronizationData next) {
        List<LdapSynchronizationData> datas = ldapSynchronizationDataRepository.findByOrderByIdAsc();
        if (datas.size() > ldapSynchronizationProperties.getStatisticsSize()) {
            LdapSynchronizationData oldest = datas.get(0);
            ldapSynchronizationDataRepository.delete(oldest);
        }
        ldapSynchronizationDataRepository.save(next);
    }

}
