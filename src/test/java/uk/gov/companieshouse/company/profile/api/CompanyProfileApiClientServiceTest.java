package uk.gov.companieshouse.company.profile.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.exception.ServiceUnavailableException;
import uk.gov.companieshouse.api.handler.chskafka.PrivateChangedResourceHandler;
import uk.gov.companieshouse.api.handler.chskafka.request.PrivateChangedResourcePost;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.profile.exception.SerDesException;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
class CompanyProfileApiClientServiceTest {

    private static final Data companyData = new Data();
    private static final Object mappedData = new Data();

    @Mock
    private ApiClientService apiClientService;

    @Mock
    private InternalApiClient internalApiClient;

    @Mock
    private PrivateChangedResourceHandler privateChangedResourceHandler;

    @Mock
    private PrivateChangedResourcePost changedResourcePost;

    @Mock
    private ApiResponse<Void> response;

    @Mock
    private Logger logger;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CompanyProfileApiService companyProfileApiService;

    @Test
    void should_invoke_chs_kafka_endpoint_successfully() throws ApiErrorResponseException {

        when(apiClientService.getInternalApiClient()).thenReturn(internalApiClient);
        when(internalApiClient.privateChangedResourceHandler()).thenReturn(privateChangedResourceHandler);
        when(privateChangedResourceHandler.postChangedResource(any(), any())).thenReturn(changedResourcePost);
        when(changedResourcePost.execute()).thenReturn(response);

        ApiResponse<Void> apiResponse = companyProfileApiService.invokeChsKafkaApi("123456", "CH4000056");

        Assertions.assertThat(apiResponse).isNotNull();

        verify(apiClientService, times(1)).getInternalApiClient();
        verify(internalApiClient, times(1)).privateChangedResourceHandler();
        verify(privateChangedResourceHandler, times(1)).postChangedResource(any(), any());
        verify(changedResourcePost, times(1)).execute();
    }

    @Test
    void should_invoke_delete_chs_kafka_endpoint_successfully() throws ApiErrorResponseException, JsonProcessingException {

        when(apiClientService.getInternalApiClient()).thenReturn(internalApiClient);
        when(internalApiClient.privateChangedResourceHandler()).thenReturn(privateChangedResourceHandler);
        when(objectMapper.writeValueAsString(any())).thenReturn("serialisedData");
        when(objectMapper.readValue(anyString(), eq(Object.class))).thenReturn(mappedData);
        when(privateChangedResourceHandler.postChangedResource(any(), any())).thenReturn(changedResourcePost);
        when(changedResourcePost.execute()).thenReturn(response);

        ApiResponse<Void> apiResponse = companyProfileApiService.invokeChsKafkaApiWithDeleteEvent("123456", "CH4000056",
                companyData);

        Assertions.assertThat(apiResponse).isNotNull();

        verify(apiClientService, times(1)).getInternalApiClient();
        verify(internalApiClient, times(1)).privateChangedResourceHandler();
        verify(objectMapper).writeValueAsString(companyData);
        verify(objectMapper).readValue("serialisedData", Object.class);
        verify(privateChangedResourceHandler, times(1)).postChangedResource(any(), any());
        verify(changedResourcePost, times(1)).execute();
    }

    @ParameterizedTest
    @MethodSource("provideExceptionParameters")
    void should_handle_exception_when_chs_kafka_endpoint_throws_appropriate_exception(int statusCode, String statusMessage)
            throws ApiErrorResponseException {
        when(apiClientService.getInternalApiClient()).thenReturn(internalApiClient);
        when(internalApiClient.privateChangedResourceHandler()).thenReturn(privateChangedResourceHandler);
        when(privateChangedResourceHandler.postChangedResource(any(), any())).thenReturn(changedResourcePost);

        HttpResponseException.Builder builder =
                new HttpResponseException.Builder(statusCode, statusMessage, new HttpHeaders());
        ApiErrorResponseException apiErrorResponseException =
                new ApiErrorResponseException(builder);
        when(changedResourcePost.execute()).thenThrow(apiErrorResponseException);

        Assert.assertThrows(ServiceUnavailableException.class,
                () -> companyProfileApiService.invokeChsKafkaApi("3245435", "CH4000056"));

        verify(apiClientService, times(1)).getInternalApiClient();
        verify(internalApiClient, times(1)).privateChangedResourceHandler();
        verify(privateChangedResourceHandler, times(1)).postChangedResource(any(),
                any());
        verify(changedResourcePost, times(1)).execute();
    }

    @ParameterizedTest
    @MethodSource("provideExceptionParameters")
    void should_handle_exception_when_chs_kafka_delete_endpoint_throws_appropriate_exception(int statusCode, String statusMessage)
            throws ApiErrorResponseException {
        HttpResponseException.Builder builder =
                new HttpResponseException.Builder(statusCode, statusMessage, new HttpHeaders());
        ApiErrorResponseException apiErrorResponseException =
                new ApiErrorResponseException(builder);

        when(apiClientService.getInternalApiClient()).thenReturn(internalApiClient);
        when(internalApiClient.privateChangedResourceHandler()).thenReturn(privateChangedResourceHandler);
        when(privateChangedResourceHandler.postChangedResource(any(), any())).thenReturn(changedResourcePost);
        when(changedResourcePost.execute()).thenThrow(apiErrorResponseException);

        Assert.assertThrows(ServiceUnavailableException.class,
                () -> companyProfileApiService.invokeChsKafkaApiWithDeleteEvent("3245435", "CH4000056", new Data()));

        verify(apiClientService, times(1)).getInternalApiClient();
        verify(internalApiClient, times(1)).privateChangedResourceHandler();
        verify(privateChangedResourceHandler, times(1)).postChangedResource(any(),
                any());
        verify(changedResourcePost, times(1)).execute();
    }

    @Test
    void should_handle_exception_when_object_mapper_read_throws_exception() throws JsonProcessingException {
        when(apiClientService.getInternalApiClient()).thenReturn(internalApiClient);
        when(internalApiClient.privateChangedResourceHandler()).thenReturn(privateChangedResourceHandler);
        when(objectMapper.writeValueAsString(any())).thenReturn("serialisedData");
        when(objectMapper.readValue(anyString(), eq(Object.class))).thenThrow(JsonProcessingException.class);

        Assert.assertThrows(SerDesException.class,
                () -> companyProfileApiService.invokeChsKafkaApiWithDeleteEvent("3245435", "CH4000056", companyData));

        verify(apiClientService).getInternalApiClient();
        verify(internalApiClient).privateChangedResourceHandler();
        verify(objectMapper).writeValueAsString(companyData);
        verify(objectMapper).readValue("serialisedData", Object.class);
        verifyNoInteractions(privateChangedResourceHandler);
        verifyNoInteractions(changedResourcePost);
    }

    @Test
    void should_handle_exception_when_object_mapper_write_throws_exception() throws JsonProcessingException {
        when(apiClientService.getInternalApiClient()).thenReturn(internalApiClient);
        when(internalApiClient.privateChangedResourceHandler()).thenReturn(privateChangedResourceHandler);
        when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);

        Assert.assertThrows(SerDesException.class,
                () -> companyProfileApiService.invokeChsKafkaApiWithDeleteEvent("3245435", "CH4000056", new Data()));

        verify(apiClientService).getInternalApiClient();
        verify(internalApiClient).privateChangedResourceHandler();
        verify(objectMapper).writeValueAsString(companyData);
        verifyNoMoreInteractions(objectMapper);
        verifyNoInteractions(privateChangedResourceHandler);
        verifyNoInteractions(changedResourcePost);
    }

    private static Stream<Arguments> provideExceptionParameters() {
        return Stream.of(
                Arguments.of(503, "Service Unavailable", ServiceUnavailableException.class),
                Arguments.of(500, "Internal Service Error", ApiErrorResponseException.class)
        );
    }
}
