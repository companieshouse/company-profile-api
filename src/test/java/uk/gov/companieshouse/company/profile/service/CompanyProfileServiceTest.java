package uk.gov.companieshouse.company.profile.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import com.google.gson.Gson;
import com.mongodb.client.result.UpdateResult;
import io.cucumber.java.BeforeAll;
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
import uk.gov.companieshouse.company.profile.transform.CompanyProfileTransformer;
import uk.gov.companieshouse.company.profile.util.LinkRequest;
import uk.gov.companieshouse.company.profile.util.LinkRequestFactory;
import uk.gov.companieshouse.company.profile.util.TestHelper;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.EXEMPTIONS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.EXEMPTIONS_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.OFFICERS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.OFFICERS_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_STATEMENTS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_STATEMENTS_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_LINK_TYPE;

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
    private Data data;

    @Mock
    private Links links;

    @Mock
    private UpdateResult updateResult;

    @Mock
    private LinkRequest linkRequest;

    @Mock
    private LinkRequestFactory linkRequestFactory;

    @Mock
    private CompanyProfileTransformer companyProfileTransformer;

    @InjectMocks
    CompanyProfileService companyProfileService;

    private Gson gson = new Gson();

    private TestHelper testHelper;

    private static CompanyProfile COMPANY_PROFILE;

    private static CompanyProfileDocument COMPANY_PROFILE_DOCUMENT;

    @BeforeAll
    void setUp() throws IOException {
        testHelper = new TestHelper();
        COMPANY_PROFILE = testHelper.createCompanyProfileObject();
        COMPANY_PROFILE_DOCUMENT = testHelper.createCompanyProfileDocument();
    }

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
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE, Links::getExemptions);
        when(linkRequestFactory.createLinkRequest(EXEMPTIONS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add exemptions link throws document not found exception")
    void addExemptionsLinkNotFound() {
        // given
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE, Links::getExemptions);
        when(linkRequestFactory.createLinkRequest(EXEMPTIONS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format("No company profile with company number %s found", MOCK_COMPANY_NUMBER), exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Add exemptions link throws resource state conflict exception")
    void addExemptionsLinkConflict() throws ApiErrorResponseException {
        // given
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE, Links::getExemptions);
        when(linkRequestFactory.createLinkRequest(EXEMPTIONS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getExemptions()).thenReturn(String.format("/company/%s/exemptions", MOCK_COMPANY_NUMBER));

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        Exception exception = assertThrows(ResourceStateConflictException.class, executable);
        assertEquals("Resource state conflict; exemptions link already exists", exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Add exemptions link throws service unavailable exception when illegal argument exception caught")
    void addExemptionsLinkIllegalArgument() throws ApiErrorResponseException {
        // given
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE, Links::getExemptions);
        when(linkRequestFactory.createLinkRequest(EXEMPTIONS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(companyProfileApiService.invokeChsKafkaApi(any(), any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add exemptions link throws service unavailable exception when data access exception thrown during findById")
    void addExemptionsLinkDataAccessExceptionFindById() throws ApiErrorResponseException {
        // given
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE, Links::getExemptions);
        when(linkRequestFactory.createLinkRequest(EXEMPTIONS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Add exemptions link throws service unavailable exception when data access exception thrown during update")
    void addExemptionsLinkDataAccessExceptionUpdate() throws ApiErrorResponseException {
        // given
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE, Links::getExemptions);
        when(linkRequestFactory.createLinkRequest(EXEMPTIONS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(mongoTemplate.updateFirst(any(), any(), eq(CompanyProfileDocument.class))).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
    }

    @Test
    @DisplayName("Delete exemptions link successfully updates MongoDB and calls chs-kafka-api")
    void deleteExemptionsLink() throws ApiErrorResponseException {
        // given
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE, Links::getExemptions);
        when(linkRequestFactory.createLinkRequest(EXEMPTIONS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getExemptions()).thenReturn(String.format("/company/%s/exemptions", MOCK_COMPANY_NUMBER));

        // when
        companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete exemptions link throws document not found exception")
    void deleteExemptionsLinkNotFound() {
        // given
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE, Links::getExemptions);
        when(linkRequestFactory.createLinkRequest(EXEMPTIONS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format("No company profile with company number %s found", MOCK_COMPANY_NUMBER), exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete exemptions link throws resource state conflict exception")
    void deleteExemptionsLinkConflict() throws ApiErrorResponseException {
        // given
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE, Links::getExemptions);
        when(linkRequestFactory.createLinkRequest(EXEMPTIONS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        Exception exception = assertThrows(ResourceStateConflictException.class, executable);
        assertEquals("Resource state conflict; exemptions link already does not exist", exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete exemptions link throws service unavailable exception when illegal argument exception caught")
    void deleteExemptionsLinkIllegalArgument() throws ApiErrorResponseException {
        // given
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE, Links::getExemptions);
        when(linkRequestFactory.createLinkRequest(EXEMPTIONS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getExemptions()).thenReturn(String.format("/company/%s/exemptions", MOCK_COMPANY_NUMBER));
        when(companyProfileApiService.invokeChsKafkaApi(any(), any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete exemptions link throws service unavailable exception when data access exception thrown during findById")
    void deleteExemptionsLinkDataAccessExceptionFindById() throws ApiErrorResponseException {
        // given
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE, Links::getExemptions);
        when(linkRequestFactory.createLinkRequest(EXEMPTIONS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete exemptions link throws service unavailable exception when data access exception thrown during update")
    void deleteExemptionsLinkDataAccessExceptionUpdate() throws ApiErrorResponseException {
        // given
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE, Links::getExemptions);
        when(linkRequestFactory.createLinkRequest(EXEMPTIONS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getExemptions()).thenReturn(String.format("/company/%s/exemptions", MOCK_COMPANY_NUMBER));
        when(mongoTemplate.updateFirst(any(), any(), eq(CompanyProfileDocument.class))).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
    }

    @Test
    @DisplayName("Add officers link successfully updates MongoDB and calls chs-kafka-api")
    void addOfficersLink() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE, Links::getOfficers);
        when(linkRequestFactory.createLinkRequest(OFFICERS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add officers link throws document not found exception")
    void addOfficersLinkNotFound() {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE, Links::getOfficers);
        when(linkRequestFactory.createLinkRequest(OFFICERS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format("No company profile with company number %s found", MOCK_COMPANY_NUMBER), exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Add officers link throws resource state conflict exception")
    void addOfficersLinkConflict() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE, Links::getOfficers);
        when(linkRequestFactory.createLinkRequest(OFFICERS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getOfficers()).thenReturn(String.format("/company/%s/officers", MOCK_COMPANY_NUMBER));

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        Exception exception = assertThrows(ResourceStateConflictException.class, executable);
        assertEquals("Resource state conflict; officers link already exists", exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Add officers link throws service unavailable exception when illegal argument exception caught")
    void addOfficersLinkIllegalArgument() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE, Links::getOfficers);
        when(linkRequestFactory.createLinkRequest(OFFICERS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(companyProfileApiService.invokeChsKafkaApi(any(), any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add officer link throws service unavailable exception when data access exception thrown during findById")
    void addOfficersLinkDataAccessExceptionFindById() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE, Links::getOfficers);
        when(linkRequestFactory.createLinkRequest(OFFICERS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Add officers link throws service unavailable exception when data access exception thrown during update")
    void addOfficersLinkDataAccessExceptionUpdate() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE, Links::getOfficers);
        when(linkRequestFactory.createLinkRequest(OFFICERS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(mongoTemplate.updateFirst(any(), any(), eq(CompanyProfileDocument.class))).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
    }


    @Test
    @DisplayName("Delete officers link successfully updates MongoDB and calls chs-kafka-api")
    void deleteOfficersLink() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE, Links::getOfficers);
        when(linkRequestFactory.createLinkRequest(OFFICERS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getOfficers()).thenReturn(String.format("/company/%s/officers", MOCK_COMPANY_NUMBER));

        // when
        companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete officers link throws document not found exception")
    void deleteOfficersLinkNotFound() {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE, Links::getOfficers);
        when(linkRequestFactory.createLinkRequest(OFFICERS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format("No company profile with company number %s found", MOCK_COMPANY_NUMBER), exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete officers link throws resource state conflict exception")
    void deleteOfficersLinkConflict() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE, Links::getOfficers);
        when(linkRequestFactory.createLinkRequest(OFFICERS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        Exception exception = assertThrows(ResourceStateConflictException.class, executable);
        assertEquals("Resource state conflict; officers link already does not exist", exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete officers link throws service unavailable exception when illegal argument exception caught")
    void deleteOfficersLinkIllegalArgument() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE, Links::getOfficers);
        when(linkRequestFactory.createLinkRequest(OFFICERS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getOfficers()).thenReturn(String.format("/company/%s/officers", MOCK_COMPANY_NUMBER));
        when(companyProfileApiService.invokeChsKafkaApi(any(), any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete officers link throws service unavailable exception when data access exception thrown during findById")
    void deleteOfficersLinkDataAccessExceptionFindById() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE, Links::getOfficers);
        when(linkRequestFactory.createLinkRequest(OFFICERS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete officers link throws service unavailable exception when data access exception thrown during update")
    void deleteOfficersLinkDataAccessExceptionUpdate() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE, Links::getOfficers);
        when(linkRequestFactory.createLinkRequest(OFFICERS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getOfficers()).thenReturn(String.format("/company/%s/officers", MOCK_COMPANY_NUMBER));
        when(mongoTemplate.updateFirst(any(), any(), eq(CompanyProfileDocument.class))).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
    }

    @Test
    @DisplayName("Add psc statements link successfully updates MongoDB and calls chs-kafka-api")
    void addPscStatementsLink() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE,
                Links::getPersonsWithSignificantControlStatements);
        when(linkRequestFactory.createLinkRequest(PSC_STATEMENTS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add psc statements link throws document not found exception")
    void addPscStatementsLinkNotFound() {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE,
                Links::getPersonsWithSignificantControlStatements);
        when(linkRequestFactory.createLinkRequest(PSC_STATEMENTS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format("No company profile with company number %s found", MOCK_COMPANY_NUMBER), exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Add psc statements link throws resource state conflict exception")
    void addPscStatementsLinkConflict() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE,
                Links::getPersonsWithSignificantControlStatements);
        when(linkRequestFactory.createLinkRequest(PSC_STATEMENTS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getPersonsWithSignificantControlStatements()).thenReturn(String.format(
                "/company/%s/persons-with-significant-control-statements", MOCK_COMPANY_NUMBER));

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);


        // then
        Exception exception = assertThrows(ResourceStateConflictException.class, executable);
        assertEquals("Resource state conflict; persons-with-significant-control-statements" +
                " link already exists", exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Add psc statements link throws service unavailable exception when illegal argument exception caught")
    void addPscStatementsLinkIllegalArgument() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE,
                Links::getPersonsWithSignificantControlStatements);
        when(linkRequestFactory.createLinkRequest(PSC_STATEMENTS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(companyProfileApiService.invokeChsKafkaApi(any(), any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add psc statements link throws service unavailable exception when data access exception thrown during findById")
    void addPscStatementsLinkDataAccessExceptionFindById() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE,
                Links::getPersonsWithSignificantControlStatements);
        when(linkRequestFactory.createLinkRequest(PSC_STATEMENTS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Add psc statements link throws service unavailable exception when data access exception thrown during update")
    void addPscStatementsLinkDataAccessExceptionUpdate() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE,
                Links::getPersonsWithSignificantControlStatements);
        when(linkRequestFactory.createLinkRequest(PSC_STATEMENTS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(mongoTemplate.updateFirst(any(), any(), eq(CompanyProfileDocument.class))).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
    }

    @Test
    @DisplayName("Delete psc statements link successfully updates MongoDB and calls chs-kafka-api")
    void deletePscStatementsLink() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE,
                Links::getPersonsWithSignificantControlStatements);
        when(linkRequestFactory.createLinkRequest(PSC_STATEMENTS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getPersonsWithSignificantControlStatements()).thenReturn(String.format(
                "/company/%s/persons-with-significant-control-statements", MOCK_COMPANY_NUMBER));

        // when
        companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);
        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete psc statements link throws document not found exception")
    void deletePscStatementsLinkNotFound() {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE,
                Links::getPersonsWithSignificantControlStatements);
        when(linkRequestFactory.createLinkRequest(PSC_STATEMENTS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format("No company profile with company number %s found", MOCK_COMPANY_NUMBER), exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete psc statements link throws resource state conflict exception")
    void deletePscStatementsLinkConflict() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE,
                Links::getPersonsWithSignificantControlStatements);
        when(linkRequestFactory.createLinkRequest(PSC_STATEMENTS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        Exception exception = assertThrows(ResourceStateConflictException.class, executable);
        assertEquals("Resource state conflict; persons-with-significant-control-statements" +
                " link already does not exist", exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete psc statements link throws service unavailable exception when illegal argument exception caught")
    void deletePscStatementsLinkIllegalArgument() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE,
                Links::getPersonsWithSignificantControlStatements);
        when(linkRequestFactory.createLinkRequest(PSC_STATEMENTS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getPersonsWithSignificantControlStatements()).thenReturn(String.format(
                "/company/%s/persons-with-significant-control-statements", MOCK_COMPANY_NUMBER));
        when(companyProfileApiService.invokeChsKafkaApi(any(), any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete psc statements link throws service unavailable exception when data access exception thrown during findById")
    void deletePscStatementsLinkDataAccessExceptionFindById() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE,
                Links::getPersonsWithSignificantControlStatements);
        when(linkRequestFactory.createLinkRequest(PSC_STATEMENTS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete psc statements link throws service unavailable exception when data access exception thrown during update")
    void deletePscStatementsLinkDataAccessExceptionUpdate() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE,
                Links::getPersonsWithSignificantControlStatements);
        when(linkRequestFactory.createLinkRequest(PSC_STATEMENTS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getPersonsWithSignificantControlStatements()).thenReturn(String.format(
                "/company/%s/persons-with-significant-control-statements", MOCK_COMPANY_NUMBER));
        when(mongoTemplate.updateFirst(any(), any(), eq(CompanyProfileDocument.class))).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
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

    @Test
    @DisplayName("Add psc link successfully updates MongoDB and calls chs-kafka-api")
    void addPscLink() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_LINK_TYPE, PSC_DELTA_TYPE,
                Links::getPersonsWithSignificantControl);
        when(linkRequestFactory.createLinkRequest(PSC_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add psc link throws document not found exception")
    void addPscLinkNotFound() {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_LINK_TYPE, PSC_DELTA_TYPE,
                Links::getPersonsWithSignificantControl);
        when(linkRequestFactory.createLinkRequest(PSC_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format("No company profile with company number %s found", MOCK_COMPANY_NUMBER), exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Add psc link throws resource state conflict exception")
    void addPscLinkConflict() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_LINK_TYPE, PSC_DELTA_TYPE,
                Links::getPersonsWithSignificantControl);
        when(linkRequestFactory.createLinkRequest(PSC_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getPersonsWithSignificantControl()).thenReturn(String.format(
                "/company/%s/persons-with-significant-control", MOCK_COMPANY_NUMBER));

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);


        // then
        Exception exception = assertThrows(ResourceStateConflictException.class, executable);
        assertEquals("Resource state conflict; persons-with-significant-control" +
                " link already exists", exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Add psc link throws service unavailable exception when illegal argument exception caught")
    void addPscLinkIllegalArgument() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_LINK_TYPE, PSC_DELTA_TYPE,
                Links::getPersonsWithSignificantControl);
        when(linkRequestFactory.createLinkRequest(PSC_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(companyProfileApiService.invokeChsKafkaApi(any(), any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add psc link throws service unavailable exception when data access exception thrown during findById")
    void addPscLinkDataAccessExceptionFindById() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_LINK_TYPE, PSC_DELTA_TYPE,
                Links::getPersonsWithSignificantControl);
        when(linkRequestFactory.createLinkRequest(PSC_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Add psc link throws service unavailable exception when data access exception thrown during update")
    void addPscLinkDataAccessExceptionUpdate() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_LINK_TYPE, PSC_DELTA_TYPE,
                Links::getPersonsWithSignificantControl);
        when(linkRequestFactory.createLinkRequest(PSC_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(mongoTemplate.updateFirst(any(), any(), eq(CompanyProfileDocument.class))).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
    }

    @Test
    @DisplayName("Delete psc link successfully updates MongoDB and calls chs-kafka-api")
    void deletePscLink() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_LINK_TYPE, PSC_DELTA_TYPE,
                Links::getPersonsWithSignificantControl);
        when(linkRequestFactory.createLinkRequest(PSC_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getPersonsWithSignificantControl()).thenReturn(String.format(
                "/company/%s/persons-with-significant-control", MOCK_COMPANY_NUMBER));

        // when
        companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);
        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete psc link throws document not found exception")
    void deletePscLinkNotFound() {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_LINK_TYPE, PSC_DELTA_TYPE,
                Links::getPersonsWithSignificantControl);
        when(linkRequestFactory.createLinkRequest(PSC_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format("No company profile with company number %s found", MOCK_COMPANY_NUMBER), exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete psc link throws resource state conflict exception")
    void deletePscLinkConflict() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_LINK_TYPE, PSC_DELTA_TYPE,
                Links::getPersonsWithSignificantControl);
        when(linkRequestFactory.createLinkRequest(PSC_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        Exception exception = assertThrows(ResourceStateConflictException.class, executable);
        assertEquals("Resource state conflict; persons-with-significant-control" +
                " link already does not exist", exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete psc link throws service unavailable exception when illegal argument exception caught")
    void deletePscLinkIllegalArgument() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_LINK_TYPE, PSC_DELTA_TYPE,
                Links::getPersonsWithSignificantControl);
        when(linkRequestFactory.createLinkRequest(PSC_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getPersonsWithSignificantControl()).thenReturn(String.format(
                "/company/%s/persons-with-significant-control", MOCK_COMPANY_NUMBER));
        when(companyProfileApiService.invokeChsKafkaApi(any(), any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete psc link throws service unavailable exception when data access exception thrown during findById")
    void deletePscLinkDataAccessExceptionFindById() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_LINK_TYPE, PSC_DELTA_TYPE,
                Links::getPersonsWithSignificantControl);
        when(linkRequestFactory.createLinkRequest(PSC_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete psc link throws service unavailable exception when data access exception thrown during update")
    void deletePscLinkDataAccessExceptionUpdate() throws ApiErrorResponseException {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_LINK_TYPE, PSC_DELTA_TYPE,
                Links::getPersonsWithSignificantControl);
        when(linkRequestFactory.createLinkRequest(PSC_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getPersonsWithSignificantControl()).thenReturn(String.format(
                "/company/%s/persons-with-significant-control", MOCK_COMPANY_NUMBER));
        when(mongoTemplate.updateFirst(any(), any(), eq(CompanyProfileDocument.class))).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
    }

    @Test
    @DisplayName("put company profile")
    void putCompanyProfileSuccessfully() throws IOException {
        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenReturn(Optional.empty());
        when(companyProfileTransformer.transform(COMPANY_PROFILE, MOCK_COMPANY_NUMBER, Optional.empty()))
                .thenReturn(COMPANY_PROFILE_DOCUMENT);
        companyProfileService.processCompanyProfile(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER,
                COMPANY_PROFILE);
        verify(companyProfileRepository).save(COMPANY_PROFILE_DOCUMENT);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
    }


}