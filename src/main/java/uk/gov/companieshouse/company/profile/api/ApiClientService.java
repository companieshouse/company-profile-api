package uk.gov.companieshouse.company.profile.api;

import uk.gov.companieshouse.api.InternalApiClient;

/**
 * The {@code ApiClientService} interface provides an abstraction that can be used when testing {@code ApiClientManager} static
 * methods, without imposing the use of a test framework that supports mocking of static methods.
 */
public interface ApiClientService {

    InternalApiClient getInternalApiClient();
}
