package uk.gov.companieshouse.company.profile.exceptions;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String msg, Throwable ex) {
        super(msg, ex);
    }
}
