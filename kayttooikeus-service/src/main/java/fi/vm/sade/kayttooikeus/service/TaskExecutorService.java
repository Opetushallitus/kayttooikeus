package fi.vm.sade.kayttooikeus.service;


import java.time.Period;

public interface TaskExecutorService {
    int sendExpirationReminders(Period...expireThresholds);

    void discardExpiredInvitations(Period threshold);

    void discardExpiredApplications(Period threshold);
}
