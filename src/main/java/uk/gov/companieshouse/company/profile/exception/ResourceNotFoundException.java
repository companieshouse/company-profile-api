package uk.gov.companieshouse.company.profile.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

public class ResourceNotFoundException extends ResponseStatusException {

    //This exception class can be replaced in favour of data-sync library class once it has been
    //upgraded to use HttpStatusCode instead of HttpStatus as HttpStatus does not work in spring 3+
    public ResourceNotFoundException(HttpStatusCode status, String msg) {
        super(status, msg);
    }

}
