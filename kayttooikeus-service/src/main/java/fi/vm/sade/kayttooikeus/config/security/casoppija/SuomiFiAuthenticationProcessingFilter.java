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

    public static final String HETU_ATTRIBUTE = "nationalIdentificationNumber";
    public static final String SUKUNIMI_ATTRIBUTE = "sn";
    public static final String ETUNIMET_ATTRIBUTE = "FirstName";
    public static final String ETUNIMET_ALT_ATTRIBUTE = "firstName";
    static final String OPPIJA_TICKET_PARAM_NAME = "ticket";
    static final String SUOMI_FI_DETAILS_ATTR_KEY = "suomiFiDetails";

    private static final Logger LOGGER = LoggerFactory.getLogger(SuomiFiAuthenticationProcessingFilter.class);
    private final TicketValidator ticketValidator;

    public SuomiFiAuthenticationProcessingFilter(TicketValidator ticketValidator) {
        this.ticketValidator = ticketValidator;
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        String ticket = request.getParameter(OPPIJA_TICKET_PARAM_NAME);
        if (ticket != null) {
            String parameters = extractServiceParameters(request);
            String service = String.join("?", request.getRequestURL(), parameters);
            LOGGER.info("Validating ticket: \"{}\"", ticket);
            try {
                Assertion assertion = ticketValidator.validate(ticket, service);
                if (assertion.isValid()) {
                    LOGGER.info("Ticket \"{}\" is valid.", ticket);
                    SuomiFiUserDetails details = extractUserDetails(assertion);
                    // asetetaan requestiin, SuomiFiAuthenticationDetailsSourcen saataville
                    request.setAttribute(SUOMI_FI_DETAILS_ATTR_KEY, details);
                    request.getSession().setAttribute("SUOMIFI_SESSION", true);
                    return String.join(", ", details.sukunimi, details.etunimet);
                } else {
                    LOGGER.warn("Invalid ticket: \"{}\"", ticket);
                }
            } catch (TicketValidationException e) {
                LOGGER.warn("Failed to validate ticket: \"" + ticket + "\"", e);
            }
        }
        return null;
    }

    String extractServiceParameters(HttpServletRequest request) {
        return request.getParameterMap().entrySet().stream()
                .filter(entry -> !entry.getKey().equals(OPPIJA_TICKET_PARAM_NAME))
                .map(entry ->
                        Stream.of(entry.getValue())
                                .map(value -> entry.getKey() + "=" + value)
                                .collect(Collectors.joining("&")))
                .collect(Collectors.joining("&"));
    }

    SuomiFiUserDetails extractUserDetails(Assertion assertion) {
        Map<String, Object> attributes = assertion.getPrincipal().getAttributes();
        String hetu = (String) attributes.get(HETU_ATTRIBUTE);
        String sukunimi = (String) attributes.get(SUKUNIMI_ATTRIBUTE);
        // epäselvää, millä attribuutilla etunimet ovat - esimerkeissä ristiriitaista tietoa!
        String etunimet = (String) attributes.getOrDefault(
                ETUNIMET_ATTRIBUTE, attributes.get(ETUNIMET_ALT_ATTRIBUTE));
        return new SuomiFiUserDetails(hetu, sukunimi, etunimet);
    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
        return request.getParameter(OPPIJA_TICKET_PARAM_NAME);
    }

}
