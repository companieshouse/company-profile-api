package uk.gov.companieshouse.company.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import static uk.gov.companieshouse.company.profile.util.LinkRequest.CHARGES_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.CHARGES_GET;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.CHARGES_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.EXEMPTIONS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.EXEMPTIONS_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.FILING_HISTORY_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.FILING_HISTORY_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.INSOLVENCY_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.INSOLVENCY_GET;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.INSOLVENCY_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.OFFICERS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.OFFICERS_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_STATEMENTS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_STATEMENTS_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.UK_ESTABLISHMENTS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.UK_ESTABLISHMENTS_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.TestHelper.createExistingCompanyProfile;
import static uk.gov.companieshouse.company.profile.util.TestHelper.createExistingLinks;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.mongodb.client.result.UpdateResult;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import uk.gov.companieshouse.api.company.Accounts;
import uk.gov.companieshouse.api.company.AnnualReturn;
import uk.gov.companieshouse.api.company.BranchCompanyDetails;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.ConfirmationStatement;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.company.Links;
import uk.gov.companieshouse.api.company.NextAccounts;
import uk.gov.companieshouse.api.company.RegisteredOfficeAddress;
import uk.gov.companieshouse.api.company.UkEstablishment;
import uk.gov.companieshouse.api.company.UkEstablishmentsList;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.exception.BadRequestException;
import uk.gov.companieshouse.api.exception.DocumentNotFoundException;
import uk.gov.companieshouse.api.exception.ResourceStateConflictException;
import uk.gov.companieshouse.api.exception.ServiceUnavailableException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.CompanyProfileDocument;
import uk.gov.companieshouse.api.model.Updated;
import uk.gov.companieshouse.company.profile.api.CompanyProfileApiService;
import uk.gov.companieshouse.company.profile.exception.ResourceNotFoundException;
import uk.gov.companieshouse.company.profile.model.VersionedCompanyProfileDocument;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;
import uk.gov.companieshouse.company.profile.transform.CompanyProfileTransformer;
import uk.gov.companieshouse.company.profile.util.LinkRequest;
import uk.gov.companieshouse.company.profile.util.LinkRequestFactory;
import uk.gov.companieshouse.company.profile.util.TestHelper;
import uk.gov.companieshouse.logging.Logger;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CompanyProfileServiceTest {
    private static final String MOCK_COMPANY_NUMBER = "6146287";
    private static final String MOCK_CONTEXT_ID = "123456";
    private static final String MOCK_PARENT_COMPANY_NUMBER = "321033";
    private static final String ANOTHER_PARENT_COMPANY_NUMBER = "FC123456";

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
    private VersionedCompanyProfileDocument document;
    @Mock
    private Data data;
    @Mock
    private Links links;
    @Mock
    private UpdateResult updateResult;
    @Mock
    private LinkRequest linkRequest;
    @Mock
    ConfirmationStatement confirmationStatement;
    @Mock
    AnnualReturn annualReturn;
    @Mock
    Accounts accounts;
    @Mock
    NextAccounts nextAccounts;
    @Mock
    private LinkRequestFactory linkRequestFactory;
    @Mock
    private CompanyProfileTransformer companyProfileTransformer;

    @InjectMocks @Spy
    CompanyProfileService companyProfileService;

    private final Gson gson = new Gson();
    private static CompanyProfile COMPANY_PROFILE;
    private static VersionedCompanyProfileDocument COMPANY_PROFILE_DOCUMENT;
    private static Links EXISTING_LINKS;
    private static VersionedCompanyProfileDocument EXISTING_COMPANY_PROFILE_DOCUMENT;
    private static VersionedCompanyProfileDocument EXISTING_PARENT_COMPANY_PROFILE_DOCUMENT;
    private static List<VersionedCompanyProfileDocument> UK_ESTABLISHMENTS_TEST_INPUT;
    private static List<UkEstablishment> UK_ESTABLISHMENTS_TEST_OUTPUT;
    private static VersionedCompanyProfileDocument EXISTING_UK_ESTABLISHMENT_COMPANY;
    private static VersionedCompanyProfileDocument EXISTING_PARENT_COMPANY;

    private final LinkRequest chargesLinkRequest = new LinkRequest(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER,
            CHARGES_LINK_TYPE, CHARGES_DELTA_TYPE, CHARGES_GET);
    private final LinkRequest insolvencyLinkRequest = new LinkRequest(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER,
            INSOLVENCY_LINK_TYPE, INSOLVENCY_DELTA_TYPE, INSOLVENCY_GET);
    private final String CHARGES_LINK = String.format("/company/%s/charges", MOCK_COMPANY_NUMBER);
    private final String INSOLVENCY_LINK = String.format("/company/%s/insolvency", MOCK_COMPANY_NUMBER);

    @BeforeAll
    static void setUp() throws IOException {
        TestHelper testHelper = new TestHelper();
        COMPANY_PROFILE = testHelper.createCompanyProfileObject();
        COMPANY_PROFILE_DOCUMENT = testHelper.createCompanyProfileDocument();
        EXISTING_LINKS = createExistingLinks();
        EXISTING_COMPANY_PROFILE_DOCUMENT = createExistingCompanyProfile();
        EXISTING_PARENT_COMPANY_PROFILE_DOCUMENT = createExistingCompanyProfile();
        UK_ESTABLISHMENTS_TEST_INPUT = Arrays.asList(
                testHelper.createUkEstablishmentTestInput(MOCK_COMPANY_NUMBER + 1),
                testHelper.createUkEstablishmentTestInput(MOCK_COMPANY_NUMBER + 2));
        UK_ESTABLISHMENTS_TEST_OUTPUT = Arrays.asList(
                testHelper.createUkEstablishmentTestOutput(MOCK_COMPANY_NUMBER + 1),
                testHelper.createUkEstablishmentTestOutput(MOCK_COMPANY_NUMBER + 2));
        EXISTING_UK_ESTABLISHMENT_COMPANY = testHelper.createCompanyProfileTypeUkEstablishment(MOCK_COMPANY_NUMBER);
        EXISTING_PARENT_COMPANY = testHelper.createParentCompanyProfile(ANOTHER_PARENT_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("When company profile is retrieved successfully then it is returned")
    void getCompanyProfile() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        companyData.setType("ltd");
        LocalDateTime localDateTime = LocalDateTime.now();
        Updated updated = mock(Updated.class);
        VersionedCompanyProfileDocument companyProfileDocument = new VersionedCompanyProfileDocument(companyData, localDateTime, updated, false);
        companyProfileDocument.setId(MOCK_COMPANY_NUMBER);
        companyProfileDocument.getCompanyProfile().setCompanyStatus("string");

        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.of(companyProfileDocument));

        Optional<VersionedCompanyProfileDocument> companyProfileActual =
                companyProfileService.get(MOCK_COMPANY_NUMBER);

        assertThat(companyProfileActual).containsSame(companyProfileDocument);
        verify(logger, times(2)).trace(anyString(), any());
    }

    @Test
    @DisplayName("When company profile is retrieved successfully with CareOf then it is returned")
    void getCompanyProfileWithCareOf() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        companyData.setType("ltd");
        RegisteredOfficeAddress roa = new RegisteredOfficeAddress();
        roa.setCareOf("careOf");
        roa.setCareOfName("careOfName");
        companyData.setRegisteredOfficeAddress(roa);
        LocalDateTime localDateTime = LocalDateTime.now();
        Updated updated = mock(Updated.class);
        VersionedCompanyProfileDocument companyProfileDocument = new VersionedCompanyProfileDocument(companyData, localDateTime, updated, false);
        companyProfileDocument.setId(MOCK_COMPANY_NUMBER);
        companyProfileDocument.getCompanyProfile().setCompanyStatus("string");

        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.of(companyProfileDocument));

        Optional<VersionedCompanyProfileDocument> companyProfileActual =
                companyProfileService.get(MOCK_COMPANY_NUMBER);

        assertThat(companyProfileActual).containsSame(companyProfileDocument);
        assertEquals("careOf", companyProfileActual.get().getCompanyProfile().getRegisteredOfficeAddress().getCareOf());
        assertNull(companyProfileActual.get().getCompanyProfile().getRegisteredOfficeAddress().getCareOfName());
        verify(logger, times(2)).trace(anyString(), any());
    }

    @Test
    @DisplayName("When company profile is retrieved successfully with only CareOfName then it is returned with CareOf")
    void getCompanyProfileWithCareOfName() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        companyData.setType("ltd");
        RegisteredOfficeAddress roa = new RegisteredOfficeAddress();
        roa.setCareOfName("careOfName");
        companyData.setRegisteredOfficeAddress(roa);
        LocalDateTime localDateTime = LocalDateTime.now();
        Updated updated = mock(Updated.class);
        VersionedCompanyProfileDocument companyProfileDocument = new VersionedCompanyProfileDocument(companyData, localDateTime, updated, false);
        companyProfileDocument.setId(MOCK_COMPANY_NUMBER);
        companyProfileDocument.getCompanyProfile().setCompanyStatus("string");

        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.of(companyProfileDocument));

        Optional<VersionedCompanyProfileDocument> companyProfileActual =
                companyProfileService.get(MOCK_COMPANY_NUMBER);

        assertThat(companyProfileActual).containsSame(companyProfileDocument);
        assertEquals("careOfName", companyProfileActual.get().getCompanyProfile().getRegisteredOfficeAddress().getCareOf());
        assertNull(companyProfileActual.get().getCompanyProfile().getRegisteredOfficeAddress().getCareOfName());
        verify(logger, times(2)).trace(anyString(), any());
    }

    @Test
    @DisplayName("When company details is retrieved successfully then it is returned")
    void getCompanyDetails() throws JsonProcessingException {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        companyData.setCompanyName("String");
        LocalDateTime localDateTime = LocalDateTime.now();
        Updated updated = mock(Updated.class);
        VersionedCompanyProfileDocument mockCompanyProfileDocument = new VersionedCompanyProfileDocument(companyData, localDateTime, updated, false);
        mockCompanyProfileDocument.setId(MOCK_COMPANY_NUMBER);
        mockCompanyProfileDocument.getCompanyProfile().setCompanyStatus("String");
        CompanyDetails mockCompanyDetails = new CompanyDetails();
        mockCompanyDetails.setCompanyStatus("String");
        mockCompanyDetails.setCompanyName("String");
        mockCompanyDetails.setCompanyNumber(MOCK_COMPANY_NUMBER);
        Optional<CompanyDetails> mockCompanyDetailsOP = Optional.of(mockCompanyDetails);

        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.of(mockCompanyProfileDocument));

        Optional<CompanyDetails> companyDetailsActual =
                companyProfileService.getCompanyDetails(MOCK_COMPANY_NUMBER);

        assertEquals(mockCompanyDetailsOP,companyDetailsActual);
    }

    @Test
    @DisplayName("When no company profile is retrieved then return empty optional")
    void getNoCompanyProfileReturned() {
        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.empty());

        Optional<VersionedCompanyProfileDocument> companyProfileActual =
                companyProfileService.get(MOCK_COMPANY_NUMBER);

        assertTrue(companyProfileActual.isEmpty());
        verify(logger, times(2)).trace(anyString(), any());
    }

    @Test
    @DisplayName("When no company profile is retrieved then return empty optional")
    void getNoCompanyDetailsReturned() throws JsonProcessingException {
        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.empty());

        Optional<CompanyDetails> companyDetailsActual =
                companyProfileService.getCompanyDetails(MOCK_COMPANY_NUMBER);

        assertFalse(companyDetailsActual.isPresent());
    }

    @Test
    @DisplayName("When there's a connection issue while performing the GET request then throw a "
            + "service unavailable exception")
    void getConnectionIssueServiceUnavailable() {
        when(companyProfileRepository.findById(anyString()))
                .thenThrow(new DataAccessResourceFailureException("Connection broken"));

        Assert.assertThrows(ServiceUnavailableException.class,
                () -> companyProfileService.get(MOCK_COMPANY_NUMBER));
        verify(logger, times(1)).trace(anyString(), any());
    }

    @Test
    @DisplayName("When there's a connection issue while performing the GET request then throw a "
            + "service unavailable exception")
    void getCompanyDetailsConnectionIssueServiceUnavailable() {
        when(companyProfileRepository.findById(anyString()))
                .thenThrow(new ServiceUnavailableException("Service unavailable"));

        Exception exception = assertThrows(ServiceUnavailableException.class,
                () -> companyProfileService.getCompanyDetails(MOCK_COMPANY_NUMBER));

        assertEquals("Service unavailable", exception.getMessage());
    }

    @Test
    @DisplayName("When an illegal argument exception is thrown while performing the GET request then throw a "
            + "bad request exception")
    void getInvalidBadRequest() {
        when(companyProfileRepository.findById(anyString()))
                .thenThrow(new IllegalArgumentException());

        Assert.assertThrows(BadRequestException.class,
                () -> companyProfileService.get(MOCK_COMPANY_NUMBER));
        verify(logger, times(1)).trace(anyString(), any());
    }

    @Test
    void when_insolvency_data_is_given_then_data_should_be_saved() throws Exception {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        LocalDateTime localDateTime = LocalDateTime.now();
        Updated updated = new Updated(localDateTime,
                null, "company-profile");

        VersionedCompanyProfileDocument mockCompanyProfileDocument = new VersionedCompanyProfileDocument(companyData, localDateTime, updated, false);
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
    void patchConnectionIssueServiceUnavailable() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        LocalDateTime localDateTime = LocalDateTime.now();
        Updated updated = mock(Updated.class);

        VersionedCompanyProfileDocument mockCompanyProfileDocument = new VersionedCompanyProfileDocument(companyData, localDateTime, updated, false);
        mockCompanyProfileDocument.setId(MOCK_COMPANY_NUMBER);

        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.of(mockCompanyProfileDocument));

        when(apiResponse.getStatusCode()).thenReturn(200);
        when(companyProfileApiService.invokeChsKafkaApi(anyString(), anyString())).thenReturn(apiResponse);

        CompanyProfile companyProfileWithInsolvency = mockCompanyProfileWithoutInsolvency();
        companyProfileWithInsolvency.getData().getLinks().setInsolvency("INSOLVENCY_LINK");

        when(mongoTemplate.upsert(any(Query.class), any(Update.class), any(Class.class))).thenThrow(new DataAccessResourceFailureException("Connection broken"));
        Assert.assertThrows(ServiceUnavailableException.class,
                () -> companyProfileService.updateInsolvencyLink(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER,
                        companyProfileWithInsolvency));
    }

    @Test
    @DisplayName("When company profile does not exist while performing the PATCH request then throw a "
            + "DocumentNotFoundException")
    void patchDocumentNotFound() {
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
    void addExemptionsLink() {
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
    void addExemptionsLinkConflict() {
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
    void addExemptionsLinkIllegalArgument() {
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
    void addExemptionsLinkDataAccessExceptionFindById() {
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
    void addExemptionsLinkDataAccessExceptionUpdate() {
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
    void deleteExemptionsLink() {
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
    void deleteExemptionsLinkConflict() {
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
    void deleteExemptionsLinkIllegalArgument() {
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
    void deleteExemptionsLinkDataAccessExceptionFindById() {
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
    void deleteExemptionsLinkDataAccessExceptionUpdate() {
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
    @DisplayName("Add charges link successfully updates MongoDB and calls chs-kafka-api")
    void addChargesLink() {
        // given
        when(linkRequestFactory.createLinkRequest(CHARGES_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add charges link throws document not found exception")
    void addChargesLinkNotFound() {
        // given
        when(linkRequestFactory.createLinkRequest(CHARGES_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format("No company profile with company number %s found", MOCK_COMPANY_NUMBER), exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Add charges link throws resource state conflict exception")
    void addChargesLinkConflict() {
        // given
        when(linkRequestFactory.createLinkRequest(CHARGES_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getCharges()).thenReturn(CHARGES_LINK);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        Exception exception = assertThrows(ResourceStateConflictException.class, executable);
        assertEquals("Resource state conflict; charges link already exists", exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Add charges link throws service unavailable exception when illegal argument exception caught")
    void addChargesLinkIllegalArgument() {
        // given
        when(linkRequestFactory.createLinkRequest(CHARGES_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(companyProfileApiService.invokeChsKafkaApi(any(), any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add charges link throws service unavailable exception when data access exception thrown during findById")
    void addChargesLinkDataAccessExceptionFindById() {
        // given
        when(linkRequestFactory.createLinkRequest(CHARGES_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Add charges link throws service unavailable exception when data access exception thrown during update")
    void addChargesLinkDataAccessExceptionUpdate() {
        // given
        when(linkRequestFactory.createLinkRequest(CHARGES_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(mongoTemplate.updateFirst(any(), any(), eq(CompanyProfileDocument.class))).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
    }

    @Test
    @DisplayName("Delete charges link successfully updates MongoDB and calls chs-kafka-api")
    void deleteChargesLink() {
        // given
        when(linkRequestFactory.createLinkRequest(CHARGES_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getCharges()).thenReturn(CHARGES_LINK);

        // when
        companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete charges link throws document not found exception")
    void deleteChargesLinkNotFound() {
        // given
        when(linkRequestFactory.createLinkRequest(CHARGES_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format("No company profile with company number %s found", MOCK_COMPANY_NUMBER), exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete charges link throws resource state conflict exception")
    void deleteChargesLinkConflict() {
        // given
        when(linkRequestFactory.createLinkRequest(CHARGES_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        Exception exception = assertThrows(ResourceStateConflictException.class, executable);
        assertEquals("Resource state conflict; charges link already does not exist", exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete charges link throws service unavailable exception when illegal argument exception caught")
    void deleteChargesLinkIllegalArgument() {
        // given
        when(linkRequestFactory.createLinkRequest(CHARGES_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getCharges()).thenReturn(CHARGES_LINK);
        when(companyProfileApiService.invokeChsKafkaApi(any(), any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete charges link throws service unavailable exception when data access exception thrown during findById")
    void deleteChargesLinkDataAccessExceptionFindById() {
        // given
        when(linkRequestFactory.createLinkRequest(CHARGES_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete charges link throws service unavailable exception when data access exception thrown during update")
    void deleteChargesLinkDataAccessExceptionUpdate() {
        // given
        when(linkRequestFactory.createLinkRequest(CHARGES_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getCharges()).thenReturn(CHARGES_LINK);
        when(mongoTemplate.updateFirst(any(), any(), eq(CompanyProfileDocument.class))).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
    }

    @Test
    @DisplayName("Add insolvency link successfully updates MongoDB and calls chs-kafka-api")
    void addInsolvencyLink() {
        // given
        when(linkRequestFactory.createLinkRequest(INSOLVENCY_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add insolvency link throws document not found exception")
    void addInsolvencyLinkNotFound() {
        // given
        when(linkRequestFactory.createLinkRequest(INSOLVENCY_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format("No company profile with company number %s found", MOCK_COMPANY_NUMBER), exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Add insolvency link throws resource state conflict exception")
    void addInsolvencyLinkConflict() {
        // given
        when(linkRequestFactory.createLinkRequest(INSOLVENCY_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getInsolvency()).thenReturn(INSOLVENCY_LINK);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        Exception exception = assertThrows(ResourceStateConflictException.class, executable);
        assertEquals("Resource state conflict; insolvency link already exists", exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Add insolvency link throws service unavailable exception when illegal argument exception caught")
    void addInsolvencyLinkIllegalArgument() {
        // given
        when(linkRequestFactory.createLinkRequest(INSOLVENCY_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(companyProfileApiService.invokeChsKafkaApi(any(), any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add insolvency link throws service unavailable exception when data access exception thrown during findById")
    void addInsolvencyLinkDataAccessExceptionFindById() {
        // given
        when(linkRequestFactory.createLinkRequest(INSOLVENCY_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Add insolvency link throws service unavailable exception when data access exception thrown during update")
    void addInsolvencyLinkDataAccessExceptionUpdate() {
        // given
        when(linkRequestFactory.createLinkRequest(INSOLVENCY_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(mongoTemplate.updateFirst(any(), any(), eq(CompanyProfileDocument.class))).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
    }

    @Test
    @DisplayName("Delete insolvency link successfully updates MongoDB and calls chs-kafka-api")
    void deleteInsolvencyLink() {
        // given
        when(linkRequestFactory.createLinkRequest(INSOLVENCY_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getInsolvency()).thenReturn(INSOLVENCY_LINK);

        // when
        companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete insolvency link throws document not found exception")
    void deleteInsolvencyLinkNotFound() {
        // given
        when(linkRequestFactory.createLinkRequest(INSOLVENCY_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format("No company profile with company number %s found", MOCK_COMPANY_NUMBER), exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete insolvency link throws resource state conflict exception")
    void deleteInsolvencyLinkConflict() {
        // given
        when(linkRequestFactory.createLinkRequest(INSOLVENCY_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        Exception exception = assertThrows(ResourceStateConflictException.class, executable);
        assertEquals("Resource state conflict; insolvency link already does not exist", exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete insolvency link throws service unavailable exception when illegal argument exception caught")
    void deleteInsolvencyLinkIllegalArgument() {
        // given
        when(linkRequestFactory.createLinkRequest(INSOLVENCY_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getInsolvency()).thenReturn(INSOLVENCY_LINK);
        when(companyProfileApiService.invokeChsKafkaApi(any(), any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete insolvency link throws service unavailable exception when data access exception thrown during findById")
    void deleteInsolvencyLinkDataAccessExceptionFindById() {
        // given
        when(linkRequestFactory.createLinkRequest(INSOLVENCY_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete insolvency link throws service unavailable exception when data access exception thrown during update")
    void deleteInsolvencyLinkDataAccessExceptionUpdate() {
        // given
        when(linkRequestFactory.createLinkRequest(INSOLVENCY_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getInsolvency()).thenReturn(INSOLVENCY_LINK);
        when(mongoTemplate.updateFirst(any(), any(), eq(CompanyProfileDocument.class))).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
    }

    @Test
    @DisplayName("Add officers link successfully updates MongoDB and calls chs-kafka-api")
    void addOfficersLink() {
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
    void addOfficersLinkConflict() {
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
    void addOfficersLinkIllegalArgument() {
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
    void addOfficersLinkDataAccessExceptionFindById() {
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
    void addOfficersLinkDataAccessExceptionUpdate() {
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
    void deleteOfficersLink() {
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
    void deleteOfficersLinkConflict() {
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
    void deleteOfficersLinkIllegalArgument() {
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
    void deleteOfficersLinkDataAccessExceptionFindById() {
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
    void deleteOfficersLinkDataAccessExceptionUpdate() {
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
    void addPscStatementsLink() {
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
    void addPscStatementsLinkConflict() {
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
    void addPscStatementsLinkIllegalArgument() {
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
    void addPscStatementsLinkDataAccessExceptionFindById() {
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
    void addPscStatementsLinkDataAccessExceptionUpdate() {
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
    void deletePscStatementsLink() {
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
    void deletePscStatementsLinkConflict() {
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
    void deletePscStatementsLinkIllegalArgument() {
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
    void deletePscStatementsLinkDataAccessExceptionFindById() {
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
    void deletePscStatementsLinkDataAccessExceptionUpdate() {
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

    @Test
    @DisplayName("Add psc link successfully updates MongoDB and calls chs-kafka-api")
    void addPscLink() {
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
    void addPscLinkConflict() {
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
    void addPscLinkIllegalArgument() {
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
    void addPscLinkDataAccessExceptionFindById() {
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
    void addPscLinkDataAccessExceptionUpdate() {
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
    void deletePscLink() {
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
    void deletePscLinkConflict() {
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
    void deletePscLinkIllegalArgument() {
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
    void deletePscLinkDataAccessExceptionFindById() {
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
    void deletePscLinkDataAccessExceptionUpdate() {
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
    @DisplayName("Put company profile")
    void putCompanyProfileSuccessfully() {
        when(companyProfileRepository.findById(MOCK_PARENT_COMPANY_NUMBER)).thenReturn(Optional.of(EXISTING_PARENT_COMPANY_PROFILE_DOCUMENT));
        EXISTING_PARENT_COMPANY_PROFILE_DOCUMENT.getCompanyProfile().getLinks().setUkEstablishments(null);
        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenReturn(Optional.empty());
        when(companyProfileTransformer.transform(any(), any(), any())) //Something with the company_profile_document here.
                .thenReturn(COMPANY_PROFILE_DOCUMENT.version(0L));

        companyProfileService.processCompanyProfile(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER,
                COMPANY_PROFILE);

        Assertions.assertNotNull(COMPANY_PROFILE);
        Assertions.assertNotNull(COMPANY_PROFILE_DOCUMENT);
        Assertions.assertNull(COMPANY_PROFILE_DOCUMENT.getCompanyProfile().getLinks().getOverseas());
        verify(companyProfileRepository).insert(COMPANY_PROFILE_DOCUMENT);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Put company profile with existing links")
    void putCompanyProfileWithExistingLinksSuccessfully() {
        when(companyProfileRepository.findById(MOCK_PARENT_COMPANY_NUMBER))
                .thenReturn(Optional.of(EXISTING_PARENT_COMPANY_PROFILE_DOCUMENT));
        EXISTING_PARENT_COMPANY_PROFILE_DOCUMENT.getCompanyProfile().getLinks().setUkEstablishments(null);

        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenReturn(Optional.of(EXISTING_COMPANY_PROFILE_DOCUMENT));
        when(companyProfileTransformer.transform(EXISTING_COMPANY_PROFILE_DOCUMENT, COMPANY_PROFILE, EXISTING_LINKS))
                .thenReturn(EXISTING_COMPANY_PROFILE_DOCUMENT);

        companyProfileService.processCompanyProfile(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER,
                COMPANY_PROFILE);

        Assertions.assertNotNull(COMPANY_PROFILE);
        Assertions.assertNotNull(COMPANY_PROFILE_DOCUMENT);
        Assertions.assertNotNull(EXISTING_COMPANY_PROFILE_DOCUMENT);
        Assertions.assertNotNull(EXISTING_LINKS);
        verify(companyProfileRepository).save(EXISTING_COMPANY_PROFILE_DOCUMENT);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("When a company number is provided to retrieve company profile successfully then it is returned")
    public void testRetrieveCompanyNumber() throws ResourceNotFoundException, JsonProcessingException {
        document.setCompanyProfile(new Data());

        when(companyProfileRepository.findById(anyString())).thenReturn(Optional.of(document));

        Data result = companyProfileService.retrieveCompanyNumber(MOCK_COMPANY_NUMBER);

        assertEquals(document.getCompanyProfile(), result);
        verify(companyProfileRepository, times(1)).findById(anyString());
    }

    @Test
    @DisplayName("An empty list of company names or corporate annotations stored in Mongo should not be returned")
    public void testRetrieveCompanyNumberWithEmptyList() throws ResourceNotFoundException, JsonProcessingException {
        CompanyProfileDocument doc = new CompanyProfileDocument();
        Data data = new Data();
        data.setCompanyNumber(MOCK_COMPANY_NUMBER);
        data.setCorporateAnnotation(Collections.emptyList());
        data.setPreviousCompanyNames(Collections.emptyList());
        doc.setCompanyProfile(data);

        when(companyProfileRepository.findById(anyString())).thenReturn(Optional.of(doc));

        Data result = companyProfileService.retrieveCompanyNumber(MOCK_COMPANY_NUMBER);
        assertNull(result.getCorporateAnnotation());
        assertNull(result.getPreviousCompanyNames());

        assertEquals(doc.getCompanyProfile(), result);
        verify(companyProfileRepository, times(1)).findById(anyString());
    }

    @Test
    @DisplayName("When retrieving company profile then it is returned with careOf")
    public void testRetrieveCompanyNumberCareOf() throws ResourceNotFoundException {
        VersionedCompanyProfileDocument doc = new VersionedCompanyProfileDocument();
        Data data = new Data();
        RegisteredOfficeAddress roa = new RegisteredOfficeAddress();
        roa.setCareOf("careOf");
        roa.setCareOfName("careOfName");
        data.setRegisteredOfficeAddress(roa);
        doc.setCompanyProfile(data);

        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenReturn(Optional.of(doc));

        Data result = companyProfileService.retrieveCompanyNumber(MOCK_COMPANY_NUMBER);

        assertEquals(doc.getCompanyProfile(), result);
        assertEquals("careOf", result.getRegisteredOfficeAddress().getCareOf());
        assertNull(result.getRegisteredOfficeAddress().getCareOfName());
        verify(companyProfileRepository, times(1)).findById(anyString());
    }

    @Test
    @DisplayName("When retrieving company profile without careOf then it is returned with careOf populated by careOfName")
    public void testRetrieveCompanyNumberCareOfName() throws ResourceNotFoundException {
        VersionedCompanyProfileDocument doc = new VersionedCompanyProfileDocument();
        Data data = new Data();
        RegisteredOfficeAddress roa = new RegisteredOfficeAddress();
        roa.setCareOfName("careOfName");
        data.setRegisteredOfficeAddress(roa);
        doc.setCompanyProfile(data);

        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenReturn(Optional.of(doc));

        Data result = companyProfileService.retrieveCompanyNumber(MOCK_COMPANY_NUMBER);

        assertEquals(doc.getCompanyProfile(), result);
        assertEquals("careOfName", result.getRegisteredOfficeAddress().getCareOf());
        assertNull(result.getRegisteredOfficeAddress().getCareOfName());
        verify(companyProfileRepository, times(1)).findById(anyString());
    }

    @Test
    @DisplayName("Retrieve date of cessation on a company number with dissolution date")
    public void testRetrieveCompanyNumberCessationDate() throws ResourceNotFoundException, JsonProcessingException {
        VersionedCompanyProfileDocument profileDocument = new VersionedCompanyProfileDocument();
        Data profileData = new Data();
        profileData.setDateOfDissolution(LocalDate.of(2019, 1, 1));
        profileDocument.setCompanyProfile(profileData);

        when(companyProfileRepository.findById(anyString())).thenReturn(Optional.of(profileDocument));

        Data result = companyProfileService.retrieveCompanyNumber(MOCK_COMPANY_NUMBER);

        assertEquals(LocalDate.of(2019, 1, 1), result.getDateOfCessation());
        assertNull(result.getDateOfDissolution());
        verify(companyProfileRepository, times(1)).findById(anyString());
    }

    @Test
    @DisplayName("When Resource Not Found exception is thrown and that it is handled well by the CompanyProfileService")
    public void testRetrieveCompanyNumberResourceNotFoundException(){
        when(companyProfileRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> companyProfileService.retrieveCompanyNumber(MOCK_COMPANY_NUMBER));
        verify(companyProfileRepository, times(1)).findById(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("When company number is provided delete company profile")
    public void testDeleteCompanyProfile() {
        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenReturn(Optional.ofNullable(EXISTING_COMPANY_PROFILE_DOCUMENT));
        companyProfileService.deleteCompanyProfile("123456", MOCK_COMPANY_NUMBER);

        verify(companyProfileRepository, times(1)).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileRepository, times(1)).delete(EXISTING_COMPANY_PROFILE_DOCUMENT);
        verify(companyProfileService, times((0))).checkForDeleteLink(any());
    }

    @Test
    @DisplayName("Check delete link should be called when deleting Uk establishments")
    public void testDeleteCompanyProfileUkEstablishments() {
        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).
                thenReturn(Optional.ofNullable(EXISTING_UK_ESTABLISHMENT_COMPANY));
        when(companyProfileRepository.findById(ANOTHER_PARENT_COMPANY_NUMBER))
                .thenReturn(Optional.ofNullable(EXISTING_PARENT_COMPANY));
        companyProfileService.deleteCompanyProfile("123456", MOCK_COMPANY_NUMBER);

        verify(companyProfileRepository, times(1)).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileRepository, times(1)).findById(ANOTHER_PARENT_COMPANY_NUMBER);
        verify(companyProfileRepository, times(1)).delete(EXISTING_UK_ESTABLISHMENT_COMPANY);
        verify(companyProfileService, times(1)).checkForDeleteLink(any());
    }

    @Test
    @DisplayName("When company number is null throw ResourceNotFound Exception")
    public void testDeleteCompanyProfileThrowsResourceNotFoundException() {
        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            companyProfileService.deleteCompanyProfile("123456", MOCK_COMPANY_NUMBER);
        });

        verify(companyProfileRepository, times(1)).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileRepository, times(0)).delete(any());
        verify(companyProfileService, times((0))).checkForDeleteLink(any());
    }

    @Test
    @DisplayName("Add filing history link successfully updates MongoDB and calls chs-kafka-api")
    void addFilingHistoryLink() {
        // given
        LinkRequest filingHistoryLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                FILING_HISTORY_LINK_TYPE, FILING_HISTORY_DELTA_TYPE, Links::getFilingHistory);

        when(linkRequestFactory.createLinkRequest(anyString(), anyString(), anyString())).thenReturn(filingHistoryLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        companyProfileService.processLinkRequest(FILING_HISTORY_LINK_TYPE, MOCK_COMPANY_NUMBER, MOCK_CONTEXT_ID, false);

        // then
        verify(linkRequestFactory).createLinkRequest(FILING_HISTORY_LINK_TYPE, MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Can file set to true when company type ltd and status active")
    void testDetermineCanFileLtdActiveTrue() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        companyData.setCompanyStatus("active");
        companyData.setType("ltd");
        CompanyProfileDocument companyProfileDocument = new CompanyProfileDocument();
        companyProfileDocument.setCompanyProfile(companyData);

        companyProfileService.determineCanFile(companyProfileDocument);

        assertEquals(companyData.getCanFile(), true);
    }

    @Test
    @DisplayName("Can file set to false when company type ltd and status dissolved")
    void testDetermineCanFileLtdDissolvedFalse() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        companyData.setCompanyStatus("dissolved");
        companyData.setType("ltd");
        CompanyProfileDocument companyProfileDocument = new CompanyProfileDocument();
        companyProfileDocument.setCompanyProfile(companyData);

        companyProfileService.determineCanFile(companyProfileDocument);

        assertEquals(companyData.getCanFile(), false);
    }

    @Test
    @DisplayName("Can file set to false when company type ltd and status dissolved")
    void testDetermineCanFileOtherActiveFalse() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        companyData.setCompanyStatus("active");
        companyData.setType("other");
        CompanyProfileDocument companyProfileDocument = new CompanyProfileDocument();
        companyProfileDocument.setCompanyProfile(companyData);

        companyProfileService.determineCanFile(companyProfileDocument);

        assertEquals(companyData.getCanFile(), false);
    }

    @Test
    @DisplayName("Can file set to false when company type is null")
    void testDetermineCanFileCompanyTypeNull() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        companyData.setCompanyStatus("active");
        CompanyProfileDocument companyProfileDocument = new CompanyProfileDocument();
        companyProfileDocument.setCompanyProfile(companyData);

        companyProfileService.determineCanFile(companyProfileDocument);

        assertEquals(companyData.getCanFile(), false);
    }

    @Test
    @DisplayName(("Overdue set to false when next due fields are after current date"))
    void testDetermineOverdueFalse() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        companyData.setConfirmationStatement(confirmationStatement);
        companyData.setAnnualReturn(annualReturn);
        companyData.setAccounts(accounts);
        when(accounts.getNextAccounts()).thenReturn(nextAccounts);
        when(nextAccounts.getDueOn()).thenReturn(LocalDate.of(2050, 1, 1));
        when(confirmationStatement.getNextDue()).thenReturn(LocalDate.of(2050, 1, 1));
        when(annualReturn.getNextDue()).thenReturn(LocalDate.of(2050, 1, 1));

        CompanyProfileDocument companyProfileDocument = new CompanyProfileDocument();
        companyProfileDocument.setCompanyProfile(companyData);

        companyProfileService.determineOverdue(companyProfileDocument);

        assertEquals(false, confirmationStatement.getOverdue());
        assertEquals(false, nextAccounts.getOverdue());
        assertEquals(false, annualReturn.getOverdue());
    }

    @Test
    @DisplayName("Overdue set to true when next due fields are before current date")
    void testDetermineOverdueTrue() {
        CompanyProfileDocument companyProfileDocument = COMPANY_PROFILE_DOCUMENT;

        companyProfileService.determineOverdue(companyProfileDocument);

        assertEquals(true, COMPANY_PROFILE_DOCUMENT.getCompanyProfile().getConfirmationStatement().getOverdue());
        assertEquals(true, COMPANY_PROFILE_DOCUMENT.getCompanyProfile().getAccounts().getNextAccounts().getOverdue());
        assertEquals(true, COMPANY_PROFILE_DOCUMENT.getCompanyProfile().getAnnualReturn().getOverdue());
    }

    @Test
    @DisplayName("Overdue not set when next due fields are null")
    void testDetermineOverDueNull() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        companyData.setConfirmationStatement(confirmationStatement);
        companyData.setAnnualReturn(annualReturn);
        companyData.setAccounts(accounts);
        when(accounts.getNextAccounts()).thenReturn(nextAccounts);
        when(nextAccounts.getDueOn()).thenReturn(null);
        when(confirmationStatement.getNextDue()).thenReturn(null);
        when(annualReturn.getNextDue()).thenReturn(null);

        CompanyProfileDocument companyProfileDocument = new CompanyProfileDocument();
        companyProfileDocument.setCompanyProfile(companyData);

        companyProfileService.determineOverdue(companyProfileDocument);

        assertEquals(false, confirmationStatement.getOverdue());
        assertEquals(false, nextAccounts.getOverdue());
        assertEquals(false, annualReturn.getOverdue());
    }

    @Test
    @DisplayName("Add new uk establishments links successfully")
    void addNewUkEstablishmentsLinkSuccessfully() {
        CompanyProfileDocument companyProfileDocument = EXISTING_COMPANY_PROFILE_DOCUMENT;
        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenReturn(Optional.of(EXISTING_COMPANY_PROFILE_DOCUMENT));
        EXISTING_PARENT_COMPANY_PROFILE_DOCUMENT.getCompanyProfile().getLinks().setUkEstablishments(null);

        companyProfileDocument.setId(MOCK_PARENT_COMPANY_NUMBER);
        companyProfileDocument.getCompanyProfile().setCompanyNumber(MOCK_PARENT_COMPANY_NUMBER);
        when(companyProfileRepository.findById(MOCK_PARENT_COMPANY_NUMBER)).thenReturn(Optional.of(EXISTING_PARENT_COMPANY_PROFILE_DOCUMENT));

        BranchCompanyDetails branchCompanyDetails = new BranchCompanyDetails();
        branchCompanyDetails.setParentCompanyNumber(MOCK_PARENT_COMPANY_NUMBER);
        CompanyProfile companyProfile = COMPANY_PROFILE;
        companyProfile.getData().setBranchCompanyDetails(branchCompanyDetails);

        companyProfileService.processCompanyProfile(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER,
                companyProfile);

        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_PARENT_COMPANY_NUMBER);
        Assertions.assertEquals(companyProfile.getData().getLinks().getOverseas(), String.format("/company/%s", MOCK_PARENT_COMPANY_NUMBER));
    }

    @Test
    @DisplayName("Put company profile with existing links")
    void putUkEstablishmentAndOverseasSuccessfully() {
        when(companyProfileRepository.findById(MOCK_PARENT_COMPANY_NUMBER))
                .thenReturn(Optional.of(EXISTING_PARENT_COMPANY_PROFILE_DOCUMENT));
        EXISTING_PARENT_COMPANY_PROFILE_DOCUMENT.getCompanyProfile().getLinks().setUkEstablishments(null);

        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenReturn(Optional.of(EXISTING_COMPANY_PROFILE_DOCUMENT));
        when(companyProfileTransformer.transform(EXISTING_COMPANY_PROFILE_DOCUMENT, COMPANY_PROFILE, EXISTING_LINKS))
                .thenReturn(EXISTING_COMPANY_PROFILE_DOCUMENT);

        companyProfileService.processCompanyProfile(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER,
                COMPANY_PROFILE);

        Assertions.assertNotNull(COMPANY_PROFILE);
        Assertions.assertNotNull(COMPANY_PROFILE_DOCUMENT);
        Assertions.assertNotNull(EXISTING_COMPANY_PROFILE_DOCUMENT);
        Assertions.assertNotNull(EXISTING_LINKS);
        Assertions.assertEquals(COMPANY_PROFILE.getData().getLinks().getOverseas(), String.format("/company/%s", MOCK_PARENT_COMPANY_NUMBER));
        verify(companyProfileRepository).save(EXISTING_COMPANY_PROFILE_DOCUMENT);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add new uk establishments links unsuccessfully and throw 503")
    void addNewUkEstablishmentsLinkUnsuccessfullyAndThrow503() {
        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenThrow(ServiceUnavailableException.class);

        assertThrows(ServiceUnavailableException.class, () -> {
            companyProfileService.processCompanyProfile(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER,
                    COMPANY_PROFILE);
        });
        verifyNoInteractions(companyProfileApiService);
    }

    @Test
    @DisplayName("Delete uk establishment link successfully updates MongoDB and calls chs-kafka-api")
    void deleteUkEstablishmentLink() {
        // given
        LinkRequest ukEstablishmentLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                UK_ESTABLISHMENTS_LINK_TYPE, UK_ESTABLISHMENTS_DELTA_TYPE, Links::getUkEstablishments);
        when(linkRequestFactory.createLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(ukEstablishmentLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getUkEstablishments()).thenReturn(String.format(
                "/company/%s/uk-establishments", MOCK_COMPANY_NUMBER));

        // when
        companyProfileService.processLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileRepository).findAllByParentCompanyNumber(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Don't delete the uk establishment link due to establishments existing")
    void deleteUkEstablishmentLinkEstablishmentsExists() {
        // given
        LinkRequest ukEstablishmentLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                UK_ESTABLISHMENTS_LINK_TYPE, UK_ESTABLISHMENTS_DELTA_TYPE, Links::getUkEstablishments);
        when(linkRequestFactory.createLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(ukEstablishmentLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getUkEstablishments()).thenReturn(String.format(
                "/company/%s/uk-establishments", MOCK_COMPANY_NUMBER));
        when(companyProfileRepository.findAllByParentCompanyNumber(MOCK_COMPANY_NUMBER))
                .thenReturn(UK_ESTABLISHMENTS_TEST_INPUT);

        // when
        companyProfileService.processLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                MOCK_CONTEXT_ID, true);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileRepository).findAllByParentCompanyNumber(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
    }

    @Test
    @DisplayName("Delete uk establishment link throws document not found exception")
    void deleteUkEstablishmentLinkNotFound() {
        // given
        LinkRequest ukEstablishmentLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                UK_ESTABLISHMENTS_LINK_TYPE, UK_ESTABLISHMENTS_DELTA_TYPE, Links::getUkEstablishments);
        when(linkRequestFactory.createLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(ukEstablishmentLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE,
                MOCK_COMPANY_NUMBER, MOCK_CONTEXT_ID, true);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format("No company profile with company number %s found",
                MOCK_COMPANY_NUMBER), exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete uk establishment throws resource state conflict exception")
    void deleteUkEstablishmentConflict() {
        // given
        LinkRequest ukEstablishmentLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                UK_ESTABLISHMENTS_LINK_TYPE, UK_ESTABLISHMENTS_DELTA_TYPE, Links::getUkEstablishments);
        when(linkRequestFactory.createLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(ukEstablishmentLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE,
                MOCK_COMPANY_NUMBER, MOCK_CONTEXT_ID, true);

        // then
        Exception exception = assertThrows(ResourceStateConflictException.class, executable);
        assertEquals("Resource state conflict; uk-establishments link already does not exist",
                exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete uk establishment throws service unavailable exception when illegal argument exception caught")
    void deleteUkEstablishmentIllegalArgument() {
        // given
        LinkRequest ukEstablishmentLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                UK_ESTABLISHMENTS_LINK_TYPE, UK_ESTABLISHMENTS_DELTA_TYPE, Links::getUkEstablishments);
        when(linkRequestFactory.createLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(ukEstablishmentLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getUkEstablishments()).thenReturn(String.format(
                "/company/%s/uk-establishments", MOCK_COMPANY_NUMBER));
        when(companyProfileApiService.invokeChsKafkaApi(any(), any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE,
                MOCK_COMPANY_NUMBER, MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileRepository).findAllByParentCompanyNumber(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete uk establishment throws service unavailable exception when data access exception thrown during findById")
    void deleteUkEstablishmentDataAccessExceptionFindById() {
        // given
        LinkRequest ukEstablishmentLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                UK_ESTABLISHMENTS_LINK_TYPE, UK_ESTABLISHMENTS_DELTA_TYPE, Links::getUkEstablishments);
        when(linkRequestFactory.createLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(ukEstablishmentLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE,
                MOCK_COMPANY_NUMBER, MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete uk establishment throws service unavailable exception when data access exception thrown during update")
    void deleteUkEstablishmentDataAccessExceptionUpdate() {
        // given
        LinkRequest ukEstablishmentLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                UK_ESTABLISHMENTS_LINK_TYPE, UK_ESTABLISHMENTS_DELTA_TYPE, Links::getUkEstablishments);
        when(linkRequestFactory.createLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE,
                MOCK_CONTEXT_ID,MOCK_COMPANY_NUMBER)).thenReturn(ukEstablishmentLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getUkEstablishments()).thenReturn(String.format(
                "/company/%s/uk-establishments", MOCK_COMPANY_NUMBER));
        when(mongoTemplate.updateFirst(any(), any(), eq(CompanyProfileDocument.class))).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE,
                MOCK_COMPANY_NUMBER, MOCK_CONTEXT_ID, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileRepository).findAllByParentCompanyNumber(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
    }

    @Test
    @DisplayName("Create parent company profile for uk establishment when not already present")
    void createParentCompanyForUkEstablishment() {
        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenReturn(Optional.empty());
        when(companyProfileRepository.findById(MOCK_PARENT_COMPANY_NUMBER)).thenReturn(Optional.empty());
        when(companyProfileTransformer.transform(any(), any(), any())).thenReturn(COMPANY_PROFILE_DOCUMENT);

        companyProfileService.processCompanyProfile(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER,
                COMPANY_PROFILE);

        verify(companyProfileRepository, times(1)).save(any());
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_CONTEXT_ID, MOCK_PARENT_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Overdue not set when all fields are null")
    void testDetermineOverDueAllNull() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);

        CompanyProfileDocument companyProfileDocument = new CompanyProfileDocument();
        companyProfileDocument.setCompanyProfile(companyData);

        companyProfileService.determineOverdue(companyProfileDocument);

        assertEquals(confirmationStatement.getOverdue(), false);
        assertEquals(nextAccounts.getOverdue(), false);
        assertEquals(annualReturn.getOverdue(), false);
    }

    @Test
    @DisplayName("Returns a list of UK establishments for given parent company number")
    void testGetUKEstablishmentsReturnsCorrectData() {
        VersionedCompanyProfileDocument companyProfileDocument = new VersionedCompanyProfileDocument();
        companyProfileDocument.setId(MOCK_PARENT_COMPANY_NUMBER);
        // given
        when(companyProfileRepository.findById(MOCK_PARENT_COMPANY_NUMBER)).thenReturn(
                Optional.of(companyProfileDocument));
        when(companyProfileRepository.findAllByParentCompanyNumber(MOCK_PARENT_COMPANY_NUMBER))
                .thenReturn(UK_ESTABLISHMENTS_TEST_INPUT);

        // when
        UkEstablishmentsList result = companyProfileService.getUkEstablishments(MOCK_PARENT_COMPANY_NUMBER);

        // then
        assertEquals("/company/321033", result.getLinks().getSelf());
        assertEquals("related-companies", result.getKind());
        assertEquals(UK_ESTABLISHMENTS_TEST_OUTPUT, result.getItems());
        verify(companyProfileRepository).findById(MOCK_PARENT_COMPANY_NUMBER);
        verify(companyProfileRepository).findAllByParentCompanyNumber(MOCK_PARENT_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Should not remove fields on update")
    void updateCompanyProfileShouldNotRemoveHasChargesField() {
        VersionedCompanyProfileDocument existingDoc = new VersionedCompanyProfileDocument();
        existingDoc.setCompanyProfile(new Data());
        existingDoc.setId("6146287");
        existingDoc.getCompanyProfile().setHasBeenLiquidated(true);
        existingDoc.getCompanyProfile().setHasCharges(true);
        existingDoc.version(1L);
        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenReturn(Optional.of(existingDoc));

        CompanyProfile profileToTransform = new CompanyProfile();
        profileToTransform.setData(new Data());
        profileToTransform.getData().setHasBeenLiquidated(true);
        profileToTransform.getData().setHasCharges(true);
        profileToTransform.getData().setCompanyNumber("6146287");
        when(companyProfileTransformer.transform(existingDoc, profileToTransform, null))
                .thenReturn(COMPANY_PROFILE_DOCUMENT);

        CompanyProfile companyProfile = new CompanyProfile();
        companyProfile.setData(new Data());
        companyProfile.getData().setHasCharges(null);
        companyProfile.getData().setHasBeenLiquidated(null);
        companyProfile.getData().setCompanyNumber("6146287");
        companyProfileService.processCompanyProfile(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER,
                companyProfile);
        verify(companyProfileTransformer).transform(existingDoc, profileToTransform, null);

        Assertions.assertNotNull(COMPANY_PROFILE_DOCUMENT);
        verify(companyProfileRepository).save(COMPANY_PROFILE_DOCUMENT);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
    }

    @Test
    void updateCompanyProfileWhenHasChargesIsFalse() {
        CompanyProfile companyProfile = new CompanyProfile();
        Links links = new Links();
        companyProfile.setData(new Data());
        companyProfile.getData().setLinks(links);
        companyProfile.getData().setHasCharges(false);
        companyProfile.getData().setHasBeenLiquidated(false);
        companyProfile.getData().setCompanyNumber(MOCK_COMPANY_NUMBER);

        when(companyProfileRepository.findById(anyString())).thenReturn(Optional.of(EXISTING_COMPANY_PROFILE_DOCUMENT));
        when(companyProfileTransformer.transform(any(), any(), any())).thenReturn(COMPANY_PROFILE_DOCUMENT);

        companyProfileService.processCompanyProfile(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER,
                companyProfile);

        assertFalse(companyProfile.getData().getHasCharges());
    }

    @Test
    void updateCompanyProfileWhenHasChargesIsNull() {
        CompanyProfile companyProfile = new CompanyProfile();
        Links links = new Links();
        companyProfile.setData(new Data());
        companyProfile.getData().setLinks(links);
        companyProfile.getData().setHasCharges(null);
        companyProfile.getData().setHasBeenLiquidated(false);
        companyProfile.getData().setCompanyNumber(MOCK_COMPANY_NUMBER);

        when(companyProfileRepository.findById(anyString())).thenReturn(Optional.of(EXISTING_COMPANY_PROFILE_DOCUMENT));
        when(companyProfileTransformer.transform(any(), any(), any())).thenReturn(COMPANY_PROFILE_DOCUMENT);


        companyProfileService.processCompanyProfile(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER,
                companyProfile);

        assertFalse(companyProfile.getData().getHasCharges());
    }
}