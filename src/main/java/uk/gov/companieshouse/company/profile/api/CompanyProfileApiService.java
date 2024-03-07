package uk.gov.companieshouse.company.profile.api;

import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.chskafka.ChangedResourceEvent;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.exception.ServiceUnavailableException;
import uk.gov.companieshouse.api.handler.chskafka.request.PrivateChangedResourcePost;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.profile.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;

@Service
public class CompanyProfileApiService {

    private static final String CHANGED_EVENT_TYPE = "changed";
    private static final String CHANGED_RESOURCE_URI = "/resource-changed";
    private final Logger logger;
    private final ApiClientService apiClientService;

    /**
     * Invoke Insolvency API.
     */
    public CompanyProfileApiService(ApiClientService apiClientService,
                                    Logger logger) {
        this.apiClientService = apiClientService;
        this.logger = logger;
    }

    /**
     * Call chs-kafka api.
     *
     * @param companyNumber company insolvency number
     * @return response returned from chs-kafka api
     */
    public ApiResponse<Void> invokeChsKafkaApi(String contextId, String companyNumber)
            throws ApiErrorResponseException {
        InternalApiClient internalApiClient = apiClientService.getInternalApiClient();
        String resourceHandler = internalApiClient.getInternalBasePath();
        logger.traceContext(contextId, String.format("Created client %s", resourceHandler),
                DataMapHolder.getLogMap());

        PrivateChangedResourcePost changedResourcePost =
                internalApiClient.privateChangedResourceHandler().postChangedResource(
                        CHANGED_RESOURCE_URI, mapChangedResource(contextId, companyNumber));

        try {
            return changedResourcePost.execute();
        } catch (ApiErrorResponseException ex) {
            HttpStatus statusCode = HttpStatus.valueOf(ex.getStatusCode());
            if (statusCode == HttpStatus.SERVICE_UNAVAILABLE) {
                logger.errorContext(contextId, String.format("Service unavailable while "
                        + "calling /resource-changed with company number %s", companyNumber),
                        new ServiceUnavailableException(ex.getMessage()),
                        DataMapHolder.getLogMap());
                throw new ServiceUnavailableException(ex.getMessage());
            } else {
                logger.errorContext(contextId, String.format("Error occurred while calling "
                        + "/resource-changed with company number %s", companyNumber), ex,
                        DataMapHolder.getLogMap());
                throw ex;
            }
        }
    }

    private ChangedResource mapChangedResource(String contextId, String companyNumber) {
        String resourceUri = "/company/" + companyNumber;

        ChangedResourceEvent event = new ChangedResourceEvent();
        event.setType(CHANGED_EVENT_TYPE);
        event.publishedAt(String.valueOf(OffsetDateTime.now()));

        ChangedResource changedResource = new ChangedResource();
        changedResource.setResourceUri(resourceUri);
        changedResource.event(event);
        changedResource.setResourceKind("company-profile");
        changedResource.setContextId(contextId);

        return changedResource;
    }

}
