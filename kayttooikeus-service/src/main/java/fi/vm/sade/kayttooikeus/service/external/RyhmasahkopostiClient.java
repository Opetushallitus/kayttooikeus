package fi.vm.sade.kayttooikeus.service.external;

import fi.vm.sade.ryhmasahkoposti.api.dto.EmailData;

public interface RyhmasahkopostiClient {
    /**
     * @param emailData to send
     * @return response from ryhmasahkoposti-service
     * @throws Exception on failure
     */
    String sendRyhmasahkoposti(EmailData emailData);
}
