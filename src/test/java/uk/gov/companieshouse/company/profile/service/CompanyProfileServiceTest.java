package uk.gov.companieshouse.company.profile.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import com.google.gson.Gson;
import com.mongodb.client.result.UpdateResult;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.company.Links;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.profile.api.CompanyProfileApiService;
import uk.gov.companieshouse.company.profile.exceptions.BadRequestException;
import uk.gov.companieshouse.company.profile.exceptions.DocumentNotFoundException;
import uk.gov.companieshouse.company.profile.exceptions.ResourceStateConflictException;
import uk.gov.companieshouse.company.profile.exceptions.ServiceUnavailableException;
import uk.gov.companieshouse.company.profile.model.CompanyProfileDocument;
import uk.gov.companieshouse.company.profile.model.Updated;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;
import uk.gov.companieshouse.logging.Logger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyProfileServiceTest {
    private static final String MOCK_COMPANY_NUMBER = "6146287";
    private static final String MOCK_CONTEXT_ID = "123456";

    @Mock
    CompanyProfileRepository companyProfileRepository;

    @Mock
    MongoTemplate mongoTemplate;

    @Mock
    Logger logger;

    @Mock
    ApiResponse<Void> apiResponse;

    @Mock
    CompanyProfileApiService companyProfileApiService;

    @Mock
    private CompanyProfileDocument document;

    @Mock
    private UpdateResult updateResult;

    @InjectMocks
    CompanyProfileService companyProfileService;

    private Gson gson = new Gson();

    @Test
    @DisplayName("When company profile is retrieved successfully then it is returned")
    void getCompanyProfile() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        LocalDateTime localDateTime = LocalDateTime.now();
        Updated updated = mock(Updated.class);
        CompanyProfileDocument mockCompanyProfileDocument = new CompanyProfileDocument(companyData, localDateTime, updated, false);
        mockCompanyProfileDocument.setId(MOCK_COMPANY_NUMBER);

        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.of(mockCompanyProfileDocument));

        Optional<CompanyProfileDocument> companyProfileActual =
                companyProfileService.get(MOCK_COMPANY_NUMBER);

        assertThat(companyProfileActual).containsSame(mockCompanyProfileDocument);
        verify(logger, times(2)).trace(anyString());
    }

    @Test
    @DisplayName("When no company profile is retrieved then return empty optional")
    void getNoCompanyProfileReturned() {
        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.empty());

        Optional<CompanyProfileDocument> companyProfileActual =
                companyProfileService.get(MOCK_COMPANY_NUMBER);

        assertTrue(companyProfileActual.isEmpty());
        verify(logger, times(2)).trace(anyString());
    }

    @Test
    @DisplayName("When there's a connection issue while performing the GET request then throw a "
            + "service unavailable exception")
    void getConnectionIssueServiceUnavailable() {
        when(companyProfileRepository.findById(anyString()))
                .thenThrow(new DataAccessResourceFailureException("Connection broken"));

        Assert.assertThrows(ServiceUnavailableException.class,
                () -> companyProfileService.get(MOCK_COMPANY_NUMBER));
        verify(logger, times(1)).trace(anyString());
    }

    @Test
    @DisplayName("When an illegal argument exception is thrown while performing the GET request then throw a "
            + "bad request exception")
    void getInvalidBadRequest() {
        when(companyProfileRepository.findById(anyString()))
                .thenThrow(new IllegalArgumentException());

        Assert.assertThrows(BadRequestException.class,
                () -> companyProfileService.get(MOCK_COMPANY_NUMBER));
        verify(logger, times(1)).trace(anyString());
    }

    @Test
    void when_insolvency_data_is_given_then_data_should_be_saved() throws Exception {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        LocalDateTime localDateTime = LocalDateTime.now();
        Updated updated = new Updated(localDateTime,
                null, "company-profile");

        CompanyProfileDocument mockCompanyProfileDocument = new CompanyProfileDocument(companyData, localDateTime, updated, false);
        mockCompanyProfileDocument.setId(MOCK_COMPANY_NUMBER);

        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.of(mockCompanyProfileDocument));
        when(apiResponse.getStatusCode()).thenReturn(200);
        when(companyProfileApiService.invokeChsKafkaApi(anyString(), anyString())).thenReturn(apiResponse);

        CompanyProfile companyProfileWithInsolvency = mockCompanyProfileWithoutInsolvency();
        companyProfileWithInsolvency.getData().getLinks().setInsolvency("INSOLVENCY_LINK");

        companyProfileService.updateInsolvencyLink(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER,
                companyProfileWithInsolvency);

        verify(mongoTemplate).upsert(any(Query.class), any(Update.class), any(Class.class));
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("When there's a connection issue while performing the PATCH request then throw a "
            + "service unavailable exception")
    void patchConnectionIssueServiceUnavailable() throws ApiErrorResponseException {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        LocalDateTime localDateTime = LocalDateTime.now();
        Updated updated = mock(Updated.class);

        CompanyProfileDocument mockCompanyProfileDocument = new CompanyProfileDocument(companyData, localDateTime, updated, false);
        mockCompanyProfileDocument.setId(MOCK_COMPANY_NUMBER);

        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.of(mockCompanyProfileDocument));

        when(apiResponse.getStatusCode()).thenReturn(200);
        when(companyProfileApiService.invokeChsKafkaApi(anyString(), anyString())).thenReturn(apiResponse);

        CompanyProfile companyProfile = mockCompanyProfileWithoutInsolvency();
        CompanyProfile companyProfileWithInsolvency = companyProfile;
        companyProfileWithInsolvency.getData().getLinks().setInsolvency("INSOLVENCY_LINK");

        when(mongoTemplate.upsert(any(Query.class), any(Update.class), any(Class.class))).thenThrow(new DataAccessResourceFailureException("Connection broken"));
        Assert.assertThrows(ServiceUnavailableException.class,
                () -> companyProfileService.updateInsolvencyLink(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER,
                        companyProfileWithInsolvency));
    }

    @Test
    @DisplayName("When company profile does not exist while performing the PATCH request then throw a "
            + "DocumentNotFoundException")
    void patchDocumentNotfound() throws ApiErrorResponseException {
        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.empty());

        CompanyProfile companyProfileWithInsolvency = mockCompanyProfileWithoutInsolvency();
        companyProfileWithInsolvency.getData().getLinks().setInsolvency("INSOLVENCY_LINK");

        Assert.assertThrows(DocumentNotFoundException.class,
                () -> companyProfileService.updateInsolvencyLink(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER,
                        companyProfileWithInsolvency));

        verify(apiResponse, never()).getStatusCode();
        verify(companyProfileApiService, never()).invokeChsKafkaApi(anyString(), anyString());
        verify(companyProfileRepository, never()).save(any());
        verify(companyProfileRepository, times(1)).findById(anyString());
    }

    @Test
    @DisplayName("Add exemptions link successfully updates MongoDB and calls chs-kafka-api")
    void addExemptionsLink() throws ApiErrorResponseException {
        // given
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(mongoTemplate.updateFirst(any(), any(), eq(CompanyProfileDocument.class))).thenReturn(updateResult);
        when(updateResult.getMatchedCount()).thenReturn(1L);

        // when
        companyProfileService.addExemptionsLink(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add exemptions link throws document not found exception")
    void addExemptionsLinkNotFound() {
        // given
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.addExemptionsLink(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format("No company profile with company number %s found", MOCK_COMPANY_NUMBER), exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add exemptions link throws resource state conflict exception")
    void addExemptionsLinkConflict() {
        // given
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(mongoTemplate.updateFirst(any(), any(), eq(CompanyProfileDocument.class))).thenReturn(updateResult);
        when(updateResult.getMatchedCount()).thenReturn(0L);

        // when
        Executable executable = () -> companyProfileService.addExemptionsLink(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);

        // then
        Exception exception = assertThrows(ResourceStateConflictException.class, executable);
        assertEquals("Resource state conflict; exemptions link already exists", exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add exemptions link throws service unavailable exception when illegal argument exception caught")
    void addExemptionsLinkIllegalArgument() throws ApiErrorResponseException {
        // given
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(mongoTemplate.updateFirst(any(), any(), eq(CompanyProfileDocument.class))).thenReturn(updateResult);
        when(updateResult.getMatchedCount()).thenReturn(1L);
        when(companyProfileApiService.invokeChsKafkaApi(any(), any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.addExemptionsLink(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add exemptions link throws service unavailable exception when data access exception thrown during findById")
    void addExemptionsLinkDataAccessExceptionFindById() {
        // given
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.addExemptionsLink(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add exemptions link throws service unavailable exception when data access exception thrown during update")
    void addExemptionsLinkDataAccessExceptionUpdate() {
        // given
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(mongoTemplate.updateFirst(any(), any(), eq(CompanyProfileDocument.class))).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.addExemptionsLink(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
    }

    private CompanyProfile mockCompanyProfileWithoutInsolvency() {
        CompanyProfile companyProfile = new CompanyProfile();
        Data data = new Data();
        data.setCompanyNumber(MOCK_COMPANY_NUMBER);

        Links links = new Links();
        links.setOfficers("officer");

        data.setLinks(links);
        companyProfile.setData(data);
        return companyProfile;
    }

    private CompanyProfileDocument generateCompanyProfileDocument(CompanyProfile companyProfile) {
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        CompanyProfileDocument companyProfileDocument =
                new CompanyProfileDocument(companyProfile.getData(), localDateTime, new Updated(localDateTime, null, "company-profile"), false);
        companyProfileDocument.setId(companyProfile.getData().getCompanyNumber());
        return companyProfileDocument;
    }

    private String expectedFindQuery(String companyNumber) {
        return String.format("{\"data.company_number\": \"%s\"}", companyNumber);
    }

    private String expectedUpdateQuery(String insolvencyLink) {
        return String.format("{\"$set\": {\"data.links.insolvency\": \"%s\", \"data.etag\": \"",
                insolvencyLink);
    }
}