package fi.vm.sade.kayttooikeus.config.security.casoppija;

import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.TicketValidationException;
import org.jasig.cas.client.validation.TicketValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SuomiFiAuthenticationProcessingFilter extends AbstractPreAuthenticatedProcessingFilter {

    public static final String OPPIJA_TICKET_PARAM_NAME = "ticket";
    public static final String HETU_ATTRIBUTE = "nationalIdentificationNumber";
    public static final String SUKUNIMI_ATTRIBUTE = "sn";
    public static final String ETUNIMET_ATTRIBUTE = "firstName";
    static final String SUOMI_FI_DETAILS_ATTR_KEY = "suomiFiDetails";

    private static final Logger LOGGER = LoggerFactory.getLogger(SuomiFiAuthenticationProcessingFilter.class);
    private final TicketValidator ticketValidator;

    public SuomiFiAuthenticationProcessingFilter(TicketValidator ticketValidator) {
        this.ticketValidator = ticketValidator;
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        String ticket = request.getParameter(OPPIJA_TICKET_PARAM_NAME);
        String parameters = request.getParameterMap().entrySet().stream()
                .filter(entry -> !entry.getKey().equals(OPPIJA_TICKET_PARAM_NAME))
                .map(entry ->
                        Stream.of(entry.getValue())
                                .map(value -> entry.getKey() + "=" + value)
                                .collect(Collectors.joining("&")))
                .collect(Collectors.joining("&"));
        String service = String.join("?", request.getRequestURL(), parameters);
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
                    request.setAttribute(SUOMI_FI_DETAILS_ATTR_KEY, details);
                    return String.join(", ", sukunimi, etunimet);
                } else {
                    LOGGER.warn("Invalid ticket: \"{}\"", ticket);
                }
            } catch (TicketValidationException e) {
                LOGGER.warn("Failed to validate ticket: \"" + ticket + "\"", e);
            }
        }
        return null;
    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
        return request.getParameter(OPPIJA_TICKET_PARAM_NAME);
    }

}
