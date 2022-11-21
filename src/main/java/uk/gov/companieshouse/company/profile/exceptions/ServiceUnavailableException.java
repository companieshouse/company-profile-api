package uk.gov.companieshouse.company.profile.exceptions;

import org.springframework.dao.DataAccessException;

public class ServiceUnavailableException extends DataAccessException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}