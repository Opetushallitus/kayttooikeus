package fi.vm.sade.kayttooikeus.controller;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import fi.vm.sade.generic.common.ValidationException;
import fi.vm.sade.kayttooikeus.service.exception.*;
import fi.vm.sade.kayttooikeus.service.external.ExternalServiceException;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.common.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.ldap.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

@ControllerAdvice(basePackageClasses = ErrorHandlerAdvice.class)
public class ErrorHandlerAdvice {
    public static final Locale FI = new Locale("fi", "FI");
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandlerAdvice.class);
    private static final Function<? super ConstraintViolation<?>, String> MESSAGES_TRANSFORMER = violation 
            -> violation == null ? null : violation.getMessage() + ": " + violation.getInvalidValue();
    private static final Function<? super ConstraintViolation<?>, ViolationDto> VIOLATIONS_TRANSFORMER = violation
            -> violation == null ? null : new ViolationDto(violation.getPropertyPath().toString(),
                Iterators.getLast(violation.getPropertyPath().iterator()).toString(), violation.getMessage(),
                    violation.getInvalidValue());
    
    private MessageSource messageSource;

    @Autowired
    public ErrorHandlerAdvice(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND) // 404 Entity not found by primary key.
    @ExceptionHandler(NotFoundException.class)
    @ResponseBody
    public Map<String,Object> notFound(HttpServletRequest req, NotFoundException exception) {
        return handleException(req, exception, "error_NotFoundException",
                !Objects.equals(exception.getMessage(), "") ?
                        exception.getMessage() :
                        messageSource.getMessage("error_NotFoundException", new Object[0], getLocale(req)));
    }
    
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthenticationException.class)
    @ResponseBody
    public Map<String,Object> unauthorized(HttpServletRequest req, AuthenticationException exception) {
        return handleException(req, exception, "error_NotAuthorizedException",
                messageSource.getMessage("error_NotAuthorizedException", new Object[0], getLocale(req)));
    }

    @ResponseStatus(value = HttpStatus.UNAUTHORIZED) // 401 Not authorized
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    public Map<String,Object> notAuthorized(HttpServletRequest req, AccessDeniedException exception) {
        return handleException(req, exception, "error_NotAuthorizedException",
                messageSource.getMessage("error_NotAuthorizedException", new Object[0], getLocale(req)));
    }

    @ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "unauthorized") // 401 Not authorized
    @ExceptionHandler(UnauthorizedException.class)
    public void unauthorized() {
    }

    @ResponseStatus(value = HttpStatus.GATEWAY_TIMEOUT) // 504: Gateway Timeout
    @ExceptionHandler(ExternalServiceException.class)
    @ResponseBody
    public Map<String,Object> errorCallingExternalService(HttpServletRequest req, ExternalServiceException exception) {
        return handleException(req, exception, "error_calling_external_service",
                messageSource.getMessage("error_calling_external_service", new Object[]{exception.getResource()}, getLocale(req)));
    }

    private Locale getLocale(HttpServletRequest req) {
        Locale locale;
        if (req.getParameter("lang") != null) {
            locale = Locale.forLanguageTag(req.getParameter("lang"));
        } else if (req.getParameter("locale") != null) {
            locale = Locale.forLanguageTag(req.getParameter("locale"));
        } else {
            locale = FI;
        }
        return locale;
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST) // 400 Bad request.
    @ExceptionHandler(org.hibernate.exception.ConstraintViolationException.class)
    @ResponseBody
    public Map<String,Object> constraintViolatingRequest(HttpServletRequest req, ConstraintViolationException exception) {
        return handleConstraintViolations(req, exception, exception.getConstraintViolations());
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST) // 400 Bad request.
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    @ResponseBody
    public Map<String,Object> methodArgumentNotValidException(HttpServletRequest req, MethodArgumentNotValidException exception) {
        return handleConstraintViolations(req, exception, exception.getBindingResult());
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST) // 400 Bad request.
    @ExceptionHandler({ConstraintViolationException.class, javax.validation.ValidationException.class})
    @ResponseBody
    public Map<String,Object> badRequest(HttpServletRequest req, ConstraintViolationException exception) {
        return handleConstraintViolations(req, exception, exception.getConstraintViolations());
    }
    
    @ResponseStatus(value = HttpStatus.BAD_REQUEST) // 400 Bad request.
    @ExceptionHandler(ValidationException.class)
    @ResponseBody
    public Map<String,Object> badRequest(HttpServletRequest req, ValidationException exception) {
        Collection<ViolationDto> violations = exception.getViolations() != null ? Collections2.transform(exception.getViolations(), VIOLATIONS_TRANSFORMER::apply) : new ArrayList<>();
        Collection<String> violationsMsgs = exception.getValidationMessages();
        Map<String,Object> result = handleException(req, exception, exception.getKey(),
                exception.getKey() != null ? messageSource.getMessage(exception.getKey(), new Object[0], getLocale(req)) + (violations.isEmpty() ? "" : ": ") : ""
                + StringHelper.join(", ", violationsMsgs.iterator()));
        result.put("errors", violationsMsgs);
        result.put("violations", violations);
        return result;
    }
    
    @ResponseStatus(value = HttpStatus.BAD_REQUEST) // 400 Bad Request
    @ExceptionHandler({IllegalArgumentException.class, PasswordException.class, UsernameAlreadyExistsException.class})
    @ResponseBody
    public Map<String,Object> badRequest(HttpServletRequest req, RuntimeException exception) {
        return handleException(req, exception, "bad_request_illegal_argument", exception.getMessage());
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST) // 400 Bad Request
    @ExceptionHandler(fi.vm.sade.kayttooikeus.service.exception.ValidationException.class)
    @ResponseBody
    public Map<String,Object> badRequest(HttpServletRequest req, fi.vm.sade.kayttooikeus.service.exception.ValidationException exception) {
        return handleException(req, exception, "bad_request", exception.getMessage());
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN) // 403 Forbidden
    @ExceptionHandler(ForbiddenException.class)
    @ResponseBody
    public Map<String,Object> forbidden(HttpServletRequest req, ForbiddenException exception) {
        return handleException(req, exception, "forbidden", exception.getMessage());
    }

    @ExceptionHandler(DataInconsistencyException.class)
    public ResponseEntity<Map<String, Object>> dataInconsistencyException(DataInconsistencyException exception, HttpServletRequest request) {
        logger.error(exception.getMessage(), exception);
        return constructErrorResponse(exception, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR) // 500 Internal
    @ExceptionHandler(Exception.class)
    @ResponseBody // any other type
    public Map<String,Object> internalError(HttpServletRequest req, Exception exception) {
        return handleException(req, exception, "internal_server_error", exception.getMessage());
    }

    @ExceptionHandler(UnprocessableEntityException.class)
    public ResponseEntity<Map<String, Object>> unprocessableEntityException(UnprocessableEntityException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        Map<String, Object> body = constructErrorBody(exception, status, request);
        Errors errors = exception.getErrors();
        body.put("globalErrors", errors.getGlobalErrors().stream()
                .map(this::constructObjectError)
                .collect(toList()));
        body.put("fieldErrors", errors.getFieldErrors().stream()
                .map(this::constructFieldError)
                .collect(toList()));
        return ResponseEntity.status(status).body(body);
    }

    private Map<String, Object> constructObjectError(ObjectError objectError) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", objectError.getCode());
        body.put("arguments", objectError.getArguments());
        body.put("message", objectError.getDefaultMessage());
        return body;
    }

    private Map<String, Object> constructFieldError(FieldError fieldError) {
        Map<String, Object> body = constructObjectError(fieldError);
        body.put("field", fieldError.getField());
        body.put("rejectedValue", fieldError.getRejectedValue());
        return body;
    }

    private ResponseEntity<Map<String, Object>> constructErrorResponse(Exception exception, HttpStatus status, HttpServletRequest request) {
        Map<String, Object> body = constructErrorBody(exception, status, request);
        return ResponseEntity.status(status).body(body);
    }

    private Map<String, Object> constructErrorBody(Exception exception, HttpStatus status, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", new Date());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("exception", exception.getClass().getCanonicalName());
        body.put("message", exception.getMessage());
        body.put("path", request.getRequestURI());
        return body;
    }

    private Map<String,Object> handleConstraintViolations(HttpServletRequest req, Exception exception,
                                                          Set<? extends ConstraintViolation<?>> exViolations) {
        Collection<ViolationDto> violations = Collections2.transform(exViolations, VIOLATIONS_TRANSFORMER::apply);
        Collection<String> violationsMsgs = Collections2.transform(exViolations, MESSAGES_TRANSFORMER::apply);
        Map<String,Object> result = handleException(req, exception, "bad_request_error",
                StringHelper.join(", ", violationsMsgs.iterator()));
        result.put("errors", violationsMsgs);
        result.put("violations", violations);
        return result;
    }

    private Map<String,Object> handleConstraintViolations(HttpServletRequest req, Exception exception,
                                                          BindingResult exViolations) {
        Collection<ViolationDto> violations = exViolations.getFieldErrors().stream()
                .map(ex -> new ViolationDto(ex.getField(), ex.getField(), ex.getDefaultMessage(), ex.getRejectedValue()))
                .collect(toList());
        Collection<String> violationsMsgs = exViolations.getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(toList());
        Map<String,Object> result = handleException(req, exception, "bad_request_error",
                StringHelper.join(", ", violationsMsgs.iterator()));
        result.put("errors", violationsMsgs);
        result.put("violations", violations);
        return result;
    }

    private Map<String,Object> handleException(HttpServletRequest req, Throwable exception, String messageKey, String message,
                                               Object... params) {
        logger.error("Request: " + req.getRequestURL() + " raised " + exception, exception);
        Map<String,Object> result = new HashMap<String,Object>();
        result.put("message", message);
        result.put("messageKey", messageKey);
        result.put("messageParams", params);
        result.put("errorType", exception.getClass().getSimpleName());
        result.put("url", req.getRequestURL());
        result.put("method", req.getMethod());
        result.put("parameters", req.getParameterMap());
        return result;
    }
    
    @Getter @Setter
    public static class ViolationDto implements Serializable {
        private String field;
        private String path;
        private String message;
        private Object value;

        public ViolationDto() {
        }

        public ViolationDto(String path, String field, String message, Object value) {
            this.path = path;
            this.field = field;
            this.message = message;
            this.value = value;
        }
    }
}
