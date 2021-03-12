package fi.vm.sade.kayttooikeus.config.security.casoppija;

import fi.vm.sade.properties.OphProperties;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.TicketValidationException;
import org.jasig.cas.client.validation.TicketValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class SuomiFiAuthenticationProcessingFilter extends AbstractPreAuthenticatedProcessingFilter {

    public static final String OPPIJA_TICKET_PARAM_NAME = "ticket";
    public static final String HETU_ATTRIBUTE = "nationalIdentificationNumber";
    public static final String SUKUNIMI_ATTRIBUTE = "sn";
    public static final String ETUNIMET_ATTRIBUTE = "firstName";
    static final String SUOMI_FI_DETAILS_ATTR_KEY = "suomiFiDetails";

    private static final Logger LOGGER = LoggerFactory.getLogger(SuomiFiAuthenticationProcessingFilter.class);
    private final TicketValidator ticketValidator;
    private final String service;

    public SuomiFiAuthenticationProcessingFilter(OphProperties ophProperties, TicketValidator ticketValidator) {
        this.ticketValidator = ticketValidator;
        this.service = ophProperties.url("kayttooikeus-service.cas.tunnistus");
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(HttpServletRequest httpServletRequest) {
        String ticket = httpServletRequest.getParameter(OPPIJA_TICKET_PARAM_NAME);
        if (ticket != null) {
            LOGGER.info("Validating ticket: \"{}\"", ticket);
            try {
                Assertion assertion = ticketValidator.validate(ticket, service);
                if (assertion.isValid()) {
                    LOGGER.info("Ticket \"{}\" is valid.", ticket);
                    Map<String, Object> attributes = assertion.getPrincipal().getAttributes();
                    String hetu = (String) attributes.get(HETU_ATTRIBUTE);
                    String sukunimi = (String) attributes.get(SUKUNIMI_ATTRIBUTE);
                    String etunimet = (String) attributes.get(ETUNIMET_ATTRIBUTE);
                    SuomiFiUserDetails details = new SuomiFiUserDetails(hetu, sukunimi, etunimet);
                    httpServletRequest.setAttribute(SUOMI_FI_DETAILS_ATTR_KEY, details);
                    return String.join(", ", sukunimi, etunimet);
                } else {
                    LOGGER.warn("Invalid ticket: \"{}\"", ticket);
                }
            } catch (TicketValidationException e) {
                LOGGER.warn("Failed to validate ticket: \"{}\"", ticket);
            }
        }
        return null;
    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getParameter(OPPIJA_TICKET_PARAM_NAME);
    }

}
