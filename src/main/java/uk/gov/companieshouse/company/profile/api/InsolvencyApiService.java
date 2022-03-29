package uk.gov.companieshouse.company.profile.api;

import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.chskafka.ChangedResourceEvent;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.chskafka.request.PrivateChangedResourcePost;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.logging.Logger;

@Service
public class InsolvencyApiService {

    private static final String CHANGED_RESOURCE_URI = "/resource-changed";
    private final Logger logger;
    private final String chsKafkaUrl;
    private final ApiClientService apiClientService;

    /**
     * Invoke Insolvency API.
     */
    public InsolvencyApiService(@Value("${chs.kafka.api.endpoint}")String chsKafkaUrl,
                                ApiClientService apiClientService, Logger logger) {
        this.chsKafkaUrl = chsKafkaUrl;
        this.apiClientService = apiClientService;
        this.logger = logger;
    }

    /**
     * Call chs-kafka api.
     * @param companyNumber company insolvency number
     * @return response returned from chs-kafka api
     */
    public ApiResponse<Void> invokeChsKafkaApi(String contextId, String companyNumber) {
        InternalApiClient internalApiClient = apiClientService.getInternalApiClient();
        internalApiClient.setBasePath(chsKafkaUrl);

        PrivateChangedResourcePost changedResourcePost =
                internalApiClient.privateChangedResourceHandler().postChangedResource(
                        CHANGED_RESOURCE_URI, mapChangedResource(contextId, companyNumber));

        try {
            return changedResourcePost.execute();
        } catch (ApiErrorResponseException exp) {
            logger.error("Error occurred while calling /resource-changed endpoint", exp);
            throw new RuntimeException();
        }
    }

    private ChangedResource mapChangedResource(String contextId, String companyNumber) {
        String resourceUri = "/company/" + companyNumber + "/company-insolvency";

        ChangedResourceEvent event = new ChangedResourceEvent();
        event.setType("changed");
        event.publishedAt(String.valueOf(OffsetDateTime.now()));

        ChangedResource changedResource = new ChangedResource();
        changedResource.setResourceUri(resourceUri);
        changedResource.event(event);
        changedResource.setResourceKind("company-insolvency");
        changedResource.setContextId(contextId);

        return changedResource;
    }

}
