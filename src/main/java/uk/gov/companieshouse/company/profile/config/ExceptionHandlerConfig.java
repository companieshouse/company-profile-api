package uk.gov.companieshouse.company.profile.config;

import static uk.gov.companieshouse.company.profile.CompanyProfileApiApplication.APPLICATION_NAME_SPACE;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import uk.gov.companieshouse.api.exception.BadRequestException;
import uk.gov.companieshouse.api.exception.DocumentNotFoundException;
import uk.gov.companieshouse.api.exception.MethodNotAllowedException;
import uk.gov.companieshouse.api.exception.ResourceStateConflictException;
import uk.gov.companieshouse.api.exception.ServiceUnavailableException;
import uk.gov.companieshouse.company.profile.exception.ConflictException;
import uk.gov.companieshouse.company.profile.exception.ResourceNotFoundException;
import uk.gov.companieshouse.company.profile.exception.SerDesException;
import uk.gov.companieshouse.company.profile.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@ControllerAdvice
public class ExceptionHandlerConfig {

    private static final String X_REQUEST_ID_HEADER = "x-request-id";
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    private Map<String, Object> responseAndLogBuilderHandler(Exception ex, WebRequest request) {
        var requestId = request.getHeader(X_REQUEST_ID_HEADER);
        if (StringUtils.isEmpty(requestId)) {
            requestId = generateShortCorrelationId();
        }

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("timestamp", LocalDateTime.now());
        responseBody.put("message", ex.getMessage());
        responseBody.put("request_id", requestId);

        LOGGER.error(ex.getMessage(), DataMapHolder.getLogMap());

        return responseBody;
    }

    /**
     * Runtime exception handler. Acts as the catch-all scenario.
     *
     * @param ex      exception to handle.
     * @param request request.
     * @return error response to return.
     */
    @ExceptionHandler(value = {Exception.class, SerDesException.class})
    public ResponseEntity<Object> handleException(Exception ex,
            WebRequest request) {
        return new ResponseEntity<>(responseAndLogBuilderHandler(ex, request),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * DocumentNotFoundException exception handler. Thrown when the requested document could not be found in the DB in place of
     * status code 404.
     *
     * @param ex      exception to handle.
     * @param request request.
     * @return error response to return.
     */
    @ExceptionHandler(value = {DocumentNotFoundException.class, ResourceNotFoundException.class})
    public ResponseEntity<Object> handleDocumentNotFoundException(Exception ex,
            WebRequest request) {
        return new ResponseEntity<>(responseAndLogBuilderHandler(ex, request),
                HttpStatus.NOT_FOUND);
    }

    /**
     * BadRequestException exception handler. Thrown when data is given in the wrong format.
     *
     * @param ex      exception to handle.
     * @param request request.
     * @return error response to return.
     */
    @ExceptionHandler(value = {BadRequestException.class, DateTimeParseException.class,
            HttpMessageNotReadableException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<Object> handleBadRequestException(Exception ex,
            WebRequest request) {
        return new ResponseEntity<>(responseAndLogBuilderHandler(ex, request),
                HttpStatus.BAD_REQUEST);

    }

    /**
     * MethodNotAllowedException exception handler.
     *
     * @param ex      exception to handle.
     * @param request request.
     * @return error response to return.
     */
    @ExceptionHandler(value = {MethodNotAllowedException.class,
            HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<Object> handleMethodNotAllowedException(Exception ex,
            WebRequest request) {
        return new ResponseEntity<>(responseAndLogBuilderHandler(ex, request),
                HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * ServiceUnavailableException exception handler. To be thrown when there are connection issues.
     *
     * @param ex      exception to handle.
     * @param request request.
     * @return error response to return.
     */
    @ExceptionHandler(value = {ServiceUnavailableException.class})
    public ResponseEntity<Object> handleServiceUnavailableException(Exception ex,
            WebRequest request) {
        return new ResponseEntity<>(responseAndLogBuilderHandler(ex, request),
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * ResourceStateConflictException handler. Thrown when the requested document already has a resource link or
     * delta at is outdated.
     *
     * @return error response to return.
     */
    @ExceptionHandler(value = {ResourceStateConflictException.class, ConflictException.class})
    public ResponseEntity<Object> handleResourceStateConflictException(Exception ex, WebRequest request) {
        return new ResponseEntity<>(responseAndLogBuilderHandler(ex, request),
                HttpStatus.CONFLICT);
    }

    private String generateShortCorrelationId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
