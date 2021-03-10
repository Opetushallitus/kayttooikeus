package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.service.OppijaCasTicketService;
import fi.vm.sade.kayttooikeus.service.dto.OppijaCasTunnistusDto;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.TicketValidationException;
import org.jasig.cas.client.validation.TicketValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

import static fi.vm.sade.kayttooikeus.config.security.TunnistusSecurityConfig.OPPIJA_TICKET_VALIDATOR_QUALIFIER;

@Service
public class OppijaCasTicketServiceImpl implements OppijaCasTicketService {

    private static final String HETU_ATTRIBUTE = "nationalIdentificationNumber";
    private static final String SUKUNIMI_ATTRIBUTE = "sn";
    private static final String ETUNIMET_ATTRIBUTE = "firstName";
    private final TicketValidator oppijaTicketValidator;

    @Autowired
    public OppijaCasTicketServiceImpl(
            @Qualifier(OPPIJA_TICKET_VALIDATOR_QUALIFIER) TicketValidator oppijaTicketValidator) {
        this.oppijaTicketValidator = oppijaTicketValidator;
    }

    @Override
    public OppijaCasTunnistusDto haeTunnistustiedot(String casTicket, String service) throws TicketValidationException {
        Assertion assertion = oppijaTicketValidator.validate(casTicket, service);
        if (assertion.isValid()) {
            Map<String,Object> attributes = assertion.getPrincipal().getAttributes();
            String hetu = (String) attributes.get(HETU_ATTRIBUTE);
            String sukunimi = (String) attributes.get(SUKUNIMI_ATTRIBUTE);
            String etunimet = (String) attributes.get(ETUNIMET_ATTRIBUTE);
            return new OppijaCasTunnistusDto(hetu, sukunimi, etunimet);
        }
        throw new TicketValidationException("Ticket is not valid");
    }
}
