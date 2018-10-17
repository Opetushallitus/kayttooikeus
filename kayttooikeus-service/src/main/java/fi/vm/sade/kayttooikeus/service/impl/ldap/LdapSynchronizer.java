package fi.vm.sade.kayttooikeus.service.impl.ldap;

import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.repositories.HenkiloHibernateRepository;
import fi.vm.sade.kayttooikeus.repositories.LdapUpdateDataCriteria;
import fi.vm.sade.kayttooikeus.repositories.LdapUpdateDataRepository;
import fi.vm.sade.kayttooikeus.service.TimeService;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static fi.vm.sade.kayttooikeus.util.FunctionalUtils.ifPresentOrElse;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@Service
@Transactional
@RequiredArgsConstructor
public class LdapSynchronizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapSynchronizer.class);
    // This value is a trigger for running all users
    public static final String RUN_ALL_BATCH = "runall";

    private final TimeService timeService;
    private final LdapService ldapService;
    private final LdapUpdateDataRepository ldapUpdateDataRepository;
    private final HenkiloDataRepository henkiloRepository;
    private final HenkiloHibernateRepository henkiloHibernateRepository;
    private final OppijanumerorekisteriClient oppijanumerorekisteriClient;

    /**
     * Suorittaa yhden synkronointiajon.
     *
     * @param previous edellisen synkronoinnin tiedot
     * @param nightTime yöaika päällä/pois päältä
     * @param batchSize käsiteltävien henkilöiden määrä
     * @param loadThresholdInSeconds synkronoinnin maksimikesto
     * @return tämän synkronoinnin tiedot
     */
    public Optional<LdapSynchronizationData> run(Optional<LdapSynchronizationData> previous,
            boolean nightTime, long batchSize, long loadThresholdInSeconds) {
        return new Synchronizer(nightTime, batchSize, loadThresholdInSeconds, timeService.getCurrentTimeMillis()).run(previous);
    }

    /**
     * Päivittää henkilön tiedot välittömästi LDAPiin.
     *
     * @param oid henkilö oid
     */
    public void synchronize(String oid) {
        ifPresentOrElse(oppijanumerorekisteriClient.findHenkiloByOid(oid).filter(LdapSynchronizer::isValid),
                this::synchronize,
                () -> ldapService.deleteByOid(oid));
    }

    private static boolean isValid(HenkiloDto henkilo) {
        return !henkilo.isPassivoitu();
    }

    private void synchronize(HenkiloDto dto) {
        String oid = dto.getOidHenkilo();
        ifPresentOrElse(henkiloRepository.findByOidHenkilo(oid).filter(LdapSynchronizer::isValid),
                entity -> ldapService.upsert(entity, dto),
                () -> ldapService.deleteByOid(oid));
    }

    private static boolean isValid(Henkilo henkilo) {
        return henkilo.getKayttajatiedot() != null && henkilo.getKayttajatiedot().getUsername() != null;
    }

    @RequiredArgsConstructor
    private class Synchronizer {

        private final boolean nightTime;
        private final long batchSize;
        private final long loadThresholdInSeconds;
        private final long start;

        public Optional<LdapSynchronizationData> run(Optional<LdapSynchronizationData> previous) {
            // käsitellään aina ensin ASAP-lista tyhjäksi riippumatta kellonajasta
            List<LdapUpdateData> toBeUpdated = getToBeUpdated(LdapPriorityType.ASAP, batchSize);
            if (!toBeUpdated.isEmpty()) {
                return Optional.of(synchronize(toBeUpdated));
            }

            if (nightTime) {
                return runNightSynchronizer();
            }
            return runDaySynchronizer(previous);
        }

        private Optional<LdapSynchronizationData> runNightSynchronizer() {
            List<LdapUpdateData> toBeUpdated = getToBeUpdated(LdapPriorityType.NIGHT, 1);
            if (!toBeUpdated.isEmpty()) {
                return Optional.of(createQueueData(toBeUpdated.get(0)));
            }
            toBeUpdated = getToBeUpdated(LdapPriorityType.NORMAL, batchSize);
            if (!toBeUpdated.isEmpty()) {
                return Optional.of(synchronize(toBeUpdated));
            }
            return Optional.empty();
        }

        private Optional<LdapSynchronizationData> runDaySynchronizer(Optional<LdapSynchronizationData> previous) {
            if (previous.map(t -> !t.isRunBatch()).orElse(true)) {
                List<LdapUpdateData> toBeUpdated = getToBeUpdated(LdapPriorityType.BATCH, 1);
                if (!toBeUpdated.isEmpty()) {
                    LdapSynchronizationData ldapSynchronizationData = createQueueData(toBeUpdated.get(0));
                    ldapUpdateDataRepository.delete(toBeUpdated.get(0));
                    return Optional.of(ldapSynchronizationData);
                }
            }
            List<LdapUpdateData> toBeUpdated = getToBeUpdated(LdapPriorityType.NORMAL, batchSize);
            if (!toBeUpdated.isEmpty()) {
                return Optional.of(synchronize(toBeUpdated));
            }
            if (previous.map(LdapSynchronizationData::isRunBatch).orElse(false)) {
                // luodaan merkintä jotta synkronointi ei jää jumiin tyhjän
                // batch-operaation jäljiltä (kts. metodin alussa if)
                return Optional.of(getSynchronizationData(0, false, false));
            }
            return Optional.empty();
        }

        private List<LdapUpdateData> getToBeUpdated(LdapPriorityType priority, long limit) {
            LdapUpdateDataCriteria criteria = LdapUpdateDataCriteria.builder()
                    .priorities(singletonList(priority))
                    .statuses(asList(LdapStatusType.IN_QUEUE, LdapStatusType.RETRY))
                    .build();
            return ldapUpdateDataRepository.findBy(criteria, limit);
        }

        private LdapSynchronizationData synchronize(Iterable<LdapUpdateData> updateDatas) {
            boolean coolOff = false;
            int i = 0;
            Iterator<LdapUpdateData> iterator = updateDatas.iterator();
            while (iterator.hasNext()) {
                LdapUpdateData updateData = iterator.next();
                long startTime = timeService.getCurrentTimeMillis();
                synchrorize(updateData);
                i++;
                long time = (timeService.getCurrentTimeMillis() - startTime) / 1000;
                if (time > loadThresholdInSeconds && iterator.hasNext()) {
                    coolOff = true;
                    break;
                }
            }
            return getSynchronizationData(i, coolOff, false);
        }

        private void synchrorize(LdapUpdateData updateData) {
            try {
                LdapSynchronizer.this.synchronize(updateData.getHenkiloOid());
                ldapUpdateDataRepository.delete(updateData);
            } catch (Exception e) {
                if (updateData.getStatus() == LdapStatusType.RETRY) {
                    LOGGER.error("Henkilön synkronointi epäonnistui {}, asetetaan tilaksi FAILED!", updateData.getHenkiloOid(), e);
                    updateData.setStatus(LdapStatusType.FAILED);
                } else {
                    LOGGER.warn("Henkilön synkronointi epäonnistui {}, asetetaan tilaksi RETRY.", updateData.getHenkiloOid(), e);
                    updateData.setStatus(LdapStatusType.RETRY);
                }
            }
        }

        private LdapSynchronizationData createQueueData(LdapUpdateData updateData) {
            int dataSize = 0;
            if (updateData.getKayttoOikeusRyhmaId() != null) {
                LOGGER.info("Lisätään käyttöoikeusryhmän {} henkilöt LDAP-synkronointijonoon", updateData.getKayttoOikeusRyhmaId());
                dataSize += createQueueDataFromKayttoOikeusRyhma(updateData.getKayttoOikeusRyhmaId());
            } else if (RUN_ALL_BATCH.equals(updateData.getHenkiloOid())) {
                LOGGER.info("Lisätään kaikki henkilöt LDAP-synkronointijonoon");
                dataSize += createQueueDataFromAll();
            } else {
                LOGGER.error("Tuntematon {} ", updateData);
            }
            return getSynchronizationData(dataSize, false, true);
        }

        private long createQueueDataFromKayttoOikeusRyhma(Long kayttoOikeusRyhmaId) {
            List<String> existingOids = new ArrayList<>();
            ldapUpdateDataRepository.findByHenkiloOidIsNotNull().forEach(updateData -> {
                if (updateData.getStatus() == LdapStatusType.FAILED) {
                    ldapUpdateDataRepository.delete(updateData);
                } else {
                    existingOids.add(updateData.getHenkiloOid());
                }
            });

            return henkiloHibernateRepository.findOidsByKayttoOikeusRyhmaId(kayttoOikeusRyhmaId).stream()
                    .filter(oid -> !existingOids.contains(oid))
                    .map(this::createHenkiloUpdateData)
                    .map(ldapUpdateDataRepository::save)
                    .count();
        }

        private long createQueueDataFromAll() {
            ldapUpdateDataRepository.deleteAllInBatch();
            return henkiloHibernateRepository.findOidsByHavingUsername().stream()
                    .map(this::createHenkiloUpdateData)
                    .map(ldapUpdateDataRepository::save)
                    .count();
        }

        private LdapUpdateData createHenkiloUpdateData(String henkiloOid) {
            return LdapUpdateData.builder()
                    .priority(LdapPriorityType.NORMAL)
                    .henkiloOid(henkiloOid)
                    .status(LdapStatusType.IN_QUEUE)
                    .modified(timeService.getDateTimeNow())
                    .build();
        }

        private LdapSynchronizationData getSynchronizationData(int dataSize, boolean coolOff, boolean runBatch) {
            long totalTime = timeService.getCurrentTimeMillis() - start;

            LdapSynchronizationData ldapSynchronizationData = new LdapSynchronizationData();
            ldapSynchronizationData.setLastRun(timeService.getDateTimeNow());
            ldapSynchronizationData.setAverageUpdateTime(totalTime, dataSize);
            ldapSynchronizationData.setTotalRuntimeInSeconds((int) (totalTime / 1000));
            ldapSynchronizationData.setTotalAmount(dataSize);
            ldapSynchronizationData.setCoolOff(coolOff);
            ldapSynchronizationData.setRunBatch(runBatch);
            return ldapSynchronizationData;
        }

    }

}
