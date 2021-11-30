package fi.vm.sade.kayttooikeus.config.scheduling;

import fi.vm.sade.kayttooikeus.model.Identifiable;
import fi.vm.sade.kayttooikeus.service.ExpiringEntitiesService;
import org.slf4j.Logger;

import java.time.Period;
import java.util.Collection;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingInt;

public interface ExpiringEntitiesTask<T extends Identifiable> {

    default void expire(String name, ExpiringEntitiesService<T> service, Logger logger, Period threshold) {
        Collection<T> entities = service.findExpired(threshold);
        logger.info("Discarding {} expired {}s", entities.size(), name);
        Map<Boolean, Integer> result = entities.stream()
                .map(entity -> {
                    try {
                        service.discard(entity);
                        sendNotification(entity);
                    } catch (Exception e) {
                        logger.warn("Error while discarding {} id: {}", name, entity.getId(), e);
                        return false;
                    }
                    return true;
                })
                .collect(groupingBy(Boolean::booleanValue, summingInt(success -> 1)));
        if (!result.isEmpty()) {
            logger.info("Sent discarded {}s notifications. {} success, {} failures",
                    name,
                    result.getOrDefault(true, 0),
                    result.getOrDefault(false, 0));
        }
        if (result.containsKey(false)) {
            logger.error("There were errors while discarding {}s, please check the logs", name);
        }
    }

    void sendNotification(T entity);
}
