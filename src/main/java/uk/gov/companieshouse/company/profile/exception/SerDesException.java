package uk.gov.companieshouse.company.profile.exception;

public class SerDesException extends RuntimeException {

    public SerDesException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
