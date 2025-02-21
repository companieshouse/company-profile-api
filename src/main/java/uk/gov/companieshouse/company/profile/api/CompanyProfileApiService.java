package uk.gov.companieshouse.company.profile.api;

import static uk.gov.companieshouse.company.profile.CompanyProfileApiApplication.APPLICATION_NAME_SPACE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.chskafka.ChangedResourceEvent;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.exception.ServiceUnavailableException;
import uk.gov.companieshouse.api.handler.chskafka.request.PrivateChangedResourcePost;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.profile.exception.SerDesException;
import uk.gov.companieshouse.company.profile.logging.DataMapHolder;
import uk.gov.companieshouse.company.profile.util.DateUtils;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class CompanyProfileApiService {

    public static final String CHANGED_EVENT_TYPE = "changed";
    public static final String DELETED_EVENT_TYPE = "deleted";
    private static final String CHANGED_RESOURCE_URI = "/private/resource-changed";
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    private final ApiClientService apiClientService;
    private final ObjectMapper objectMapper;

    /**
     * Invoke Insolvency API.
     */
    public CompanyProfileApiService(ApiClientService apiClientService, ObjectMapper objectMapper) {
        this.apiClientService = apiClientService;
        this.objectMapper = objectMapper;
    }

    /**
     * Call chs-kafka api.
     *
     * @param companyNumber company insolvency number
     * @return response returned from chs-kafka api
     */
    public ApiResponse<Void> invokeChsKafkaApi(String contextId, String companyNumber) {
        InternalApiClient internalApiClient = apiClientService.getInternalApiClient();
        PrivateChangedResourcePost changedResourcePost = internalApiClient
                .privateChangedResourceHandler()
                .postChangedResource(CHANGED_RESOURCE_URI,
                        mapChangedResource(contextId, companyNumber, CHANGED_EVENT_TYPE, null));
        return handleApiCall(changedResourcePost);
    }

    /**
     * Invoke chs-kafka api with delete event.
     *
     * @param companyNumber  company insolvency number
     * @param contextId      context ID
     * @param companyProfile the company profile being deleted
     * @return response returned from chs-kafka api
     */
    public ApiResponse<Void> invokeChsKafkaApiWithDeleteEvent(String contextId,
            String companyNumber, Data companyProfile) {
        InternalApiClient internalApiClient = apiClientService.getInternalApiClient();
        PrivateChangedResourcePost changedResourcePost = internalApiClient
                .privateChangedResourceHandler()
                .postChangedResource(CHANGED_RESOURCE_URI,
                        mapChangedResource(contextId, companyNumber, DELETED_EVENT_TYPE,
                                companyProfile));
        return handleApiCall(changedResourcePost);
    }

    private ChangedResource mapChangedResource(String contextId, String companyNumber,
            String eventType, Data companyProfile) {
        ChangedResource changedResource = new ChangedResource();

        ChangedResourceEvent event = new ChangedResourceEvent();
        event.setType(eventType);
        if (eventType.equals(DELETED_EVENT_TYPE)) {
            try {
                Object dataAsObject = objectMapper.readValue(
                        objectMapper.writeValueAsString(companyProfile), Object.class
                );
                changedResource.setDeletedData(dataAsObject);
            } catch (JsonProcessingException ex) {
                final String msg = "Failed to serialise/deserialise deleted data";
                LOGGER.error(msg, DataMapHolder.getLogMap());
                throw new SerDesException(msg, ex);
            }
        }
        event.publishedAt(DateUtils.publishedAtString(Instant.now()));

        changedResource.setResourceUri("/company/" + companyNumber);
        changedResource.event(event);
        changedResource.setResourceKind("company-profile");
        changedResource.setContextId(contextId);

        return changedResource;
    }

    private ApiResponse<Void> handleApiCall(PrivateChangedResourcePost post) {
        try {
            return post.execute();
        } catch (ApiErrorResponseException exception) {
            LOGGER.error("Unsuccessful call to /private/resource-changed endpoint",
                    exception, DataMapHolder.getLogMap());
            throw new ServiceUnavailableException(exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.error("Error occurred while calling /private/resource-changed"
                    + " endpoint", exception, DataMapHolder.getLogMap());
            throw exception;
        }
    }
}
