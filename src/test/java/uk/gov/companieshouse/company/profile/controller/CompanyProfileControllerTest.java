package uk.gov.companieshouse.company.profile.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.exception.BadRequestException;
import uk.gov.companieshouse.api.exception.DocumentNotFoundException;
import uk.gov.companieshouse.api.exception.ResourceNotFoundException;
import uk.gov.companieshouse.api.exception.ResourceStateConflictException;
import uk.gov.companieshouse.api.exception.ServiceUnavailableException;
import uk.gov.companieshouse.api.model.CompanyProfileDocument;
import uk.gov.companieshouse.api.model.Updated;
import uk.gov.companieshouse.company.profile.adapter.LocalDateTypeAdapter;
import uk.gov.companieshouse.company.profile.config.ApplicationConfig;
import uk.gov.companieshouse.company.profile.config.ExceptionHandlerConfig;
import uk.gov.companieshouse.company.profile.service.CompanyProfileService;
import uk.gov.companieshouse.company.profile.util.TestHelper;
import uk.gov.companieshouse.logging.Logger;

// Need to set context configuration otherwise non-dependent beans (the repository) will be created.
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = CompanyProfileController.class)
@ContextConfiguration(classes = {CompanyProfileController.class, ExceptionHandlerConfig.class})
@Import({ApplicationConfig.class})
class CompanyProfileControllerTest {
    private static final String MOCK_COMPANY_NUMBER = "6146287";
    private final TestHelper testHelper = new TestHelper();
    private static final String COMPANY_URL = String.format("/company/%s/links", MOCK_COMPANY_NUMBER);
    private static final String EXEMPTIONS_LINK_URL = String.format("/company/%s/links/exemptions", MOCK_COMPANY_NUMBER);
    private static final String DELETE_EXEMPTIONS_LINK_URL = String.format("/company/%s/links/exemptions/delete", MOCK_COMPANY_NUMBER);
    private static final String OFFICERS_LINK_URL = String.format("/company/%s/links/officers", MOCK_COMPANY_NUMBER);
    private static final String DELETE_OFFICERS_LINK_URL = String.format("/company/%s/links/officers/delete", MOCK_COMPANY_NUMBER);
    private static final String PSC_STATEMENTS_LINK_URL = String.format(
            "/company/%s/links/persons-with-significant-control-statements", MOCK_COMPANY_NUMBER);
    private static final String DELETE_PSC_STATEMENTS_LINK_URL = String.format(
            "/company/%s/links/persons-with-significant-control-statements/delete", MOCK_COMPANY_NUMBER);
    private static final String FILING_HISTORY_LINK_URL = String.format("/company/%s/links/filing-history", MOCK_COMPANY_NUMBER);
    private static final String PUT_COMPANY_PROFILE_URL = String.format(
            "/company/test/%s", MOCK_COMPANY_NUMBER);

    private static final String GET_COMPANY_URL = String.format(
            "/company/test/{company_number}");
    private static final String DELETE_COMPANY_URL = String.format(
            "/company/{company_number}");

    private static final String DELETE_COMPANY_PROFILE_URL = String.format("/company/%s", MOCK_COMPANY_NUMBER);

    @MockBean
    private Logger logger;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
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

        CompanyProfileDocument mockCompanyProfileDocument = new CompanyProfileDocument(companyData, localDateTime, updated, false);

        when(companyProfileService.get(MOCK_COMPANY_NUMBER)).thenReturn(Optional.of(mockCompanyProfileDocument));

        mockMvc.perform(get(COMPANY_URL).header("ERIC-Identity", "SOME_IDENTITY").header("ERIC-Identity-Type", "key"))
                .andExpect(status().isOk())
                .andExpect(content().string(objectMapper.writeValueAsString(mockCompanyProfile)));
    }

    @Test
    @DisplayName(
            "Company Profile GET request returns a 404 Resource Not found response when no company profile found")
    void getCompanyProfileNotFound() throws Exception {
        when(companyProfileService.get(MOCK_COMPANY_NUMBER)).thenReturn(Optional.empty());

        mockMvc.perform(get(COMPANY_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test()
    @DisplayName("Company Profile GET request returns a 500 Internal Server Error")
    void getCompanyProfileInternalServerError() throws Exception {
        when(companyProfileService.get(any())).thenThrow(RuntimeException.class);

        mockMvc.perform(get(COMPANY_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key"))
                .andExpect(status().isInternalServerError());
    }

    @Test()
    @DisplayName("Company Profile GET request returns a 400 Bad Request Exception")
    void getCompanyProfileBadRequest() throws Exception {
        when(companyProfileService.get(any()))
                .thenThrow(new BadRequestException("Bad request - data in wrong format"));

        mockMvc.perform(get(COMPANY_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key"))
                .andExpect(status().isBadRequest());
    }

    @Test()
    @DisplayName("Company Profile GET request returns a 503 Service Unavailable")
    void getCompanyProfileServiceUnavailable() throws Exception {
        when(companyProfileService.get(any()))
                .thenThrow(new ServiceUnavailableException("Service unavailable - connection issue"));

        mockMvc.perform(get(COMPANY_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key"))
                .andExpect(status().isServiceUnavailable());
    }


    @Test
    @DisplayName("Company Profile PATCH request")
    void callCompanyProfilePatch() throws Exception {
        CompanyProfile request = new CompanyProfile();

        doNothing().when(companyProfileService).updateInsolvencyLink(anyString(), anyString(),
                isA(CompanyProfile.class));

        mockMvc.perform(patch(COMPANY_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .content(gson.toJson(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Company Profile PATCH request returns a 404 Not Found when company profile not found")
    void callCompanyProfilePatchNotFound() throws Exception {
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).updateInsolvencyLink(anyString(), anyString(), any());

        mockMvc.perform(patch(COMPANY_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .content(gson.toJson(new CompanyProfile())))
                .andExpect(status().isNotFound());
    }

    @Test()
    @DisplayName("Company Profile PATCH request returns a 400 Bad Request Exception")
    void patchCompanyProfileBadRequest() throws Exception {
        doThrow(new BadRequestException("Bad request - data in wrong format"))
                .when(companyProfileService).updateInsolvencyLink(anyString(), anyString(), any());

        mockMvc.perform(patch(COMPANY_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .content(gson.toJson(new CompanyProfile())))
                .andExpect(status().isBadRequest());
    }

    @Test()
    @DisplayName("Company Profile PATCH request returns a 503 Service Unavailable")
    void patchCompanyProfileServiceUnavailable() throws Exception {
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).updateInsolvencyLink(anyString(), anyString(), any());

        mockMvc.perform(patch(COMPANY_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .content(gson.toJson(new CompanyProfile())))
                .andExpect(status().isServiceUnavailable());
    }

    @Test()
    @DisplayName("Company Profile PATCH request returns a 500 Internal Server Error")
    void patchCompanyProfileInternalServerError() throws Exception {
        doThrow(new RuntimeException()).when(companyProfileService).updateInsolvencyLink(anyString(), anyString(), any());

        mockMvc.perform(patch(COMPANY_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .content(gson.toJson(new CompanyProfile())))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Add company exemptions link")
    void addExemptionsLink() throws Exception {
        doNothing().when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(EXEMPTIONS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .header("x-request-id", "123456"))
                .andExpect(status().isOk());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Add exemptions link request returns 404 not found when document not found exception is thrown")
    void addExemptionsLinkNotFound() throws Exception {
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Add exemptions link request returns 409 not found when resource state conflict exception is thrown")
    void addExemptionsLinkConflict() throws Exception {
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isConflict());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test()
    @DisplayName("Add exemptions link request returns 503 service unavailable when a service unavailable exception is thrown")
    void addExemptionsLinkServiceUnavailable() throws Exception {
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test()
    @DisplayName("Add exemptions link request returns 500 internal server error when a runtime exception is thrown")
    void addExemptionsLinkInternalServerError() throws Exception {
        doThrow(new RuntimeException()).when(companyProfileService)
                .processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete company exemptions link")
    void deleteExemptionsLink() throws Exception {
        doNothing()
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_EXEMPTIONS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete exemptions link request returns 404 not found when document not found exception is thrown")
    void deleteExemptionsLinkNotFound() throws Exception {
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_EXEMPTIONS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete exemptions link request returns 409 not found when resource state conflict exception is thrown")
    void deleteExemptionsLinkConflict() throws Exception {
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isConflict());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test()
    @DisplayName("Delete exemptions link request returns 503 service unavailable when a service unavailable exception is thrown")
    void deleteExemptionsLinkServiceUnavailable() throws Exception {
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test()
    @DisplayName("Delete exemptions link request returns 500 internal server error when a runtime exception is thrown")
    void deleteExemptionsLinkInternalServerError() throws Exception {
        doThrow(new RuntimeException())
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Add officers link")
    void addOfficersLink() throws Exception {
        doNothing().when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(OFFICERS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Add officers link request returns 404 not found when document not found exception is thrown")
    void addOfficersLinkNotFound() throws Exception {
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(OFFICERS_LINK_URL)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "123456")
                .header("ERIC-Identity", "SOME_IDENTITY")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
        ).andExpect(status().isNotFound());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Add officers link request returns 409 not found when resource state conflict exception is thrown")
    void addOfficersLinkConflict() throws Exception {
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isConflict());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test()
    @DisplayName("Add officers link request returns 503 service unavailable when a service unavailable exception is thrown")
    void addOfficersLinkServiceUnavailable() throws Exception {
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test()
    @DisplayName("Add officers link request returns 500 internal server error when a runtime exception is thrown")
    void addOfficersLinkInternalServerError() throws Exception {
        doThrow(new RuntimeException()).when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete company officers link")
    void deleteOfficersLink() throws Exception {
        doNothing().when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_OFFICERS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete officers link request returns 404 not found when document not found exception is thrown")
    void deleteOfficersLinkNotFound() throws Exception {
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_OFFICERS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete officers link request returns 409 not found when resource state conflict exception is thrown")
    void deleteOfficersLinkConflict() throws Exception {
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isConflict());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test()
    @DisplayName("Delete officers link request returns 503 service unavailable when a service unavailable exception is thrown")
    void deleteOfficersLinkServiceUnavailable() throws Exception {
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test()
    @DisplayName("Delete officers link request returns 500 internal server error when a runtime exception is thrown")
    void deleteOfficersLinkInternalServerError() throws Exception {
        doThrow(new RuntimeException())
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("add PSC Statements link")
    void addPscStatementsLink() throws Exception {
        doNothing().when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Add Filing History link")
    void addFilingHistoryLink() throws Exception {
        mockMvc.perform(patch(FILING_HISTORY_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());

        verify(companyProfileService).processLinkRequest("filing-history", MOCK_COMPANY_NUMBER, "123456", false);
    }

    @Test
    @DisplayName("add PSC Statements request returns 404 not found when document not found exception is thrown")
    void addPscStatementsLinkNotFound() throws Exception {
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("add PSC Statements request returns 409 not found when resource state conflict exception is thrown")
    void addPscStatementsLinkConflict() throws Exception {
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isConflict());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("add PSC Statements link request returns 503 service unavailable when a service unavailable exception is thrown")
    void addPscStatementsLinkServiceUnavailable() throws Exception {
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("add PSC Statements link request returns 503 service unavailable when a service unavailable exception is thrown")
    void addPscStatementsLinkInternalServerError() throws Exception {
        doThrow(new RuntimeException())
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete PSC Statements link")
    void deletePscStatementsLink() throws Exception {
        doNothing().when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete PSC Statements request returns 404 not found when document not found exception is thrown")
    void deletePscStatementsLinkNotFound() throws Exception {
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete PSC Statements request returns 409 not found when resource state conflict exception is thrown")
    void deletePscStatementsLinkConflict() throws Exception {
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isConflict());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete PSC Statements link request returns 503 service unavailable when a service unavailable exception is thrown")
    void deletePscStatementsLinkServiceUnavailable() throws Exception {
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Delete PSC Statements link request returns 503 service unavailable when a service unavailable exception is thrown")
    void deletePscStatementsLinkInternalServerError() throws Exception {
        doThrow(new RuntimeException())
                .when(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());

        mockMvc.perform(patch(DELETE_PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).processLinkRequest(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Put Company Profile")
    void callPutCompanyProfile() throws Exception {
        doNothing().when(companyProfileService).processCompanyProfile(anyString(), anyString(), isA(CompanyProfile.class));

        mockMvc.perform(put(PUT_COMPANY_PROFILE_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .content(testHelper.createJsonCompanyProfilePayload()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Retrieve a company profile when sending a GET request")
    void testSearchCompanyProfile() throws Exception {
        Data mockData = new Data();
        mockData.setCompanyNumber(MOCK_COMPANY_NUMBER);

        ResponseEntity<Data> expectedResponse = new ResponseEntity<>(mockData, HttpStatus.OK);

        when(companyProfileService.retrieveCompanyNumber(MOCK_COMPANY_NUMBER)).thenReturn(mockData);

        mockMvc.perform(MockMvcRequestBuilders.get(GET_COMPANY_URL, MOCK_COMPANY_NUMBER)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(companyProfileService, times(1)).retrieveCompanyNumber(MOCK_COMPANY_NUMBER);

    }

    @Test
    @DisplayName("Return 401 when no api key is present")
    void deleteCompanyProfileWhenNoApiKeyPresent() throws Exception {
        mockMvc.perform(delete(DELETE_COMPANY_PROFILE_URL)).andExpect(status().isUnauthorized());

        verify(companyProfileService
                , times(0)).deleteCompanyProfile(MOCK_COMPANY_NUMBER);
    }


    @Test
    @DisplayName("Return 200 and delete company profile")
    void deleteCompanyProfile() throws Exception {
        doNothing().when(companyProfileService).deleteCompanyProfile(MOCK_COMPANY_NUMBER);

        mockMvc.perform(delete(DELETE_COMPANY_PROFILE_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());

        verify(companyProfileService).deleteCompanyProfile(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Return 404 when no company profile is found")
    void deleteCompanyProfileNotFound() throws Exception {

        doThrow(ResourceNotFoundException.class).when(companyProfileService).deleteCompanyProfile(MOCK_COMPANY_NUMBER);

        mockMvc.perform(delete(DELETE_COMPANY_PROFILE_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());

        verify(companyProfileService).deleteCompanyProfile(MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Return 503 when service is unavailable")
    void deleteCompanyProfileWhenServiceIsUnavailable() throws Exception {
        doThrow(ServiceUnavailableException.class).when(companyProfileService).deleteCompanyProfile(MOCK_COMPANY_NUMBER);

        mockMvc.perform(delete(DELETE_COMPANY_PROFILE_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());

        verify(companyProfileService).deleteCompanyProfile(MOCK_COMPANY_NUMBER);
    }

}
