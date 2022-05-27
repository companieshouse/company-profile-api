package uk.gov.companieshouse.company.profile.exception;

import java.time.LocalDateTime;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.companieshouse.logging.Logger;

@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {
    // The correlation identifier from the header
    private static final String X_REQUEST_ID_HEADER = "x-request-id";

    private final Logger logger;

    @Autowired
    public ControllerExceptionHandler(Logger logger) {
        super();
        this.logger = logger;
    }

    @Override
    @NonNull
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {
        String errMsg = "Bad request";
        HashMap<String, Object> data = buildExceptionResponse(errMsg);
        logger.errorContext(request.getHeader(X_REQUEST_ID_HEADER), errMsg, ex, data);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(data);
    }

    /**
     * MethodNotSupported 405 handler.
     * Thrown when GET is called with a PATCH request.
     *
     * @param ex exception to handle.
     * @return error response to return.
     */
    @Override
    @NonNull
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {
        String errMsg = "Method not allowed";
        HashMap<String, Object> data = buildExceptionResponse(errMsg);
        logger.errorContext(request.getHeader(X_REQUEST_ID_HEADER), errMsg, ex, data);

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(data);
    }


    /**
     * Runtime exception 500 handler. Acts as the catch-all scenario.
     *
     * @param ex exception to handle.
     * @return error response to return.
     */
    @ExceptionHandler(value = {Exception.class})
    public ResponseEntity<Object> handleDefaultException(Exception ex, WebRequest request) {
        String errMsg = "Unexpected exception";
        HashMap<String, Object> data = buildExceptionResponse(errMsg);
        logger.errorContext(request.getHeader(X_REQUEST_ID_HEADER), errMsg, ex, data);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(data);
    }

    /**
     * BadRequestException 400 handler.
     * Thrown when data is given in the wrong format.
     *
     * @param ex exception to handle.
     * @return error response to return.
     */
    @ExceptionHandler(value = {BadRequestException.class})
    public ResponseEntity<Object> handleBadRequestException(Exception ex, WebRequest request) {
        String errMsg = "Bad request";
        HashMap<String, Object> data = buildExceptionResponse(errMsg);
        logger.errorContext(request.getHeader(X_REQUEST_ID_HEADER), errMsg, ex, data);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(data);
    }

    /**
     * BadRequestException 400 handler.
     * Thrown when data in RequestBody is not valid.
     *
     * @param ex exception to handle.
     * @return error response to return.
     */
    @Override
    @NonNull
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {
        String errMsg = "Bad request";
        HashMap<String, Object> data = buildExceptionResponse(errMsg);
        logger.errorContext(request.getHeader(X_REQUEST_ID_HEADER), errMsg, ex, data);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(data);
    }


    /**
     * IllegalArgumentException exception handler.
     *
     * @param ex      exception to handle.
     * @param request request.
     * @return error response to return.
     */
    @ExceptionHandler(value = {IllegalArgumentException.class})
    public ResponseEntity<Object> handleNotFoundException(Exception ex, WebRequest request) {
        String errMsg = "Resource not found";
        HashMap<String, Object> data = buildExceptionResponse(errMsg);
        logger.errorContext(request.getHeader(X_REQUEST_ID_HEADER), errMsg, ex, data);

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(data);
    }

    /**
     * ServiceUnavailableException 503 handler.
     * To be thrown when there are connection issues.
     *
     * @param ex exception to handle.
     * @return error response to return.
     */
    @ExceptionHandler(value = {ServiceUnavailableException.class})
    public ResponseEntity<Object> handleServiceUnavailableException(Exception ex,
                                                                    WebRequest request) {
        String errMsg = "Service unavailable";
        HashMap<String, Object> data = buildExceptionResponse(errMsg);
        logger.errorContext(request.getHeader(X_REQUEST_ID_HEADER), errMsg, ex, data);

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(data);
    }

    private HashMap<String, Object> buildExceptionResponse(String errMsg) {
        HashMap<String, Object> response = new HashMap<>();
        response.put("message", errMsg);
        response.put("timestamp", LocalDateTime.now());
        return response;
    }
}