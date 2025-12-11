package uk.gov.companieshouse.company.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.result.UpdateResult;

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
import uk.gov.companieshouse.api.exception.BadRequestException;
import uk.gov.companieshouse.api.exception.DocumentNotFoundException;
import uk.gov.companieshouse.api.exception.ResourceStateConflictException;
import uk.gov.companieshouse.api.exception.ServiceUnavailableException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.Updated;
import uk.gov.companieshouse.api.model.company.RegisteredOfficeAddressApi;
import uk.gov.companieshouse.api.model.ukestablishments.PrivateUkEstablishmentsAddressApi;
import uk.gov.companieshouse.api.model.ukestablishments.PrivateUkEstablishmentsAddressListApi;
import uk.gov.companieshouse.company.profile.api.CompanyProfileApiService;
import uk.gov.companieshouse.company.profile.exception.ConflictException;
import uk.gov.companieshouse.company.profile.exception.ResourceNotFoundException;
import uk.gov.companieshouse.company.profile.mapper.UkEstablishmentAddressMapper;
import uk.gov.companieshouse.company.profile.model.VersionedCompanyProfileDocument;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;
import uk.gov.companieshouse.company.profile.transform.CompanyProfileTransformer;
import uk.gov.companieshouse.company.profile.util.LinkRequest;
import uk.gov.companieshouse.company.profile.util.LinkRequestFactory;
import uk.gov.companieshouse.company.profile.util.TestHelper;

@ExtendWith(MockitoExtension.class)
class CompanyProfileServiceTest {

    private static final String IS_OVERSEAS_COMPANY_FILE_DISABLED_FIELD = "isOverseasCompanyFileDisabled";
    private static final boolean IS_OVERSEAS_COMPANY_FILE_DISABLED = false;
    private static final String MOCK_COMPANY_NUMBER = "6146287";
    private static final String MOCK_CONTEXT_ID = "123456";
    private static final String MOCK_PARENT_COMPANY_NUMBER = "321033";
    private static final String ANOTHER_PARENT_COMPANY_NUMBER = "FC123456";
    private static final String MOCK_DELTA_AT = "20241129123010123789";
    private static final String UK_ESTABLISHMENT_COMPANY_NUMBER = "BR765432";
    private static final String UK_ESTABLISTMENT_COMPANY_NUMBER_2 = "BR123456";
    private static final String EXPECTED_NOT_FOUND_EXCEPTION_MESSAGE = "Company profile %s not found";

    @Mock
    CompanyProfileRepository companyProfileRepository;
    @Mock
    MongoTemplate mongoTemplate;
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

    @InjectMocks
    @Spy
    CompanyProfileService companyProfileService;

    static TestHelper testHelper;

    private static CompanyProfile companyProfile;
    private static VersionedCompanyProfileDocument companyProfileDocument;
    private static Links existingLinks;
    private static VersionedCompanyProfileDocument existingCompanyProfileDocument;
    private static VersionedCompanyProfileDocument existingParentCompanyProfileDocument;
    private static List<VersionedCompanyProfileDocument> ukEstablishmentsTestInput;
    private static List<UkEstablishment> ukEstablishmentsTestOutput;
    private static VersionedCompanyProfileDocument existingUkEstablishmentCompany;
    private static VersionedCompanyProfileDocument existingParentCompany;
    private static final String DELTA_AT = "20241129123010123789";

    private final LinkRequest chargesLinkRequest = new LinkRequest(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER,
            CHARGES_LINK_TYPE, CHARGES_DELTA_TYPE, CHARGES_GET);
    private final LinkRequest insolvencyLinkRequest = new LinkRequest(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER,
            INSOLVENCY_LINK_TYPE, INSOLVENCY_DELTA_TYPE, INSOLVENCY_GET);
    private static final String CHARGES_LINK = String.format("/company/%s/charges", MOCK_COMPANY_NUMBER);
    private static final String INSOLVENCY_LINK = String.format("/company/%s/insolvency", MOCK_COMPANY_NUMBER);

    @BeforeAll
    static void setUp() throws IOException {
        testHelper = new TestHelper();
        
        companyProfile = testHelper.createCompanyProfileObject();
        companyProfileDocument = testHelper.createCompanyProfileDocument();
        existingLinks = createExistingLinks();
        existingCompanyProfileDocument = createExistingCompanyProfile();
        existingParentCompanyProfileDocument = createExistingCompanyProfile();
        ukEstablishmentsTestInput = Arrays.asList(
                testHelper.createUkEstablishmentTestInput(MOCK_COMPANY_NUMBER + 1),
                testHelper.createUkEstablishmentTestInput(MOCK_COMPANY_NUMBER + 2));
        ukEstablishmentsTestOutput = Arrays.asList(
                testHelper.createUkEstablishmentTestOutput(MOCK_COMPANY_NUMBER + 1),
                testHelper.createUkEstablishmentTestOutput(MOCK_COMPANY_NUMBER + 2));
        existingUkEstablishmentCompany = testHelper.createCompanyProfileTypeUkEstablishment(MOCK_COMPANY_NUMBER);
        existingParentCompany = testHelper.createParentCompanyProfile(ANOTHER_PARENT_COMPANY_NUMBER);
    }

    @BeforeEach
    void beforeEach() throws NoSuchFieldException, IllegalAccessException {
        var field = CompanyProfileService.class.getDeclaredField(IS_OVERSEAS_COMPANY_FILE_DISABLED_FIELD);
        field.setAccessible(true);
        field.set(companyProfileService, IS_OVERSEAS_COMPANY_FILE_DISABLED);
    }

    @Test
    @DisplayName("When company profile is retrieved successfully then it is returned")
    void getCompanyProfile() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        companyData.setType("ltd");
        LocalDateTime localDateTime = LocalDateTime.now();
        Updated updated = mock(Updated.class);

        VersionedCompanyProfileDocument theCompanyProfileDocument = new VersionedCompanyProfileDocument(companyData, localDateTime, updated, false);
        theCompanyProfileDocument.setId(MOCK_COMPANY_NUMBER);
        theCompanyProfileDocument.getCompanyProfile().setCompanyStatus("string");

        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.of(theCompanyProfileDocument));

        VersionedCompanyProfileDocument companyProfileActual = companyProfileService.get(MOCK_COMPANY_NUMBER);

        assertEquals(theCompanyProfileDocument, companyProfileActual);
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

        VersionedCompanyProfileDocument theCompanyProfileDocument = new VersionedCompanyProfileDocument(companyData, localDateTime, updated, false);
        theCompanyProfileDocument.setId(MOCK_COMPANY_NUMBER);
        theCompanyProfileDocument.getCompanyProfile().setCompanyStatus("string");

        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.of(theCompanyProfileDocument));

        VersionedCompanyProfileDocument companyProfileActual = companyProfileService.get(MOCK_COMPANY_NUMBER);

        assertEquals(theCompanyProfileDocument, companyProfileActual);
        assertEquals("careOf", companyProfileActual.getCompanyProfile().getRegisteredOfficeAddress().getCareOf());
        assertNull(companyProfileActual.getCompanyProfile().getRegisteredOfficeAddress().getCareOfName());
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

        VersionedCompanyProfileDocument theCompanyProfileDocument = new VersionedCompanyProfileDocument(companyData, localDateTime, updated, false);
        theCompanyProfileDocument.setId(MOCK_COMPANY_NUMBER);
        theCompanyProfileDocument.getCompanyProfile().setCompanyStatus("string");

        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.of(theCompanyProfileDocument));

        VersionedCompanyProfileDocument companyProfileActual = companyProfileService.get(MOCK_COMPANY_NUMBER);

        assertEquals(theCompanyProfileDocument, companyProfileActual);
        assertEquals("careOfName", companyProfileActual.getCompanyProfile().getRegisteredOfficeAddress().getCareOf());
        assertNull(companyProfileActual.getCompanyProfile().getRegisteredOfficeAddress().getCareOfName());
    }

    @Test
    @DisplayName("When company details is retrieved successfully then it is returned")
    void getCompanyDetails() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        companyData.setCompanyName("String");
        LocalDateTime localDateTime = LocalDateTime.now();
        Updated updated = mock(Updated.class);
        VersionedCompanyProfileDocument mockCompanyProfileDocument = new VersionedCompanyProfileDocument(companyData,
                localDateTime, updated, false);
        mockCompanyProfileDocument.setId(MOCK_COMPANY_NUMBER);
        mockCompanyProfileDocument.getCompanyProfile().setCompanyStatus("String");
        CompanyDetails mockCompanyDetails = new CompanyDetails();
        mockCompanyDetails.setCompanyStatus("String");
        mockCompanyDetails.setCompanyName("String");
        mockCompanyDetails.setCompanyNumber(MOCK_COMPANY_NUMBER);
        CompanyDetails mockCompanyDetailsOP = mockCompanyDetails;

        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.of(mockCompanyProfileDocument));

        CompanyDetails companyDetailsActual =
                companyProfileService.getCompanyDetails(MOCK_COMPANY_NUMBER);

        assertEquals(mockCompanyDetailsOP, companyDetailsActual);
    }

    @Test
    @DisplayName("When no company profile is retrieved then throw ResourceNotFoundException")
    void getNoCompanyProfileReturned() {
        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.empty());

        Executable actual = () -> companyProfileService.get(MOCK_COMPANY_NUMBER);

        assertThrows(ResourceNotFoundException.class, actual);
    }

    @Test
    @DisplayName("When no company profile is retrieved then throw ResourceNotFoundException")
    void getNoCompanyDetailsReturned() {
        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.empty());

        Executable actual = () -> companyProfileService.getCompanyDetails(MOCK_COMPANY_NUMBER);

        assertThrows(ResourceNotFoundException.class, actual);
    }

    @Test
    @DisplayName("When there's a connection issue while performing the GET request then throw a "
            + "service unavailable exception")
    void getConnectionIssueServiceUnavailable() {
        when(companyProfileRepository.findById(anyString()))
                .thenThrow(new DataAccessResourceFailureException("Connection broken"));

        Assert.assertThrows(ServiceUnavailableException.class,
                () -> companyProfileService.get(MOCK_COMPANY_NUMBER));
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
    }

    @Test
    void when_insolvency_data_is_given_then_data_should_be_saved() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        companyData.setLinks(new Links());
        LocalDateTime localDateTime = LocalDateTime.now();
        Updated updated = new Updated(localDateTime,
                null, "company-profile");

        VersionedCompanyProfileDocument mockCompanyProfileDocument = new VersionedCompanyProfileDocument(companyData,
                localDateTime, updated, false);
        mockCompanyProfileDocument.setId(MOCK_COMPANY_NUMBER);
        mockCompanyProfileDocument.setCompanyProfile(companyData);

        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.of(mockCompanyProfileDocument));
        when(apiResponse.getStatusCode()).thenReturn(200);
        when(companyProfileApiService.invokeChsKafkaApi(anyString())).thenReturn(apiResponse);

        CompanyProfile companyProfileWithInsolvency = mockCompanyProfileWithoutInsolvency();
        companyProfileWithInsolvency.getData().getLinks().setInsolvency("INSOLVENCY_LINK");

        companyProfileService.updateInsolvencyLink(MOCK_COMPANY_NUMBER,
                companyProfileWithInsolvency);

        verify(mongoTemplate).save(any());
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("When there's a connection issue while performing the PATCH request then throw a "
            + "service unavailable exception")
    void patchConnectionIssueServiceUnavailable() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        companyData.setLinks(new Links());
        LocalDateTime localDateTime = LocalDateTime.now();
        Updated updated = mock(Updated.class);

        VersionedCompanyProfileDocument mockCompanyProfileDocument = new VersionedCompanyProfileDocument(companyData,
                localDateTime, updated, false);
        mockCompanyProfileDocument.setId(MOCK_COMPANY_NUMBER);
        mockCompanyProfileDocument.version(0L);

        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.of(mockCompanyProfileDocument));

        when(apiResponse.getStatusCode()).thenReturn(200);
        when(companyProfileApiService.invokeChsKafkaApi(anyString())).thenReturn(apiResponse);

        CompanyProfile companyProfileWithInsolvency = mockCompanyProfileWithoutInsolvency();
        companyProfileWithInsolvency.getData().getLinks().setInsolvency("INSOLVENCY_LINK");

        when(companyProfileRepository.save(any())).thenThrow(new DataAccessResourceFailureException("Connection broken"));
        Assert.assertThrows(ServiceUnavailableException.class,
                () -> companyProfileService.updateInsolvencyLink(MOCK_COMPANY_NUMBER,
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
                () -> companyProfileService.updateInsolvencyLink(MOCK_COMPANY_NUMBER,
                        companyProfileWithInsolvency));

        verify(apiResponse, never()).getStatusCode();
        verify(companyProfileApiService, never()).invokeChsKafkaApi(anyString());
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
                MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add exemptions link throws document not found exception")
    void addExemptionsLinkNotFound() {
        // given
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE, Links::getExemptions);
        when(linkRequestFactory.createLinkRequest(EXEMPTIONS_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format(EXPECTED_NOT_FOUND_EXCEPTION_MESSAGE, MOCK_COMPANY_NUMBER), exception.getMessage());
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
                MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getExemptions()).thenReturn(String.format("/company/%s/exemptions", MOCK_COMPANY_NUMBER));

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

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
                MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(companyProfileApiService.invokeChsKafkaApi(any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add exemptions link throws service unavailable exception when data access exception thrown during findById")
    void addExemptionsLinkDataAccessExceptionFindById() {
        // given
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE, Links::getExemptions);
        when(linkRequestFactory.createLinkRequest(EXEMPTIONS_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

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
                MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(companyProfileRepository.save(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

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
                MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getExemptions()).thenReturn(String.format("/company/%s/exemptions", MOCK_COMPANY_NUMBER));

        // when
        companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete exemptions link throws document not found exception")
    void deleteExemptionsLinkNotFound() {
        // given
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE, Links::getExemptions);
        when(linkRequestFactory.createLinkRequest(EXEMPTIONS_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format(EXPECTED_NOT_FOUND_EXCEPTION_MESSAGE, MOCK_COMPANY_NUMBER), exception.getMessage());
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
                MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

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
                MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getExemptions()).thenReturn(String.format("/company/%s/exemptions", MOCK_COMPANY_NUMBER));
        when(companyProfileApiService.invokeChsKafkaApi(any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete exemptions link throws service unavailable exception when data access exception thrown during findById")
    void deleteExemptionsLinkDataAccessExceptionFindById() {
        // given
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE, Links::getExemptions);
        when(linkRequestFactory.createLinkRequest(EXEMPTIONS_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

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
                MOCK_COMPANY_NUMBER)).thenReturn(exemptionsLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getExemptions()).thenReturn(String.format("/company/%s/exemptions", MOCK_COMPANY_NUMBER));
        when(companyProfileRepository.save(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

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
                MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add charges link throws document not found exception")
    void addChargesLinkNotFound() {
        // given
        when(linkRequestFactory.createLinkRequest(CHARGES_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format(EXPECTED_NOT_FOUND_EXCEPTION_MESSAGE, MOCK_COMPANY_NUMBER), exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Add charges link throws resource state conflict exception")
    void addChargesLinkConflict() {
        // given
        when(linkRequestFactory.createLinkRequest(CHARGES_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getCharges()).thenReturn(CHARGES_LINK);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

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
                MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(companyProfileApiService.invokeChsKafkaApi(any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add charges link throws service unavailable exception when data access exception thrown during findById")
    void addChargesLinkDataAccessExceptionFindById() {
        // given
        when(linkRequestFactory.createLinkRequest(CHARGES_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

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
                MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(companyProfileRepository.save(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

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
                MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getCharges()).thenReturn(CHARGES_LINK);

        // when
        companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete charges link throws document not found exception")
    void deleteChargesLinkNotFound() {
        // given
        when(linkRequestFactory.createLinkRequest(CHARGES_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format(EXPECTED_NOT_FOUND_EXCEPTION_MESSAGE, MOCK_COMPANY_NUMBER), exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete charges link throws resource state conflict exception")
    void deleteChargesLinkConflict() {
        // given
        when(linkRequestFactory.createLinkRequest(CHARGES_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

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
                MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getCharges()).thenReturn(CHARGES_LINK);
        when(companyProfileApiService.invokeChsKafkaApi(any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete charges link throws service unavailable exception when data access exception thrown during findById")
    void deleteChargesLinkDataAccessExceptionFindById() {
        // given
        when(linkRequestFactory.createLinkRequest(CHARGES_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

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
                MOCK_COMPANY_NUMBER)).thenReturn(chargesLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getCharges()).thenReturn(CHARGES_LINK);
        when(companyProfileRepository.save(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(CHARGES_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

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
                MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add insolvency link throws document not found exception")
    void addInsolvencyLinkNotFound() {
        // given
        when(linkRequestFactory.createLinkRequest(INSOLVENCY_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format(EXPECTED_NOT_FOUND_EXCEPTION_MESSAGE, MOCK_COMPANY_NUMBER), exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Add insolvency link throws resource state conflict exception")
    void addInsolvencyLinkConflict() {
        // given
        when(linkRequestFactory.createLinkRequest(INSOLVENCY_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getInsolvency()).thenReturn(INSOLVENCY_LINK);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

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
                MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(companyProfileApiService.invokeChsKafkaApi(any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add insolvency link throws service unavailable exception when data access exception thrown during findById")
    void addInsolvencyLinkDataAccessExceptionFindById() {
        // given
        when(linkRequestFactory.createLinkRequest(INSOLVENCY_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

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
                MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(companyProfileRepository.save(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

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
                MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getInsolvency()).thenReturn(INSOLVENCY_LINK);

        // when
        companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete insolvency link throws document not found exception")
    void deleteInsolvencyLinkNotFound() {
        // given
        when(linkRequestFactory.createLinkRequest(INSOLVENCY_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format(EXPECTED_NOT_FOUND_EXCEPTION_MESSAGE, MOCK_COMPANY_NUMBER), exception.getMessage());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @DisplayName("Delete insolvency link throws resource state conflict exception")
    void deleteInsolvencyLinkConflict() {
        // given
        when(linkRequestFactory.createLinkRequest(INSOLVENCY_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

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
                MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getInsolvency()).thenReturn(INSOLVENCY_LINK);
        when(companyProfileApiService.invokeChsKafkaApi(any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete insolvency link throws service unavailable exception when data access exception thrown during findById")
    void deleteInsolvencyLinkDataAccessExceptionFindById() {
        // given
        when(linkRequestFactory.createLinkRequest(INSOLVENCY_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

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
                MOCK_COMPANY_NUMBER)).thenReturn(insolvencyLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getInsolvency()).thenReturn(INSOLVENCY_LINK);
        when(document.getVersion()).thenReturn(0L);
        when(companyProfileRepository.save(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(INSOLVENCY_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add officers link throws document not found exception")
    void addOfficersLinkNotFound() {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE, Links::getOfficers);
        when(linkRequestFactory.createLinkRequest(OFFICERS_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format(EXPECTED_NOT_FOUND_EXCEPTION_MESSAGE, MOCK_COMPANY_NUMBER), exception.getMessage());
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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getOfficers()).thenReturn(String.format("/company/%s/officers", MOCK_COMPANY_NUMBER));

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(companyProfileApiService.invokeChsKafkaApi(any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add officer link throws service unavailable exception when data access exception thrown during findById")
    void addOfficersLinkDataAccessExceptionFindById() {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE, Links::getOfficers);
        when(linkRequestFactory.createLinkRequest(OFFICERS_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(companyProfileRepository.save(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getOfficers()).thenReturn(String.format("/company/%s/officers", MOCK_COMPANY_NUMBER));

        // when
        companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete officers link throws document not found exception")
    void deleteOfficersLinkNotFound() {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE, Links::getOfficers);
        when(linkRequestFactory.createLinkRequest(OFFICERS_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format(EXPECTED_NOT_FOUND_EXCEPTION_MESSAGE, MOCK_COMPANY_NUMBER), exception.getMessage());
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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getOfficers()).thenReturn(String.format("/company/%s/officers", MOCK_COMPANY_NUMBER));
        when(companyProfileApiService.invokeChsKafkaApi(any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete officers link throws service unavailable exception when data access exception thrown during findById")
    void deleteOfficersLinkDataAccessExceptionFindById() {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE, Links::getOfficers);
        when(linkRequestFactory.createLinkRequest(OFFICERS_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getOfficers()).thenReturn(String.format("/company/%s/officers", MOCK_COMPANY_NUMBER));
        when(document.getVersion()).thenReturn(0L);
        when(companyProfileRepository.save(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add psc statements link throws document not found exception")
    void addPscStatementsLinkNotFound() {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE,
                Links::getPersonsWithSignificantControlStatements);
        when(linkRequestFactory.createLinkRequest(PSC_STATEMENTS_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format(EXPECTED_NOT_FOUND_EXCEPTION_MESSAGE, MOCK_COMPANY_NUMBER), exception.getMessage());
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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getPersonsWithSignificantControlStatements()).thenReturn(String.format(
                "/company/%s/persons-with-significant-control-statements", MOCK_COMPANY_NUMBER));

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(companyProfileApiService.invokeChsKafkaApi(any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add psc statements link throws service unavailable exception when data access exception thrown during findById")
    void addPscStatementsLinkDataAccessExceptionFindById() {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE,
                Links::getPersonsWithSignificantControlStatements);
        when(linkRequestFactory.createLinkRequest(PSC_STATEMENTS_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(document.getVersion()).thenReturn(0L);
        when(companyProfileRepository.save(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getPersonsWithSignificantControlStatements()).thenReturn(String.format(
                "/company/%s/persons-with-significant-control-statements", MOCK_COMPANY_NUMBER));

        // when
        companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);
        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete psc statements link throws document not found exception")
    void deletePscStatementsLinkNotFound() {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE,
                Links::getPersonsWithSignificantControlStatements);
        when(linkRequestFactory.createLinkRequest(PSC_STATEMENTS_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format(EXPECTED_NOT_FOUND_EXCEPTION_MESSAGE, MOCK_COMPANY_NUMBER), exception.getMessage());
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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getPersonsWithSignificantControlStatements()).thenReturn(String.format(
                "/company/%s/persons-with-significant-control-statements", MOCK_COMPANY_NUMBER));
        when(companyProfileApiService.invokeChsKafkaApi(any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete psc statements link throws service unavailable exception when data access exception thrown during findById")
    void deletePscStatementsLinkDataAccessExceptionFindById() {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE,
                Links::getPersonsWithSignificantControlStatements);
        when(linkRequestFactory.createLinkRequest(PSC_STATEMENTS_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getPersonsWithSignificantControlStatements()).thenReturn(String.format(
                "/company/%s/persons-with-significant-control-statements", MOCK_COMPANY_NUMBER));
        when(companyProfileRepository.save(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
    }

    private CompanyProfile mockCompanyProfileWithoutInsolvency() {
        CompanyProfile theCompanyProfile = new CompanyProfile();
        Data theData = new Data();
        theData.setCompanyNumber(MOCK_COMPANY_NUMBER);

        Links theLinks = new Links();
        theLinks.setOfficers("officer");

        theData.setLinks(theLinks);
        theCompanyProfile.setData(theData);
        return theCompanyProfile;
    }

    @Test
    @DisplayName("Add psc link successfully updates MongoDB and calls chs-kafka-api")
    void addPscLink() {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_LINK_TYPE, PSC_DELTA_TYPE,
                Links::getPersonsWithSignificantControl);
        when(linkRequestFactory.createLinkRequest(PSC_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add psc link throws document not found exception")
    void addPscLinkNotFound() {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_LINK_TYPE, PSC_DELTA_TYPE,
                Links::getPersonsWithSignificantControl);
        when(linkRequestFactory.createLinkRequest(PSC_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format(EXPECTED_NOT_FOUND_EXCEPTION_MESSAGE, MOCK_COMPANY_NUMBER), exception.getMessage());
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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getPersonsWithSignificantControl()).thenReturn(String.format(
                "/company/%s/persons-with-significant-control", MOCK_COMPANY_NUMBER));

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(companyProfileApiService.invokeChsKafkaApi(any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add psc link throws service unavailable exception when data access exception thrown during findById")
    void addPscLinkDataAccessExceptionFindById() {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_LINK_TYPE, PSC_DELTA_TYPE,
                Links::getPersonsWithSignificantControl);
        when(linkRequestFactory.createLinkRequest(PSC_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(companyProfileRepository.save(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                false);

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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getPersonsWithSignificantControl()).thenReturn(String.format(
                "/company/%s/persons-with-significant-control", MOCK_COMPANY_NUMBER));

        // when
        companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);
        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete psc link throws document not found exception")
    void deletePscLinkNotFound() {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_LINK_TYPE, PSC_DELTA_TYPE,
                Links::getPersonsWithSignificantControl);
        when(linkRequestFactory.createLinkRequest(PSC_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format(EXPECTED_NOT_FOUND_EXCEPTION_MESSAGE, MOCK_COMPANY_NUMBER), exception.getMessage());
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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getPersonsWithSignificantControl()).thenReturn(String.format(
                "/company/%s/persons-with-significant-control", MOCK_COMPANY_NUMBER));
        when(companyProfileApiService.invokeChsKafkaApi(any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete psc link throws service unavailable exception when data access exception thrown during findById")
    void deletePscLinkDataAccessExceptionFindById() {
        // given
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                PSC_LINK_TYPE, PSC_DELTA_TYPE,
                Links::getPersonsWithSignificantControl);
        when(linkRequestFactory.createLinkRequest(PSC_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

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
                MOCK_COMPANY_NUMBER)).thenReturn(officersLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(existingCompanyProfileDocument));
        existingCompanyProfileDocument.version(null);
        existingCompanyProfileDocument.getCompanyProfile().getLinks().setPersonsWithSignificantControl(String.format(
                "/company/%s/persons-with-significant-control", MOCK_COMPANY_NUMBER));
        when(mongoTemplate.save(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(PSC_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoInteractions(companyProfileApiService);
    }

    @Test
    @DisplayName("Put company profile")
    void putCompanyProfileSuccessfully() {
        when(companyProfileRepository.findById(MOCK_PARENT_COMPANY_NUMBER)).thenReturn(Optional.of(existingParentCompanyProfileDocument));
        existingParentCompanyProfileDocument.getCompanyProfile().getLinks().setUkEstablishments(null);
        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenReturn(Optional.empty());
        when(companyProfileTransformer.transform(any(), any(), any()))
                .thenReturn(companyProfileDocument.version(0L));

        companyProfileService.processCompanyProfile(MOCK_COMPANY_NUMBER,
                companyProfile);

        Assertions.assertNotNull(companyProfile);
        Assertions.assertNotNull(companyProfileDocument);
        Assertions.assertNull(companyProfileDocument.getCompanyProfile().getLinks().getOverseas());
        verify(companyProfileRepository).insert(companyProfileDocument);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Put company profile fails when delta is stale")
    void putCompanyProfileThrowsConflictExceptionsWhenStaleDelta() {
        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenReturn(Optional.of(companyProfileDocument));

        Executable actual = () -> companyProfileService.processCompanyProfile(MOCK_COMPANY_NUMBER,
                companyProfile);

        assertThrows(ConflictException.class, actual);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoMoreInteractions(companyProfileRepository);
        verifyNoInteractions(companyProfileTransformer);
        verifyNoInteractions(companyProfileApiService);
    }

    @Test
    @DisplayName("Put company profile with existing links")
    void putCompanyProfileWithExistingLinksSuccessfully() {
        when(companyProfileRepository.findById(MOCK_PARENT_COMPANY_NUMBER))
                .thenReturn(Optional.of(existingParentCompanyProfileDocument));
        existingParentCompanyProfileDocument.getCompanyProfile().getLinks().setUkEstablishments(null);

        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenReturn(Optional.of(existingCompanyProfileDocument));
        when(companyProfileTransformer.transform(existingCompanyProfileDocument, companyProfile, existingLinks))
                .thenReturn(existingCompanyProfileDocument);

        companyProfileService.processCompanyProfile(MOCK_COMPANY_NUMBER,
                companyProfile);

        Assertions.assertNotNull(companyProfile);
        Assertions.assertNotNull(companyProfileDocument);
        Assertions.assertNotNull(existingCompanyProfileDocument);
        Assertions.assertNotNull(existingLinks);
        verify(companyProfileRepository).save(existingCompanyProfileDocument);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("When a company number is provided to retrieve company profile successfully then it is returned")
    void testRetrieveCompanyNumber() throws ResourceNotFoundException {
        document.setCompanyProfile(new Data());

        when(companyProfileRepository.findById(anyString())).thenReturn(Optional.of(document));

        Data result = companyProfileService.retrieveCompanyNumber(MOCK_COMPANY_NUMBER);

        assertEquals(document.getCompanyProfile(), result);
        verify(companyProfileRepository, times(1)).findById(anyString());
    }

    @Test
    @DisplayName("An empty list of company names or corporate annotations stored in Mongo should not be returned")
    void testRetrieveCompanyNumberWithEmptyList() throws ResourceNotFoundException {
        VersionedCompanyProfileDocument doc = new VersionedCompanyProfileDocument();
        Data theData = new Data();
        theData.setCompanyNumber(MOCK_COMPANY_NUMBER);
        theData.setCorporateAnnotation(Collections.emptyList());
        theData.setPreviousCompanyNames(Collections.emptyList());
        doc.setCompanyProfile(theData);

        when(companyProfileRepository.findById(anyString())).thenReturn(Optional.of(doc));

        Data result = companyProfileService.retrieveCompanyNumber(MOCK_COMPANY_NUMBER);
        assertNull(result.getCorporateAnnotation());
        assertNull(result.getPreviousCompanyNames());

        assertEquals(doc.getCompanyProfile(), result);
        verify(companyProfileRepository, times(1)).findById(anyString());
    }

    @Test
    @DisplayName("When retrieving company profile then it is returned with careOf")
    void testRetrieveCompanyNumberCareOf() throws ResourceNotFoundException {
        VersionedCompanyProfileDocument doc = new VersionedCompanyProfileDocument();
        Data theData = new Data();
        RegisteredOfficeAddress roa = new RegisteredOfficeAddress();
        roa.setCareOf("careOf");
        roa.setCareOfName("careOfName");
        theData.setRegisteredOfficeAddress(roa);
        doc.setCompanyProfile(theData);

        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenReturn(Optional.of(doc));

        Data result = companyProfileService.retrieveCompanyNumber(MOCK_COMPANY_NUMBER);

        assertEquals(doc.getCompanyProfile(), result);
        assertEquals("careOf", result.getRegisteredOfficeAddress().getCareOf());
        assertNull(result.getRegisteredOfficeAddress().getCareOfName());
        verify(companyProfileRepository, times(1)).findById(anyString());
    }

    @Test
    @DisplayName("When retrieving company profile without careOf then it is returned with careOf populated by careOfName")
    void testRetrieveCompanyNumberCareOfName() throws ResourceNotFoundException {
        VersionedCompanyProfileDocument doc = new VersionedCompanyProfileDocument();
        Data theData = new Data();
        RegisteredOfficeAddress roa = new RegisteredOfficeAddress();
        roa.setCareOfName("careOfName");
        theData.setRegisteredOfficeAddress(roa);
        doc.setCompanyProfile(theData);

        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenReturn(Optional.of(doc));

        Data result = companyProfileService.retrieveCompanyNumber(MOCK_COMPANY_NUMBER);

        assertEquals(doc.getCompanyProfile(), result);
        assertEquals("careOfName", result.getRegisteredOfficeAddress().getCareOf());
        assertNull(result.getRegisteredOfficeAddress().getCareOfName());
        verify(companyProfileRepository, times(1)).findById(anyString());
    }

    @Test
    @DisplayName("Retrieve date of cessation on a company number with dissolution date")
    void testRetrieveCompanyNumberCessationDate() throws ResourceNotFoundException {
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
    @DisplayName("Retrieve company should not return proof status")
    void testRetrieveCompanyNumberRemoveProofStatus() throws ResourceNotFoundException {
        Data companyData = new Data();
        companyData.setCompanyNumber(MOCK_COMPANY_NUMBER);
        companyData.setProofStatus("paper");

        VersionedCompanyProfileDocument doc = new VersionedCompanyProfileDocument();
        doc.setCompanyProfile(companyData);

        when(companyProfileRepository.findById(anyString())).thenReturn(Optional.of(doc));

        Data result = companyProfileService.retrieveCompanyNumber(MOCK_COMPANY_NUMBER);

        assertNull(result.getProofStatus());
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("When Resource Not Found exception is thrown and that it is handled well by the CompanyProfileService")
    void testRetrieveCompanyNumberResourceNotFoundException() {
        when(companyProfileRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> companyProfileService.retrieveCompanyNumber(MOCK_COMPANY_NUMBER));
        verify(companyProfileRepository, times(1)).findById(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("When company number is provided delete company profile")
    void testDeleteCompanyProfile() {
        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenReturn(Optional.ofNullable(existingCompanyProfileDocument));
        companyProfileService.deleteCompanyProfile(MOCK_COMPANY_NUMBER, MOCK_DELTA_AT);

        verify(companyProfileRepository, times(1)).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileRepository, times(1)).delete(existingCompanyProfileDocument);
        verify(companyProfileService, times((0))).checkForDeleteLink(any());
    }

    @Test
    @DisplayName("Check delete link should be called when deleting Uk establishments")
    void testDeleteCompanyProfileUkEstablishments() {
        when(companyProfileRepository.findById(UK_ESTABLISHMENT_COMPANY_NUMBER)).
                thenReturn(Optional.ofNullable(existingUkEstablishmentCompany));
        when(companyProfileRepository.findById(ANOTHER_PARENT_COMPANY_NUMBER))
                .thenReturn(Optional.ofNullable(existingParentCompany));
        companyProfileService.deleteCompanyProfile(UK_ESTABLISHMENT_COMPANY_NUMBER, MOCK_DELTA_AT);

        verify(companyProfileRepository, times(1)).findById(UK_ESTABLISHMENT_COMPANY_NUMBER);
        verify(companyProfileRepository, times(1)).findById(ANOTHER_PARENT_COMPANY_NUMBER);
        verify(companyProfileService, times(1)).checkForDeleteLinkUkEstablishmentParent(any());
        verify(companyProfileRepository, times(1)).delete(existingUkEstablishmentCompany);
    }

    @Test
    @DisplayName("Check that child Uk establishment should still be deleted when parent is not found")
    void shouldStillDeleteChildUkEstablishmentWhenParentNotFound() {
        when(companyProfileRepository.findById(UK_ESTABLISHMENT_COMPANY_NUMBER))
                .thenReturn(Optional.ofNullable(existingUkEstablishmentCompany));
        when(companyProfileRepository.findById(ANOTHER_PARENT_COMPANY_NUMBER))
                .thenReturn(Optional.empty());

        companyProfileService.deleteCompanyProfile(UK_ESTABLISHMENT_COMPANY_NUMBER, MOCK_DELTA_AT);

        verify(companyProfileRepository).findById(UK_ESTABLISHMENT_COMPANY_NUMBER);
        verify(companyProfileRepository).findById(ANOTHER_PARENT_COMPANY_NUMBER);
        verify(companyProfileService).checkForDeleteLinkUkEstablishmentParent(any());
        verify(companyProfileRepository, times(1)).delete(existingUkEstablishmentCompany);
    }

    @Test
    @DisplayName("When company number is null process without error")
    void testDeleteCompanyProfileProcessesInvalidCompanyNumber() {
        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenReturn(Optional.empty());

        companyProfileService.deleteCompanyProfile(MOCK_COMPANY_NUMBER, MOCK_DELTA_AT);

        verify(companyProfileRepository, times(1)).findById(MOCK_COMPANY_NUMBER);
        verifyNoMoreInteractions(companyProfileRepository);
        verify(companyProfileService, times((0))).checkForDeleteLink(any());
        verify(companyProfileApiService).invokeChsKafkaApiWithDeleteEvent(MOCK_COMPANY_NUMBER, null);
    }

    @Test
    @DisplayName("When deltaAt is null throw bad request exception")
    void testDeleteCompanyProfileDeltaAtBadRequest() {
        // given

        // when
        Executable actual = () -> companyProfileService.deleteCompanyProfile(MOCK_COMPANY_NUMBER, null);

        // then
        assertThrows(BadRequestException.class, actual);
        verifyNoInteractions(companyProfileApiService);
        verifyNoInteractions(companyProfileRepository);
    }

    @Test
    @DisplayName("When deltaAt is stale throw conflict exception")
    void testDeleteCompanyProfileStaleDeltaAt() {
        // given
        when(companyProfileRepository.findById(anyString())).thenReturn(Optional.ofNullable(companyProfileDocument));

        // when
        Executable actual = () -> companyProfileService.deleteCompanyProfile(MOCK_COMPANY_NUMBER, "20001129123010123789");

        // then
        assertThrows(ConflictException.class, actual);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verifyNoMoreInteractions(companyProfileRepository);
        verifyNoInteractions(companyProfileApiService);
    }

    @Test
    @DisplayName("Add filing history link successfully updates MongoDB and calls chs-kafka-api")
    void addFilingHistoryLink() {
        // given
        LinkRequest filingHistoryLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                FILING_HISTORY_LINK_TYPE, FILING_HISTORY_DELTA_TYPE, Links::getFilingHistory);

        when(linkRequestFactory.createLinkRequest(anyString(), anyString())).thenReturn(filingHistoryLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        companyProfileService.processLinkRequest(FILING_HISTORY_LINK_TYPE, MOCK_COMPANY_NUMBER, false);

        // then
        verify(linkRequestFactory).createLinkRequest(FILING_HISTORY_LINK_TYPE, MOCK_COMPANY_NUMBER);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @ParameterizedTest(name = "Can file is correctly set for company type: {0} and status: {1} to {2}")
    @CsvSource({
            "ltd,active,true",
            "llp,active,true",
            "plc,active,true",
            "oversea-company,active,true",
            "private-limited-shares-section-30-exemption,active,true",
            "ltd,dissolved,false",
            "llp,dissolved,false",
            "plc,dissolved,false",
            "oversea-company,dissolved,false",
            "private-limited-shares-section-30-exemption,dissolved,false",
            "other,active,false",
            "other,dissolved,false"
    })
    void testDetermineCanFileLtd(String companyType, String companyStatus, boolean expectedCanFile) {

        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        companyData.setCompanyStatus(companyStatus);
        companyData.setType(companyType);
        VersionedCompanyProfileDocument theCompanyProfileDocument = new VersionedCompanyProfileDocument();
        theCompanyProfileDocument.setCompanyProfile(companyData);

        companyProfileService.determineCanFile(theCompanyProfileDocument);

        assertEquals(companyData.getCanFile(), expectedCanFile);
    }

    @Test
    @DisplayName("Can file set to false when company type is null")
    void testDetermineCanFileCompanyTypeNull() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        companyData.setCompanyStatus("active");
        VersionedCompanyProfileDocument theCompanyProfileDocument = new VersionedCompanyProfileDocument();
        theCompanyProfileDocument.setCompanyProfile(companyData);

        companyProfileService.determineCanFile(theCompanyProfileDocument);

        assertFalse(companyData.getCanFile());
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

        VersionedCompanyProfileDocument theCompanyProfileDocument = new VersionedCompanyProfileDocument();
        theCompanyProfileDocument.setCompanyProfile(companyData);

        companyProfileService.determineOverdue(theCompanyProfileDocument);

        assertFalse(confirmationStatement.getOverdue());
        assertFalse(nextAccounts.getOverdue());
        assertFalse(annualReturn.getOverdue());
    }

    @Test
    @DisplayName("Overdue set to true when next due fields are before current date")
    void testDetermineOverdueTrue() {
        VersionedCompanyProfileDocument theCompanyProfileDocument = companyProfileDocument;

        companyProfileService.determineOverdue(theCompanyProfileDocument);

        assertEquals(true, companyProfileDocument.getCompanyProfile().getConfirmationStatement().getOverdue());
        assertEquals(true, companyProfileDocument.getCompanyProfile().getAccounts().getNextAccounts().getOverdue());
        assertEquals(true, companyProfileDocument.getCompanyProfile().getAnnualReturn().getOverdue());
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

        VersionedCompanyProfileDocument theCompanyProfileDocument = new VersionedCompanyProfileDocument();
        theCompanyProfileDocument.setCompanyProfile(companyData);

        companyProfileService.determineOverdue(theCompanyProfileDocument);

        assertEquals(false, confirmationStatement.getOverdue());
        assertEquals(false, nextAccounts.getOverdue());
        assertEquals(false, annualReturn.getOverdue());
    }

    @Test
    @DisplayName("Add new uk establishments links successfully")
    void addNewUkEstablishmentsLinkSuccessfully() {
        VersionedCompanyProfileDocument theCompanyProfileDocument = existingCompanyProfileDocument;
        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenReturn(Optional.of(existingCompanyProfileDocument));
        existingParentCompanyProfileDocument.getCompanyProfile().getLinks().setUkEstablishments(null);

        theCompanyProfileDocument.setId(MOCK_PARENT_COMPANY_NUMBER);
        theCompanyProfileDocument.getCompanyProfile().setCompanyNumber(MOCK_PARENT_COMPANY_NUMBER);
        when(companyProfileRepository.findById(MOCK_PARENT_COMPANY_NUMBER)).thenReturn(Optional.of(existingParentCompanyProfileDocument));

        BranchCompanyDetails branchCompanyDetails = new BranchCompanyDetails();
        branchCompanyDetails.setParentCompanyNumber(MOCK_PARENT_COMPANY_NUMBER);
        CompanyProfile theCompanyProfile = companyProfile;
        theCompanyProfile.getData().setBranchCompanyDetails(branchCompanyDetails);

        companyProfileService.processCompanyProfile(MOCK_COMPANY_NUMBER,
                theCompanyProfile);

        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_PARENT_COMPANY_NUMBER);

        Assertions.assertEquals(theCompanyProfile.getData().getLinks().getOverseas(), String.format("/company/%s", MOCK_PARENT_COMPANY_NUMBER));
    }

    @Test
    @DisplayName("Put company profile with existing links")
    void putUkEstablishmentAndOverseasSuccessfully() {
        when(companyProfileRepository.findById(MOCK_PARENT_COMPANY_NUMBER))
                .thenReturn(Optional.of(existingParentCompanyProfileDocument));
        existingParentCompanyProfileDocument.getCompanyProfile().getLinks().setUkEstablishments(null);

        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenReturn(Optional.of(existingCompanyProfileDocument));
        when(companyProfileTransformer.transform(existingCompanyProfileDocument, companyProfile, existingLinks))
                .thenReturn(existingCompanyProfileDocument);

        companyProfileService.processCompanyProfile(MOCK_COMPANY_NUMBER,
                companyProfile);

        Assertions.assertNotNull(companyProfile);
        Assertions.assertNotNull(companyProfileDocument);
        Assertions.assertNotNull(existingCompanyProfileDocument);
        Assertions.assertNotNull(existingLinks);
        Assertions.assertEquals(companyProfile.getData().getLinks().getOverseas(), String.format("/company/%s", MOCK_PARENT_COMPANY_NUMBER));
        verify(companyProfileRepository).save(existingCompanyProfileDocument);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add new uk establishments links unsuccessfully and throw 503")
    void addNewUkEstablishmentsLinkUnsuccessfullyAndThrow503() {
        when(companyProfileRepository.findById(MOCK_COMPANY_NUMBER)).thenThrow(ServiceUnavailableException.class);

        assertThrows(ServiceUnavailableException.class, () -> {
            companyProfileService.processCompanyProfile(MOCK_COMPANY_NUMBER,
                    companyProfile);
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
                MOCK_COMPANY_NUMBER)).thenReturn(ukEstablishmentLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getUkEstablishments()).thenReturn(String.format(
                "/company/%s/uk-establishments", MOCK_COMPANY_NUMBER));

        // when
        companyProfileService.processLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

        // then
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileRepository).findAllByParentCompanyNumber(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Don't delete the uk establishment link due to establishments existing")
    void deleteUkEstablishmentLinkEstablishmentsExists() {
        // given
        LinkRequest ukEstablishmentLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                UK_ESTABLISHMENTS_LINK_TYPE, UK_ESTABLISHMENTS_DELTA_TYPE, Links::getUkEstablishments);
        when(linkRequestFactory.createLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(ukEstablishmentLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getUkEstablishments()).thenReturn(String.format(
                "/company/%s/uk-establishments", MOCK_COMPANY_NUMBER));
        when(companyProfileRepository.findAllByParentCompanyNumber(MOCK_COMPANY_NUMBER))
                .thenReturn(ukEstablishmentsTestInput);

        // when
        companyProfileService.processLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER,
                true);

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
                MOCK_COMPANY_NUMBER)).thenReturn(ukEstablishmentLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE,
                MOCK_COMPANY_NUMBER, true);

        // then
        Exception exception = assertThrows(DocumentNotFoundException.class, executable);
        assertEquals(String.format(EXPECTED_NOT_FOUND_EXCEPTION_MESSAGE,
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
                MOCK_COMPANY_NUMBER)).thenReturn(ukEstablishmentLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE,
                MOCK_COMPANY_NUMBER, true);

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
                MOCK_COMPANY_NUMBER)).thenReturn(ukEstablishmentLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getUkEstablishments()).thenReturn(String.format(
                "/company/%s/uk-establishments", MOCK_COMPANY_NUMBER));
        when(companyProfileApiService.invokeChsKafkaApi(any())).thenThrow(IllegalArgumentException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE,
                MOCK_COMPANY_NUMBER, true);

        // then
        assertThrows(ServiceUnavailableException.class, executable);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
        verify(companyProfileRepository).findAllByParentCompanyNumber(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete uk establishment throws service unavailable exception when data access exception thrown during findById")
    void deleteUkEstablishmentDataAccessExceptionFindById() {
        // given
        LinkRequest ukEstablishmentLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER,
                UK_ESTABLISHMENTS_LINK_TYPE, UK_ESTABLISHMENTS_DELTA_TYPE, Links::getUkEstablishments);
        when(linkRequestFactory.createLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE,
                MOCK_COMPANY_NUMBER)).thenReturn(ukEstablishmentLinkRequest);
        when(companyProfileRepository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE,
                MOCK_COMPANY_NUMBER, true);

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
                MOCK_COMPANY_NUMBER)).thenReturn(ukEstablishmentLinkRequest);
        when(companyProfileRepository.findById(any())).thenReturn(Optional.of(document));
        when(document.getCompanyProfile()).thenReturn(data);
        when(data.getLinks()).thenReturn(links);
        when(links.getUkEstablishments()).thenReturn(String.format(
                "/company/%s/uk-establishments", MOCK_COMPANY_NUMBER));
        when(companyProfileRepository.save(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> companyProfileService.processLinkRequest(UK_ESTABLISHMENTS_LINK_TYPE,
                MOCK_COMPANY_NUMBER, true);

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
        when(companyProfileTransformer.transform(any(), any(), any())).thenReturn(companyProfileDocument);

        companyProfileService.processCompanyProfile(MOCK_COMPANY_NUMBER,
                companyProfile);

        verify(companyProfileRepository).insert(companyProfileDocument);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_COMPANY_NUMBER);
        verify(companyProfileApiService).invokeChsKafkaApi(MOCK_PARENT_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Overdue not set when all fields are null")
    void testDetermineOverDueAllNull() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);

        VersionedCompanyProfileDocument theCompanyProfileDocument = new VersionedCompanyProfileDocument();
        theCompanyProfileDocument.setCompanyProfile(companyData);

        companyProfileService.determineOverdue(theCompanyProfileDocument);

        assertFalse(confirmationStatement.getOverdue());
        assertFalse(nextAccounts.getOverdue());
        assertFalse(annualReturn.getOverdue());
    }

    @Test
    @DisplayName("Returns a list of UK establishments for given parent company number")
    void testGetUKEstablishmentsReturnsCorrectData() {
        VersionedCompanyProfileDocument theCompanyProfileDocument = new VersionedCompanyProfileDocument();
        theCompanyProfileDocument.setId(MOCK_PARENT_COMPANY_NUMBER);
        // given
        when(companyProfileRepository.findById(MOCK_PARENT_COMPANY_NUMBER)).thenReturn(
                Optional.of(theCompanyProfileDocument));
        when(companyProfileRepository.findAllByParentCompanyNumber(MOCK_PARENT_COMPANY_NUMBER))
                .thenReturn(ukEstablishmentsTestInput);

        // when
        UkEstablishmentsList result = companyProfileService.getUkEstablishments(MOCK_PARENT_COMPANY_NUMBER);

        // then
        assertEquals("/company/321033", result.getLinks().getSelf());
        assertEquals("related-companies", result.getKind());
        assertEquals(ukEstablishmentsTestOutput, result.getItems());
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

        CompanyProfile profileToTransform = new CompanyProfile()
                .deltaAt(DELTA_AT);
        profileToTransform.setData(new Data());
        profileToTransform.getData().setHasBeenLiquidated(true);
        profileToTransform.getData().setHasCharges(true);
        profileToTransform.setHasMortgages(true);
        profileToTransform.getData().setCompanyNumber("6146287");
        when(companyProfileTransformer.transform(existingDoc, profileToTransform, null))
                .thenReturn(companyProfileDocument);

        CompanyProfile theCompanyProfile = new CompanyProfile()
                .deltaAt(DELTA_AT);
        theCompanyProfile.setData(new Data());
        theCompanyProfile.getData().setHasCharges(null);
        theCompanyProfile.getData().setHasBeenLiquidated(null);
        theCompanyProfile.getData().setCompanyNumber("6146287");
        companyProfileService.processCompanyProfile(MOCK_COMPANY_NUMBER,
                theCompanyProfile);
        verify(companyProfileTransformer).transform(existingDoc, profileToTransform, null);

        Assertions.assertNotNull(companyProfileDocument);
        verify(companyProfileRepository).save(companyProfileDocument);
        verify(companyProfileRepository).findById(MOCK_COMPANY_NUMBER);
    }

    @Test
    void updateCompanyProfileWhenHasChargesIsFalse() {
        CompanyProfile theCompanyProfile = new CompanyProfile()
                .deltaAt(DELTA_AT);
        Links theLinks = new Links();
        theCompanyProfile.setData(new Data());
        theCompanyProfile.getData().setLinks(theLinks);
        theCompanyProfile.getData().setHasCharges(false);
        theCompanyProfile.getData().setHasBeenLiquidated(false);
        theCompanyProfile.getData().setCompanyNumber(MOCK_COMPANY_NUMBER);

        when(companyProfileRepository.findById(anyString())).thenReturn(Optional.of(existingCompanyProfileDocument));
        when(companyProfileTransformer.transform(any(), any(), any())).thenReturn(companyProfileDocument);

        companyProfileService.processCompanyProfile(MOCK_COMPANY_NUMBER,
                theCompanyProfile);

        assertFalse(theCompanyProfile.getData().getHasCharges());
    }

    @Test
    void updateCompanyProfileWhenHasChargesIsNull() {
        CompanyProfile theCompanyProfile = new CompanyProfile()
                .deltaAt(DELTA_AT);
        Links theLinks = new Links();
        theCompanyProfile.setData(new Data());
        theCompanyProfile.getData().setLinks(theLinks);
        theCompanyProfile.getData().setHasCharges(null);
        theCompanyProfile.getData().setHasBeenLiquidated(false);
        theCompanyProfile.getData().setCompanyNumber(MOCK_COMPANY_NUMBER);

        when(companyProfileRepository.findById(anyString())).thenReturn(Optional.of(existingCompanyProfileDocument));
        when(companyProfileTransformer.transform(any(), any(), any())).thenReturn(companyProfileDocument);

        companyProfileService.processCompanyProfile(MOCK_COMPANY_NUMBER,
                theCompanyProfile);

        assertFalse(theCompanyProfile.getData().getHasCharges());
    }

    @Nested
    class GetUkEstablishmentsAddresses {

        private static List<VersionedCompanyProfileDocument> ukEstablishmentsAddressesTestInput;
        private static VersionedCompanyProfileDocument companyProfileDocument1;
        private static VersionedCompanyProfileDocument companyProfileDocument2;
        private MockedStatic<UkEstablishmentAddressMapper> ukEstablishmentAddressMapper;

        @BeforeAll
        static void setup() {
            companyProfileDocument1 = testHelper.createCompanyProfileTypeUkEstablishment(UK_ESTABLISHMENT_COMPANY_NUMBER, "open",
                    LocalDate.of(2021, 1, 1));
            companyProfileDocument2 = testHelper.createCompanyProfileTypeUkEstablishment(UK_ESTABLISTMENT_COMPANY_NUMBER_2,
                    "open", LocalDate.of(2020, 1, 1));

            ukEstablishmentsAddressesTestInput = Arrays.asList(
                    companyProfileDocument2,
                    companyProfileDocument1
            );
        }

        @BeforeEach
        void initStaticMock() {
            ukEstablishmentAddressMapper = mockStatic(UkEstablishmentAddressMapper.class);
        }

        @AfterEach
        void closeStaticMock() {
            if (ukEstablishmentAddressMapper != null) {
                ukEstablishmentAddressMapper.close();
            }
        }

        @Test
        void testGetUkEstablishmentsAddressesWithNoUkEstiablishments() {
            RegisteredOfficeAddressApi registeredOfficeAddressApi1 = new RegisteredOfficeAddressApi();
            registeredOfficeAddressApi1.setAddressLine1("line 1");
            registeredOfficeAddressApi1.setPostalCode("AB1 2CD");

            RegisteredOfficeAddressApi registeredOfficeAddressApi2 = new RegisteredOfficeAddressApi();
            registeredOfficeAddressApi2.setAddressLine1("line 2");
            registeredOfficeAddressApi2.setPostalCode("EF3 4GH");

            PrivateUkEstablishmentsAddressApi ukEstablishmentAddress1 = new PrivateUkEstablishmentsAddressApi();
            ukEstablishmentAddress1.setCompanyNumber(UK_ESTABLISHMENT_COMPANY_NUMBER);
            ukEstablishmentAddress1.setRegisteredOfficeAddress(registeredOfficeAddressApi1);

            PrivateUkEstablishmentsAddressApi ukEstablishmentAddress2 = new PrivateUkEstablishmentsAddressApi();
            ukEstablishmentAddress2.setCompanyNumber(UK_ESTABLISTMENT_COMPANY_NUMBER_2);
            ukEstablishmentAddress2.setRegisteredOfficeAddress(registeredOfficeAddressApi2);

            VersionedCompanyProfileDocument parentCompanyProfileDocument = new VersionedCompanyProfileDocument();
            parentCompanyProfileDocument.setId(MOCK_PARENT_COMPANY_NUMBER);
            // given
            when(companyProfileRepository.findById(MOCK_PARENT_COMPANY_NUMBER)).thenReturn(
                    Optional.of(parentCompanyProfileDocument));
            when(companyProfileRepository
                    .findAllOpenCompanyProfilesByParentNumberSortedByCreation(MOCK_PARENT_COMPANY_NUMBER))
                    .thenReturn(ukEstablishmentsAddressesTestInput);
            ukEstablishmentAddressMapper.when(() -> UkEstablishmentAddressMapper
                            .mapToUkEstablishmentAddress(companyProfileDocument1))
                    .thenReturn(ukEstablishmentAddress1);
            ukEstablishmentAddressMapper.when(() -> UkEstablishmentAddressMapper
                            .mapToUkEstablishmentAddress(companyProfileDocument2))
                    .thenReturn(ukEstablishmentAddress2);

            PrivateUkEstablishmentsAddressListApi addresses = companyProfileService
                    .getUkEstablishmentsAddresses(MOCK_PARENT_COMPANY_NUMBER);
            assertEquals(2, addresses.getData().size());
            assertEquals(ukEstablishmentAddress2, addresses.getData().getFirst());
            assertEquals(ukEstablishmentAddress1, addresses.getData().getLast());
        }
    }


}