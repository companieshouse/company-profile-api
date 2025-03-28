package uk.gov.companieshouse.company.profile.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;
import uk.gov.companieshouse.api.http.HttpClient;

@Component
public class ApiClientServiceImpl implements ApiClientService {

    private final String chsApiKey;
    private final String internalApiUrl;

    public ApiClientServiceImpl(@Value("${chs.kafka.api.key}") String chsApiKey,
            @Value("${chs.kafka.api.endpoint}") String internalApiUrl) {
        this.chsApiKey = chsApiKey;
        this.internalApiUrl = internalApiUrl;
    }


    @Override
    public InternalApiClient getInternalApiClient() {
        var internalApiClient = new InternalApiClient(getHttpClient());
        internalApiClient.setInternalBasePath(internalApiUrl);
        internalApiClient.setBasePath(internalApiUrl);

        return internalApiClient;
    }

    private HttpClient getHttpClient() {
        return new ApiKeyHttpClient(chsApiKey);
    }
}
