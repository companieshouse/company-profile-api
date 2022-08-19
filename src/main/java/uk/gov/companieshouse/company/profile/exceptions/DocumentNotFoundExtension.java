package uk.gov.companieshouse.company.profile.exceptions;

public class DocumentNotFoundExtension extends RuntimeException {

    public DocumentNotFoundExtension(String message) {
        super(message);
    }
}
