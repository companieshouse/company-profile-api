package uk.gov.companieshouse.company.profile.exceptions;

public class ResourceStateConflictException extends RuntimeException {

    public ResourceStateConflictException(String message) {
        super(message);
    }
}
