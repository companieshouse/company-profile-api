package uk.gov.companieshouse.company.profile.exceptions;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(String message) {
        super(message);
    }
}
