package uk.gov.companieshouse.company.profile.api;

import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.chskafka.ChangedResourceEvent;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.chskafka.request.PrivateChangedResourcePost;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.profile.exceptions.ServiceUnavailableException;
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
        logger.trace(String.format("Created client %s", resourceHandler));

        PrivateChangedResourcePost changedResourcePost =
                internalApiClient.privateChangedResourceHandler().postChangedResource(
                        CHANGED_RESOURCE_URI, mapChangedResource(contextId, companyNumber));

        try {
            return changedResourcePost.execute();
        } catch (ApiErrorResponseException exp) {
            HttpStatus statusCode = HttpStatus.valueOf(exp.getStatusCode());
            if (statusCode == HttpStatus.SERVICE_UNAVAILABLE) {
                logger.error(String.format("Service unavailable while calling /resource-changed "
                        + "with contextId %s and company number %s", contextId, companyNumber),
                        exp);
                throw new ServiceUnavailableException(exp.getMessage());
            } else {
                logger.error(String.format("Error occurred while calling /resource-changed with "
                        + "contextId %s and company number %s", contextId, companyNumber), exp);
                throw exp;
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
