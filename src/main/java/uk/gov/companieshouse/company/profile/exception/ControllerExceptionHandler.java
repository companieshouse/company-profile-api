package uk.gov.companieshouse.company.profile.exception;

import java.time.LocalDateTime;
import java.util.HashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@ControllerAdvice
public class ControllerExceptionHandler {
    public static final String APPLICATION_NAME_SPACE = "company-profile-api";

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    /**
     * Runtime exception 500 handler. Acts as the catch-all scenario.
     *
     * @param ex exception to handle.
     * @return error response to return.
     */
    @ExceptionHandler(value = {Exception.class})
    public ResponseEntity<Object> handleException(Exception ex) {
        String errMsg = "Unexpected exception";
        LOGGER.error(errMsg, ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildExceptionResponse(errMsg));
    }

    /**
     * BadRequestException 400 handler.
     * Thrown when data is given in the wrong format.
     *
     * @param ex exception to handle.
     * @return error response to return.
     */
    @ExceptionHandler(value = {BadRequestException.class})
    public ResponseEntity<Object> handleBadRequestException(Exception ex) {
        String errMsg = "Bad request";
        LOGGER.error(errMsg, ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildExceptionResponse(errMsg));
    }

    /**
     * MethodNotSupported 405 handler.
     * Thrown when GET is called with a PATCH request.
     *
     * @param ex exception to handle.
     * @return error response to return.
     */
    @ExceptionHandler(value = {HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<Object> handleMethodNotSupportedException(Exception ex) {
        String errMsg = "Method not allowed";
        LOGGER.error(errMsg, ex);
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(buildExceptionResponse(errMsg));
    }

    /**
     * ServiceUnavailableException 503 handler.
     * To be thrown when there are connection issues.
     *
     * @param ex exception to handle.
     * @return error response to return.
     */
    @ExceptionHandler(value = {ServiceUnavailableException.class})
    public ResponseEntity<Object> handleServiceUnavailableException(Exception ex) {
        String errMsg = "Service unavailable";
        LOGGER.error(errMsg, ex);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildExceptionResponse(errMsg));
    }

    private HashMap<String, Object> buildExceptionResponse(String errMsg) {
        HashMap<String, Object> response = new HashMap<>();
        response.put("message", errMsg);
        response.put("timestamp", LocalDateTime.now());
        return response;
    }
}