package uk.gov.companieshouse.company.profile.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.company.UkEstablishmentsList;
import uk.gov.companieshouse.api.exception.BadRequestException;
import uk.gov.companieshouse.api.exception.DocumentNotFoundException;
import uk.gov.companieshouse.api.exception.ResourceStateConflictException;
import uk.gov.companieshouse.api.exception.ServiceUnavailableException;
import uk.gov.companieshouse.api.model.Updated;
import uk.gov.companieshouse.api.model.company.RegisteredOfficeAddressApi;
import uk.gov.companieshouse.api.model.ukestablishments.PrivateUkEstablishmentsAddressApi;
import uk.gov.companieshouse.api.model.ukestablishments.PrivateUkEstablishmentsAddressListApi;
import uk.gov.companieshouse.company.profile.adapter.LocalDateTypeAdapter;
import uk.gov.companieshouse.company.profile.config.ApplicationConfig;
import uk.gov.companieshouse.company.profile.config.ExceptionHandlerConfig;
import uk.gov.companieshouse.company.profile.exception.ResourceNotFoundException;
import uk.gov.companieshouse.company.profile.model.VersionedCompanyProfileDocument;
import uk.gov.companieshouse.company.profile.service.CompanyProfileService;
import uk.gov.companieshouse.company.profile.util.TestHelper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// Need to set context configuration otherwise non-dependent beans (the repository) will be created.
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = CompanyProfileController.class)
@ContextConfiguration(classes = {CompanyProfileController.class, ExceptionHandlerConfig.class})
@Import({ApplicationConfig.class})
class CompanyProfileControllerTest {
    private static final String MOCK_COMPANY_NUMBER = "6146287";
    private static final String MOCK_PARENT_COMPANY_NUMBER = "FR123456";
    private static final String MOCK_DELTA_AT = "20241129123010123789";
    private final TestHelper testHelper = new TestHelper();
    private static final String COMPANY_PROFILE_URL = String.format("/company/%s", MOCK_COMPANY_NUMBER);
    private static final String PUT_COMPANY_PROFILE_URL = String.format("/company/%s/internal", MOCK_COMPANY_NUMBER);
    private static final String COMPANY_LINKS_URL = String.format("/company/%s/links", MOCK_COMPANY_NUMBER);
    private static final String COMPANY_DETAILS_URL = String.format("/company/%s/company-detail", MOCK_COMPANY_NUMBER);
    private static final String EXEMPTIONS_LINK_URL = String.format("/company/%s/links/exemptions", MOCK_COMPANY_NUMBER);
    private static final String DELETE_EXEMPTIONS_LINK_URL = String.format("/company/%s/links/exemptions/delete", MOCK_COMPANY_NUMBER);
    private static final String OFFICERS_LINK_URL = String.format("/company/%s/links/officers", MOCK_COMPANY_NUMBER);
    private static final String DELETE_OFFICERS_LINK_URL = String.format("/company/%s/links/officers/delete", MOCK_COMPANY_NUMBER);
    private static final String PSC_STATEMENTS_LINK_URL = String.format(
            "/company/%s/links/persons-with-significant-control-statements", MOCK_COMPANY_NUMBER);
    private static final String DELETE_PSC_STATEMENTS_LINK_URL = String.format(
            "/company/%s/links/persons-with-significant-control-statements/delete", MOCK_COMPANY_NUMBER);
    private static final String FILING_HISTORY_LINK_URL = String.format("/company/%s/links/filing-history", MOCK_COMPANY_NUMBER);
    private static final String UK_ESTABLISHMENTS_LINK_URL = String.format("/company/%s/links/uk-establishments", MOCK_COMPANY_NUMBER);
    
    private static final String GET_UK_ESTABLISHMENTS_URL = String.format(
            "/company/%s/uk-establishments", MOCK_PARENT_COMPANY_NUMBER);
    private static final String GET_UK_ESTABLISHMENTS_ADDRESSES_URL = String.format("/company/%s/uk-establishments/addresses", MOCK_COMPANY_NUMBER);
            
    private static final String DELETE_COMPANY_PROFILE_URL = String.format("/company/%s/internal", MOCK_COMPANY_NUMBER);

    private static final String X_REQUEST_ID = "123456";
    private static final String ERIC_IDENTITY = "Test-Identity";
    private static final String ERIC_IDENTITY_TYPE = "key";
    private static final String ERIC_PRIVILEGES = "*";
    private static final String ERIC_AUTH = "internal-app";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CompanyProfileService companyProfileService;

    @InjectMocks
    private CompanyProfileController companyProfileController;

    private Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
            .create();

    @Test
    @DisplayName("Retrieve a company profile containing a given company number")
    void getCompanyProfile() throws Exception {
        CompanyProfile mockCompanyProfile = new CompanyProfile();
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        mockCompanyProfile.setData(companyData);
        LocalDateTime localDateTime = LocalDateTime.now();
        Updated updated = mock(Updated.class);

        VersionedCompanyProfileDocument mockCompanyProfileDocument = new VersionedCompanyProfileDocument(companyData, localDateTime, updated, false);

        when(companyProfileService.get(MOCK_COMPANY_NUMBER)).thenReturn(mockCompanyProfileDocument);

        mockMvc.perform(get(COMPANY_LINKS_URL).header("ERIC-Identity", ERIC_IDENTITY).header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().string(objectMapper.writeValueAsString(mockCompanyProfile)));
    }

    @Test
    @DisplayName("Retrieve a company profile containing a given company number and throw 401")
    void getCompanyProfileAndThrow401() throws Exception {
        CompanyProfile mockCompanyProfile = new CompanyProfile();
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        mockCompanyProfile.setData(companyData);
        LocalDateTime localDateTime = LocalDateTime.now();
        Updated updated = mock(Updated.class);

        VersionedCompanyProfileDocument mockCompanyProfileDocument = new VersionedCompanyProfileDocument(companyData, localDateTime, updated, false);

        when(companyProfileService.get(MOCK_COMPANY_NUMBER)).thenReturn(mockCompanyProfileDocument);

        mockMvc.perform(get(COMPANY_LINKS_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Retrieve a company details containing a given company number")
    void getCompanyDetail() throws Exception {
        CompanyDetails mockCompanyDetails = new CompanyDetails();
        mockCompanyDetails.setCompanyStatus("String");
        mockCompanyDetails.setCompanyName("String");
        mockCompanyDetails.setCompanyNumber(MOCK_COMPANY_NUMBER);

        Optional<CompanyDetails> mockCompanyDetailsOP = Optional.of(mockCompanyDetails);

        when(companyProfileService.getCompanyDetails(MOCK_COMPANY_NUMBER)).thenReturn(mockCompanyDetails);

        mockMvc.perform(get(COMPANY_DETAILS_URL).header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE))
                .andExpect(content().string(objectMapper.writeValueAsString(mockCompanyDetailsOP.get())))
                .andExpect(status().isOk());

    }

    @Test
    @DisplayName(
            "Company Profile GET request returns a 404 Resource Not found response when no company profile found")
    void getCompanyProfileNotFound() throws Exception {
        when(companyProfileService.get(MOCK_COMPANY_NUMBER)).thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(get(COMPANY_LINKS_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName(
            "Company Detail GET request returns a 404 Resource Not found response when no company profile found")
    void getCompanyDetailsNotFound() throws Exception {
        when(companyProfileService.getCompanyDetails(MOCK_COMPANY_NUMBER)).thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(get(COMPANY_DETAILS_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE))
                .andExpect(status().isNotFound());
    }

    @Test()
    @DisplayName("Company Profile GET request returns a 500 Internal Server Error")
    void getCompanyProfileInternalServerError() throws Exception {
        when(companyProfileService.get(any())).thenThrow(RuntimeException.class);

        mockMvc.perform(get(COMPANY_LINKS_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE))
                .andExpect(status().isInternalServerError());
    }

    @Test()
    @DisplayName("Company Detail GET request returns a 500 Internal Server Error")
    void getCompanyDetailsInternalServerError() throws Exception {
        when(companyProfileService.getCompanyDetails(any())).thenThrow(RuntimeException.class);

        mockMvc.perform(get(COMPANY_DETAILS_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE))
                .andExpect(status().isInternalServerError());
    }

    @Test()
    @DisplayName("Company Profile GET request returns a 400 Bad Request Exception")
    void getCompanyProfileBadRequest() throws Exception {
        when(companyProfileService.get(any()))
                .thenThrow(new BadRequestException("Bad request - data in wrong format"));

        mockMvc.perform(get(COMPANY_LINKS_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE))
                .andExpect(status().isBadRequest());
    }

    @Test()
    @DisplayName("Company Profile GET request returns a 503 Service Unavailable")
    void getCompanyProfileServiceUnavailable() throws Exception {
        when(companyProfileService.get(any()))
                .thenThrow(new ServiceUnavailableException("Service unavailable - connection issue"));

        mockMvc.perform(get(COMPANY_LINKS_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE))
                .andExpect(status().isServiceUnavailable());
    }


    @Test
    @DisplayName("Company Profile PATCH request")
    void callCompanyProfilePatch() throws Exception {
        CompanyProfile request = new CompanyProfile();

        doNothing().when(companyProfileService).updateInsolvencyLink(anyString(),
                isA(CompanyProfile.class));

        mockMvc.perform(patch(COMPANY_LINKS_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .content(gson.toJson(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Company Profile PATCH request returns a 404 Not Found when company profile not found")
    void callCompanyProfilePatchNotFound() throws Exception {
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).updateInsolvencyLink(anyString(), any());

        mockMvc.perform(patch(COMPANY_LINKS_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .content(gson.toJson(new CompanyProfile())))
                .andExpect(status().isNotFound());
    }

    @Test()
    @DisplayName("Company Profile PATCH request returns a 400 Bad Request Exception")
    void patchCompanyProfileBadRequest() throws Exception {
        doThrow(new BadRequestException("Bad request - data in wrong format"))
                .when(companyProfileService).updateInsolvencyLink(anyString(), any());

        mockMvc.perform(patch(COMPANY_LINKS_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .content(gson.toJson(new CompanyProfile())))
                .andExpect(status().isBadRequest());
    }

    @Test()
    @DisplayName("Company Profile PATCH request returns a 503 Service Unavailable")
    void patchCompanyProfileServiceUnavailable() throws Exception {
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).updateInsolvencyLink(anyString(), any());

        mockMvc.perform(patch(COMPANY_LINKS_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .content(gson.toJson(new CompanyProfile())))
                .andExpect(status().isServiceUnavailable());
    }

    @Test()
    @DisplayName("Company Profile PATCH request returns a 500 Internal Server Error")
    void patchCompanyProfileInternalServerError() throws Exception {
        doThrow(new RuntimeException()).when(companyProfileService).updateInsolvencyLink(anyString(), any());

        mockMvc.perform(patch(COMPANY_LINKS_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .content(gson.toJson(new CompanyProfile())))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Add company exemptions link")
    void addExemptionsLink() throws Exception {
        doNothing().when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(EXEMPTIONS_LINK_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .header("x-request-id", X_REQUEST_ID))
                .andExpect(status().isOk());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Add exemptions link request returns 404 not found when document not found exception is thrown")
    void addExemptionsLinkNotFound() throws Exception {
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Add exemptions link request returns 409 not found when resource state conflict exception is thrown")
    void addExemptionsLinkConflict() throws Exception {
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isConflict());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test()
    @DisplayName("Add exemptions link request returns 503 service unavailable when a service unavailable exception is thrown")
    void addExemptionsLinkServiceUnavailable() throws Exception {
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test()
    @DisplayName("Add exemptions link request returns 500 internal server error when a runtime exception is thrown")
    void addExemptionsLinkInternalServerError() throws Exception {
        doThrow(new RuntimeException()).when(companyProfileService)
                .processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete company exemptions link")
    void deleteExemptionsLink() throws Exception {
        doNothing()
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_EXEMPTIONS_LINK_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete exemptions link request returns 404 not found when document not found exception is thrown")
    void deleteExemptionsLinkNotFound() throws Exception {
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_EXEMPTIONS_LINK_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete exemptions link request returns 409 not found when resource state conflict exception is thrown")
    void deleteExemptionsLinkConflict() throws Exception {
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isConflict());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test()
    @DisplayName("Delete exemptions link request returns 503 service unavailable when a service unavailable exception is thrown")
    void deleteExemptionsLinkServiceUnavailable() throws Exception {
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test()
    @DisplayName("Delete exemptions link request returns 500 internal server error when a runtime exception is thrown")
    void deleteExemptionsLinkInternalServerError() throws Exception {
        doThrow(new RuntimeException())
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Add officers link")
    void addOfficersLink() throws Exception {
        doNothing().when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(OFFICERS_LINK_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Add officers link request returns 404 not found when document not found exception is thrown")
    void addOfficersLinkNotFound() throws Exception {
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(OFFICERS_LINK_URL)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", X_REQUEST_ID)
                .header("ERIC-Identity", ERIC_IDENTITY)
                .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
        ).andExpect(status().isNotFound());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Add officers link request returns 409 not found when resource state conflict exception is thrown")
    void addOfficersLinkConflict() throws Exception {
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isConflict());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test()
    @DisplayName("Add officers link request returns 503 service unavailable when a service unavailable exception is thrown")
    void addOfficersLinkServiceUnavailable() throws Exception {
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test()
    @DisplayName("Add officers link request returns 500 internal server error when a runtime exception is thrown")
    void addOfficersLinkInternalServerError() throws Exception {
        doThrow(new RuntimeException()).when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete company officers link")
    void deleteOfficersLink() throws Exception {
        doNothing().when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_OFFICERS_LINK_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete officers link request returns 404 not found when document not found exception is thrown")
    void deleteOfficersLinkNotFound() throws Exception {
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_OFFICERS_LINK_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete officers link request returns 409 not found when resource state conflict exception is thrown")
    void deleteOfficersLinkConflict() throws Exception {
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isConflict());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test()
    @DisplayName("Delete officers link request returns 503 service unavailable when a service unavailable exception is thrown")
    void deleteOfficersLinkServiceUnavailable() throws Exception {
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test()
    @DisplayName("Delete officers link request returns 500 internal server error when a runtime exception is thrown")
    void deleteOfficersLinkInternalServerError() throws Exception {
        doThrow(new RuntimeException())
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("add PSC Statements link")
    void addPscStatementsLink() throws Exception {
        doNothing().when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Add Filing History link")
    void addFilingHistoryLink() throws Exception {
        mockMvc.perform(patch(FILING_HISTORY_LINK_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());

        verify(companyProfileService).processLinkRequest("filing-history", MOCK_COMPANY_NUMBER, false);
    }

    @Test
    @DisplayName("add PSC Statements request returns 404 not found when document not found exception is thrown")
    void addPscStatementsLinkNotFound() throws Exception {
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("add PSC Statements request returns 409 not found when resource state conflict exception is thrown")
    void addPscStatementsLinkConflict() throws Exception {
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isConflict());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("add PSC Statements link request returns 503 service unavailable when a service unavailable exception is thrown")
    void addPscStatementsLinkServiceUnavailable() throws Exception {
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("add PSC Statements link request returns 503 service unavailable when a service unavailable exception is thrown")
    void addPscStatementsLinkInternalServerError() throws Exception {
        doThrow(new RuntimeException())
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete PSC Statements link")
    void deletePscStatementsLink() throws Exception {
        doNothing().when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete PSC Statements request returns 404 not found when document not found exception is thrown")
    void deletePscStatementsLinkNotFound() throws Exception {
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete PSC Statements request returns 409 not found when resource state conflict exception is thrown")
    void deletePscStatementsLinkConflict() throws Exception {
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isConflict());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete PSC Statements link request returns 503 service unavailable when a service unavailable exception is thrown")
    void deletePscStatementsLinkServiceUnavailable() throws Exception {
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete PSC Statements link request returns 503 service unavailable when a service unavailable exception is thrown")
    void deletePscStatementsLinkInternalServerError() throws Exception {
        doThrow(new RuntimeException())
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Add uk establishments link")
    void addUkEstablishmentsLink() throws Exception {
        mockMvc.perform(patch(UK_ESTABLISHMENTS_LINK_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .header("x-request-id", X_REQUEST_ID))
                .andExpect(status().isOk());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Add uk establishments link request returns 404 not found when document not found exception is thrown")
    void addUkEstablishmentLinkNotFound() throws Exception {
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(UK_ESTABLISHMENTS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Add uk establishments link request returns 409 not found when resource state conflict exception is thrown")
    void addUkEstablishmentsLinkConflict() throws Exception {
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(UK_ESTABLISHMENTS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isConflict());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test()
    @DisplayName("Add uk establishments link request returns 503 service unavailable when a service unavailable exception is thrown")
    void addUkEstablishmentsLinkServiceUnavailable() throws Exception {
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(UK_ESTABLISHMENTS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test()
    @DisplayName("Add uk establishments link request returns 500 internal server error when a runtime exception is thrown")
    void addUkEstablishmentsLinkInternalServerError() throws Exception {
        doThrow(new RuntimeException()).when(companyProfileService)
                .processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(UK_ESTABLISHMENTS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete uk establishments link")
    void deleteUkEstablishmentsLink() throws Exception {
        mockMvc.perform(patch(UK_ESTABLISHMENTS_LINK_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete uk establishments link request returns 404 not found when document not found exception is thrown")
    void deleteUkEstablishmentsLinkNotFound() throws Exception {
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(UK_ESTABLISHMENTS_LINK_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete uk establishments link request returns 409 not found when resource state conflict exception is thrown")
    void deleteUkEstablishmentsLinkConflict() throws Exception {
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(UK_ESTABLISHMENTS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isConflict());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test()
    @DisplayName("Delete uk establishments link request returns 503 service unavailable when a service unavailable exception is thrown")
    void deleteUkEstablishmentsLinkServiceUnavailable() throws Exception {
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(UK_ESTABLISHMENTS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test()
    @DisplayName("Delete uk establishments link request returns 500 internal server error when a runtime exception is thrown")
    void deleteUkEstablishmentsLinkInternalServerError() throws Exception {
        doThrow(new RuntimeException())
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(UK_ESTABLISHMENTS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Put Company Profile")
    void callPutCompanyProfile() throws Exception {
        doNothing().when(companyProfileService)
                .processCompanyProfile(anyString(), isA(CompanyProfile.class));

        mockMvc.perform(put(PUT_COMPANY_PROFILE_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .content(testHelper.createJsonCompanyProfilePayload()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Put Company Profile and throw 400")
    void callPutCompanyProfileAndThrows400() throws Exception {
        doNothing().when(companyProfileService)
                .processCompanyProfile(anyString(), isA(CompanyProfile.class));

        mockMvc.perform(put(PUT_COMPANY_PROFILE_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Put Company Profile and throw 401")
    void callPutCompanyProfileAndReturn401() throws Exception {
        doNothing().when(companyProfileService)
                .processCompanyProfile(anyString(), isA(CompanyProfile.class));

        mockMvc.perform(put(PUT_COMPANY_PROFILE_URL)
                        .content(testHelper.createJsonCompanyProfilePayload()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Put Company Profile and throw 409")
    void callPutCompanyProfileAndThrow409() throws Exception {
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService)
                .processCompanyProfile(anyString(), isA(CompanyProfile.class));

        mockMvc.perform(put(PUT_COMPANY_PROFILE_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .content(testHelper.createJsonCompanyProfilePayload()))
                .andExpect(status().isConflict());

    }

    @Test
    @DisplayName("Put Company Profile and throw 503")
    void callPutCompanyProfileAndThrow503() throws Exception {
        doThrow(ServiceUnavailableException.class).when(companyProfileService)
                .processCompanyProfile(anyString(), isA(CompanyProfile.class));

        mockMvc.perform(put(PUT_COMPANY_PROFILE_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .content(testHelper.createJsonCompanyProfilePayload()))
                .andExpect(status().isServiceUnavailable());
    }



    @Test
    @DisplayName("Retrieve a company profile when sending a GET request")
    void testSearchCompanyProfile() throws Exception {
        Data mockData = new Data();
        mockData.setCompanyNumber(MOCK_COMPANY_NUMBER);

        ResponseEntity<Data> expectedResponse = new ResponseEntity<>(mockData, HttpStatus.OK);

        when(companyProfileService.retrieveCompanyNumber(MOCK_COMPANY_NUMBER)).thenReturn(mockData);

        mockMvc.perform(MockMvcRequestBuilders.get(COMPANY_PROFILE_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(companyProfileService, times(1)).retrieveCompanyNumber(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Retrieve a company profile when sending a GET request and Throw 401")
    void testSearchCompanyProfileAndThrow401() throws Exception {
        Data mockData = new Data();
        mockData.setCompanyNumber(MOCK_COMPANY_NUMBER);

        when(companyProfileService.retrieveCompanyNumber(MOCK_COMPANY_NUMBER)).thenReturn(mockData);

        mockMvc.perform(MockMvcRequestBuilders.get(COMPANY_PROFILE_URL))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @DisplayName("Retrieve a company profile when sending a GET request and throw 404")
    void testSearchCompanyProfileAndThrow404() throws Exception {
        Data mockData = new Data();
        mockData.setCompanyNumber(MOCK_COMPANY_NUMBER);

        doThrow(ResourceNotFoundException.class)
                .when(companyProfileService).retrieveCompanyNumber(MOCK_COMPANY_NUMBER);

        mockMvc.perform(MockMvcRequestBuilders.get(COMPANY_PROFILE_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @DisplayName("Retrieve a company profile when sending a GET request and throw 503")
    void testSearchCompanyProfileAndThrow503() throws Exception {
        Data mockData = new Data();
        mockData.setCompanyNumber(MOCK_COMPANY_NUMBER);

        doThrow(ServiceUnavailableException.class)
                .when(companyProfileService).retrieveCompanyNumber(MOCK_COMPANY_NUMBER);

        mockMvc.perform(MockMvcRequestBuilders.get(COMPANY_PROFILE_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isServiceUnavailable());
    }


    @Test
    @DisplayName("Return 401 when no api key is present")
    void deleteCompanyProfileWhenNoApiKeyPresent() throws Exception {
        mockMvc.perform(delete(DELETE_COMPANY_PROFILE_URL).header("x-request-id", X_REQUEST_ID)).andExpect(status().isUnauthorized());

        verifyNoInteractions(companyProfileService);
    }


    @Test
    @DisplayName("Return 200 and delete company profile")
    void deleteCompanyProfile() throws Exception {
        doNothing().when(companyProfileService).deleteCompanyProfile(anyString(), anyString());

        mockMvc.perform(delete(DELETE_COMPANY_PROFILE_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("X-DELTA-AT", MOCK_DELTA_AT)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());

        verify(companyProfileService).deleteCompanyProfile(MOCK_COMPANY_NUMBER, MOCK_DELTA_AT);
    }

    @Test
    @DisplayName("Return 503 when service is unavailable")
    void deleteCompanyProfileWhenServiceIsUnavailable() throws Exception {
        doThrow(ServiceUnavailableException.class).when(companyProfileService).deleteCompanyProfile(anyString(), anyString());

        mockMvc.perform(delete(DELETE_COMPANY_PROFILE_URL)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("X-DELTA-AT", MOCK_DELTA_AT)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());

        verify(companyProfileService).deleteCompanyProfile(MOCK_COMPANY_NUMBER, MOCK_DELTA_AT);
    }

    @Test
    @DisplayName("Retrieve list of uk establishments for given parent company number")
    void testGetUkEstablishmentsStatusOK() throws Exception {
        UkEstablishmentsList ukEstablishmentsList = new UkEstablishmentsList();
        ResponseEntity<UkEstablishmentsList> expectedResponse = new ResponseEntity<>(ukEstablishmentsList, HttpStatus.OK);

        when(companyProfileService.getUkEstablishments(MOCK_PARENT_COMPANY_NUMBER)).thenReturn(ukEstablishmentsList);

        mockMvc.perform(MockMvcRequestBuilders.get(GET_UK_ESTABLISHMENTS_URL, MOCK_PARENT_COMPANY_NUMBER)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("x-request-id", X_REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(companyProfileService).getUkEstablishments(MOCK_PARENT_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Failed to retrieve uk establishments due to incorrect identity type")
    void testGetUkEstablishmentsStatusUnauthorized() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(GET_UK_ESTABLISHMENTS_URL, MOCK_PARENT_COMPANY_NUMBER)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", "web")
                        .header("x-request-id", X_REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @DisplayName("Failed to retrieve uk establishments due to non existent company")
    void testGetUkEstablishmentsStatusNotFound() throws Exception {
        doThrow(ResourceNotFoundException.class).when(companyProfileService)
                .getUkEstablishments(MOCK_PARENT_COMPANY_NUMBER);

        mockMvc.perform(MockMvcRequestBuilders.get(GET_UK_ESTABLISHMENTS_URL, MOCK_PARENT_COMPANY_NUMBER)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("x-request-id", X_REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound());

        verify(companyProfileService).getUkEstablishments(MOCK_PARENT_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Failed to retrieve uk establishments due to MongoDB exception")
    void testGetUkEstablishmentsStatusServiceUnavailable() throws Exception {
        doThrow(new DataAccessException("..."){}).when(companyProfileService)
                .getUkEstablishments(MOCK_PARENT_COMPANY_NUMBER);

        mockMvc.perform(MockMvcRequestBuilders.get(GET_UK_ESTABLISHMENTS_URL, MOCK_PARENT_COMPANY_NUMBER)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("x-request-id", X_REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isServiceUnavailable());

        verify(companyProfileService).getUkEstablishments(MOCK_PARENT_COMPANY_NUMBER);
    }

    @Test
    void optionsCompanyProfileCORS() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders
                        .options(COMPANY_PROFILE_URL)
                        .header("Origin", "")
                        .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent())
            .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
            .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
            .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
            .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_MAX_AGE));
    }

    @Test
    void getCompanyProfileCORS() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders
                        .get(COMPANY_PROFILE_URL)
                        .header("Origin", "")
                        .header("ERIC-Allowed-Origin", "some-origin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Roles", ERIC_PRIVILEGES)
                        .header("ERIC-Authorised-Key-Privileges", ERIC_AUTH)
                        .header("x-request-id", X_REQUEST_ID))
            .andExpect(status().isOk())
            .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")));
    }

    @Test
    void getCompanyProfileCORSForbidden() throws Exception {

        mockMvc.perform(get(COMPANY_PROFILE_URL)
                        .header("Origin", "")
                        .header("ERIC-Allowed-Origin", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("x-request-id", X_REQUEST_ID))
            .andExpect(status().isForbidden())
            .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")));
    }

    @Test
    void putCompanyProfileCORSForbidden() throws Exception {

        mockMvc.perform(put(PUT_COMPANY_PROFILE_URL)
                        .header("Origin", "")
                        .header("ERIC-Allowed-Origin", "some-origin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("x-request-id", X_REQUEST_ID))
            .andExpect(status().isForbidden())
            .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")));
    }

    @Test
    void testGetUkEstablishmentsAddressesWithNoUkEstablishments() throws Exception {

        PrivateUkEstablishmentsAddressListApi privateUkEstablishmentsAddressList = new PrivateUkEstablishmentsAddressListApi(
            Collections.emptyList()
        );

        String result = objectMapper.writeValueAsString(Collections.emptyList());

        when(companyProfileService.getUkEstablishmentsAddresses(MOCK_COMPANY_NUMBER))
            .thenReturn(privateUkEstablishmentsAddressList);
        
        ResultActions resultActions = mockMvc.perform(get(GET_UK_ESTABLISHMENTS_ADDRESSES_URL)
                        .header("Origin", "")
                        .header("ERIC-Allowed-Origin", "some-origin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Roles", ERIC_PRIVILEGES)
                        .header("ERIC-Authorised-Key-Privileges", ERIC_AUTH)
                        .header("x-request-id", X_REQUEST_ID))
            .andExpect(status().isOk());
        
        MvcResult mvcResult =  resultActions.andReturn();

        verify(companyProfileService).getUkEstablishmentsAddresses(MOCK_COMPANY_NUMBER);
        assertEquals(result, mvcResult.getResponse().getContentAsString());
    }

    @Test
    void testGetUkEstablishmentsAddressesWithTwoUkEstablishments() throws Exception {

        RegisteredOfficeAddressApi address1 = new RegisteredOfficeAddressApi();
        address1.setAddressLine1("line 1");

        RegisteredOfficeAddressApi address2 = new RegisteredOfficeAddressApi();
        address2.setAddressLine1("line 2");

        PrivateUkEstablishmentsAddressApi ukAddress1 = new PrivateUkEstablishmentsAddressApi();
        ukAddress1.setCompanyNumber("12345678");
        ukAddress1.setRegisteredOfficeAddress(new RegisteredOfficeAddressApi());
        PrivateUkEstablishmentsAddressApi ukAddress2 = new PrivateUkEstablishmentsAddressApi();
        ukAddress2.setCompanyNumber("87654321");
        ukAddress2.setRegisteredOfficeAddress(address2);

        PrivateUkEstablishmentsAddressListApi privateUkEstablishmentsAddressList = new PrivateUkEstablishmentsAddressListApi(
            List.of(ukAddress1, ukAddress2)
        );

        String result = objectMapper.writeValueAsString(List.of(ukAddress1, ukAddress2));

        when(companyProfileService.getUkEstablishmentsAddresses(MOCK_COMPANY_NUMBER))
            .thenReturn(privateUkEstablishmentsAddressList);
        
        ResultActions resultActions = mockMvc.perform(get(GET_UK_ESTABLISHMENTS_ADDRESSES_URL)
                        .header("Origin", "")
                        .header("ERIC-Allowed-Origin", "some-origin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Roles", ERIC_PRIVILEGES)
                        .header("ERIC-Authorised-Key-Privileges", ERIC_AUTH)
                        .header("x-request-id", X_REQUEST_ID))
            .andExpect(status().isOk());
        
        MvcResult mvcResult =  resultActions.andReturn();
        verify(companyProfileService).getUkEstablishmentsAddresses(MOCK_COMPANY_NUMBER);
        assertEquals(result, mvcResult.getResponse().getContentAsString());
    }

    @Test
    void testGetUkEstablishmentsAddressesServiceUnavailable() throws Exception {

        doThrow(new DataAccessException("..."){}).when(companyProfileService)
            .getUkEstablishmentsAddresses(MOCK_COMPANY_NUMBER);

        mockMvc.perform(get(GET_UK_ESTABLISHMENTS_ADDRESSES_URL)
                        .header("Origin", "")
                        .header("ERIC-Allowed-Origin", "some-origin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Roles", ERIC_PRIVILEGES)
                        .header("ERIC-Authorised-Key-Privileges", ERIC_AUTH)
                        .header("x-request-id", X_REQUEST_ID))
            .andExpect(status().isServiceUnavailable());

        verify(companyProfileService).getUkEstablishmentsAddresses(MOCK_COMPANY_NUMBER);
    }

    @Test
    void testGetUkEstablishmentsAddressesUnauthorized() throws Exception {

        mockMvc.perform(get(GET_UK_ESTABLISHMENTS_ADDRESSES_URL)
                        .header("Origin", "")
                        .header("ERIC-Allowed-Origin", "some-origin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", "web")
                        .header("x-request-id", X_REQUEST_ID))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(companyProfileService);
    }

    @Test
    void testGetUkEstablishmentsAddressesNotFound() throws Exception {

        doThrow(ResourceNotFoundException.class).when(companyProfileService)
            .getUkEstablishmentsAddresses(MOCK_COMPANY_NUMBER);

        mockMvc.perform(get(GET_UK_ESTABLISHMENTS_ADDRESSES_URL)
                        .header("Origin", "")
                        .header("ERIC-Allowed-Origin", "some-origin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("ERIC-Identity", ERIC_IDENTITY)
                        .header("ERIC-Identity-Type", ERIC_IDENTITY_TYPE)
                        .header("ERIC-Authorised-Key-Roles", ERIC_PRIVILEGES)
                        .header("ERIC-Authorised-Key-Privileges", ERIC_AUTH)
                        .header("x-request-id", X_REQUEST_ID))
            .andExpect(status().isNotFound());

        verify(companyProfileService).getUkEstablishmentsAddresses(MOCK_COMPANY_NUMBER);
    }
}
