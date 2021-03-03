package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.service.dto.OppijaCasTunnistusDto;
import org.jasig.cas.client.validation.TicketValidationException;

public interface OppijaCasTicketService {

    OppijaCasTunnistusDto haeTunnistustiedot(String casTicket, String service) throws TicketValidationException;

}
