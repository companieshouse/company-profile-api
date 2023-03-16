package uk.gov.companieshouse.company.profile.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.EXEMPTIONS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.EXEMPTIONS_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.OFFICERS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.OFFICERS_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_STATEMENTS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_STATEMENTS_LINK_TYPE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.company.profile.config.ApplicationConfig;
import uk.gov.companieshouse.company.profile.config.ExceptionHandlerConfig;
import uk.gov.companieshouse.company.profile.exceptions.BadRequestException;
import uk.gov.companieshouse.company.profile.exceptions.DocumentNotFoundException;
import uk.gov.companieshouse.company.profile.exceptions.ResourceStateConflictException;
import uk.gov.companieshouse.company.profile.exceptions.ServiceUnavailableException;
import uk.gov.companieshouse.company.profile.model.CompanyProfileDocument;
import uk.gov.companieshouse.company.profile.model.Updated;
import uk.gov.companieshouse.company.profile.service.CompanyProfileService;
import uk.gov.companieshouse.company.profile.util.LinkRequest;
import uk.gov.companieshouse.company.profile.util.LinkRequestFactory;
import uk.gov.companieshouse.logging.Logger;

// Need to set context configuration otherwise non-dependent beans (the repository) will be created.
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = CompanyProfileController.class)
@ContextConfiguration(classes = {CompanyProfileController.class, ExceptionHandlerConfig.class})
@Import({ApplicationConfig.class})
class CompanyProfileControllerTest {
    private static final String MOCK_COMPANY_NUMBER = "6146287";
    private static final String COMPANY_URL = String.format("/company/%s/links", MOCK_COMPANY_NUMBER);
    private static final String EXEMPTIONS_LINK_URL = String.format("/company/%s/links/exemptions", MOCK_COMPANY_NUMBER);
    private static final String DELETE_EXEMPTIONS_LINK_URL = String.format("/company/%s/links/exemptions/delete", MOCK_COMPANY_NUMBER);
    private static final String OFFICERS_LINK_URL = String.format("/company/%s/links/officers", MOCK_COMPANY_NUMBER);
    private static final String DELETE_OFFICERS_LINK_URL = String.format("/company/%s/links/officers/delete", MOCK_COMPANY_NUMBER);
    private static final String PSC_STATEMENTS_LINK_URL = String.format(
            "/company/%s/links/persons-with-significant-control-statements", MOCK_COMPANY_NUMBER);
    private static final String DELETE_PSC_STATEMENTS_LINK_URL = String.format(
            "/company/%s/links/persons-with-significant-control-statements/delete", MOCK_COMPANY_NUMBER);

    @MockBean
    private Logger logger;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompanyProfileService companyProfileService;

    @MockBean
    private LinkRequestFactory linkRequestFactory;

    private Gson gson = new Gson();

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
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE);
        when(linkRequestFactory.createExemptionsLinkRequest(any(), any())).thenReturn(exemptionsLinkRequest);
        doNothing().when(companyProfileService).addExemptionsLink(any());

        mockMvc.perform(patch(EXEMPTIONS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .header("x-request-id", "123456"))
                .andExpect(status().isOk());
        verify(companyProfileService).addExemptionsLink(exemptionsLinkRequest);
    }

    @Test
    @DisplayName("Add exemptions link request returns 404 not found when document not found exception is thrown")
    void addExemptionsLinkNotFound() throws Exception {
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE);
        when(linkRequestFactory.createExemptionsLinkRequest(any(), any())).thenReturn(exemptionsLinkRequest);
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).addExemptionsLink(any());

        mockMvc.perform(patch(EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
        verify(companyProfileService).addExemptionsLink(exemptionsLinkRequest);
    }

    @Test
    @DisplayName("Add exemptions link request returns 409 not found when resource state conflict exception is thrown")
    void addExemptionsLinkConflict() throws Exception {
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE);
        when(linkRequestFactory.createExemptionsLinkRequest(any(), any())).thenReturn(exemptionsLinkRequest);
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).addExemptionsLink(any());

        mockMvc.perform(patch(EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isConflict());
        verify(companyProfileService).addExemptionsLink(exemptionsLinkRequest);
    }

    @Test()
    @DisplayName("Add exemptions link request returns 503 service unavailable when a service unavailable exception is thrown")
    void addExemptionsLinkServiceUnavailable() throws Exception {
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE);
        when(linkRequestFactory.createExemptionsLinkRequest(any(), any())).thenReturn(exemptionsLinkRequest);
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).addExemptionsLink(any());

        mockMvc.perform(patch(EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).addExemptionsLink(exemptionsLinkRequest);
    }

    @Test()
    @DisplayName("Add exemptions link request returns 500 internal server error when a runtime exception is thrown")
    void addExemptionsLinkInternalServerError() throws Exception {
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE);
        when(linkRequestFactory.createExemptionsLinkRequest(any(), any())).thenReturn(exemptionsLinkRequest);
        doThrow(new RuntimeException()).when(companyProfileService).addExemptionsLink(any());

        mockMvc.perform(patch(EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).addExemptionsLink(exemptionsLinkRequest);
    }

    @Test
    @DisplayName("Delete company exemptions link")
    void deleteExemptionsLink() throws Exception {
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE);
        when(linkRequestFactory.createExemptionsLinkRequest(any(), any())).thenReturn(exemptionsLinkRequest);
        doNothing().when(companyProfileService).deleteExemptionsLink(any());

        mockMvc.perform(patch(DELETE_EXEMPTIONS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());
        verify(companyProfileService).deleteExemptionsLink(exemptionsLinkRequest);
    }

    @Test
    @DisplayName("Delete exemptions link request returns 404 not found when document not found exception is thrown")
    void deleteExemptionsLinkNotFound() throws Exception {
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE);
        when(linkRequestFactory.createExemptionsLinkRequest(any(), any())).thenReturn(exemptionsLinkRequest);
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).deleteExemptionsLink(any());

        mockMvc.perform(patch(DELETE_EXEMPTIONS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
        verify(companyProfileService).deleteExemptionsLink(exemptionsLinkRequest);
    }

    @Test
    @DisplayName("Delete exemptions link request returns 409 not found when resource state conflict exception is thrown")
    void deleteExemptionsLinkConflict() throws Exception {
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE);
        when(linkRequestFactory.createExemptionsLinkRequest(any(), any())).thenReturn(exemptionsLinkRequest);
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).deleteExemptionsLink(any());

        mockMvc.perform(patch(DELETE_EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isConflict());
        verify(companyProfileService).deleteExemptionsLink(exemptionsLinkRequest);
    }

    @Test()
    @DisplayName("Delete exemptions link request returns 503 service unavailable when a service unavailable exception is thrown")
    void deleteExemptionsLinkServiceUnavailable() throws Exception {
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE);
        when(linkRequestFactory.createExemptionsLinkRequest(any(), any())).thenReturn(exemptionsLinkRequest);
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).deleteExemptionsLink(any());

        mockMvc.perform(patch(DELETE_EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).deleteExemptionsLink(exemptionsLinkRequest);
    }

    @Test()
    @DisplayName("Delete exemptions link request returns 500 internal server error when a runtime exception is thrown")
    void deleteExemptionsLinkInternalServerError() throws Exception {
        LinkRequest exemptionsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE);
        when(linkRequestFactory.createExemptionsLinkRequest(any(), any())).thenReturn(exemptionsLinkRequest);
        doThrow(new RuntimeException())
                .when(companyProfileService).deleteExemptionsLink(any());

        mockMvc.perform(patch(DELETE_EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).deleteExemptionsLink(exemptionsLinkRequest);
    }

    @Test
    @DisplayName("Add officers link")
    void addOfficersLink() throws Exception {
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE);
        when(linkRequestFactory.createOfficersLinkRequest(any(), any())).thenReturn(officersLinkRequest);
        doNothing().when(companyProfileService).addOfficersLink(any());

        mockMvc.perform(patch(OFFICERS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());
        verify(companyProfileService).addOfficersLink(officersLinkRequest);
    }

    @Test
    @DisplayName("Add officers link request returns 404 not found when document not found exception is thrown")
    void addOfficersLinkNotFound() throws Exception {
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE);
        when(linkRequestFactory.createOfficersLinkRequest(any(), any())).thenReturn(officersLinkRequest);
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).addOfficersLink(any());

        mockMvc.perform(patch(OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
)                .andExpect(status().isNotFound());
        verify(companyProfileService).addOfficersLink(officersLinkRequest);
    }

    @Test
    @DisplayName("Add officers link request returns 409 not found when resource state conflict exception is thrown")
    void addOfficersLinkConflict() throws Exception {
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE);
        when(linkRequestFactory.createOfficersLinkRequest(any(), any())).thenReturn(officersLinkRequest);
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).addOfficersLink(any());

        mockMvc.perform(patch(OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isConflict());
        verify(companyProfileService).addOfficersLink(officersLinkRequest);
    }

    @Test()
    @DisplayName("Add officers link request returns 503 service unavailable when a service unavailable exception is thrown")
    void addOfficersLinkServiceUnavailable() throws Exception {
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE);
        when(linkRequestFactory.createOfficersLinkRequest(any(), any())).thenReturn(officersLinkRequest);
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).addOfficersLink(any());

        mockMvc.perform(patch(OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).addOfficersLink(officersLinkRequest);
    }

    @Test()
    @DisplayName("Add officers link request returns 500 internal server error when a runtime exception is thrown")
    void addOfficersLinkInternalServerError() throws Exception {
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE);
        when(linkRequestFactory.createOfficersLinkRequest(any(), any())).thenReturn(officersLinkRequest);
        doThrow(new RuntimeException()).when(companyProfileService).addOfficersLink(any());

        mockMvc.perform(patch(OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).addOfficersLink(officersLinkRequest);
    }

    @Test
    @DisplayName("Delete company officers link")
    void deleteOfficersLink() throws Exception {
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE);
        when(linkRequestFactory.createOfficersLinkRequest(any(), any())).thenReturn(officersLinkRequest);
        doNothing().when(companyProfileService).deleteOfficersLink(any());

        mockMvc.perform(patch(DELETE_OFFICERS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());
        verify(companyProfileService).deleteOfficersLink(officersLinkRequest);
    }

    @Test
    @DisplayName("Delete officers link request returns 404 not found when document not found exception is thrown")
    void deleteOfficersLinkNotFound() throws Exception {
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE);
        when(linkRequestFactory.createOfficersLinkRequest(any(), any())).thenReturn(officersLinkRequest);
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).deleteOfficersLink(any());

        mockMvc.perform(patch(DELETE_OFFICERS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
        verify(companyProfileService).deleteOfficersLink(officersLinkRequest);
    }

    @Test
    @DisplayName("Delete officers link request returns 409 not found when resource state conflict exception is thrown")
    void deleteOfficersLinkConflict() throws Exception {
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE);
        when(linkRequestFactory.createOfficersLinkRequest(any(), any())).thenReturn(officersLinkRequest);
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).deleteOfficersLink(any());

        mockMvc.perform(patch(DELETE_OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isConflict());
        verify(companyProfileService).deleteOfficersLink(officersLinkRequest);
    }

    @Test()
    @DisplayName("Delete officers link request returns 503 service unavailable when a service unavailable exception is thrown")
    void deleteOfficersLinkServiceUnavailable() throws Exception {
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE);
        when(linkRequestFactory.createOfficersLinkRequest(any(), any())).thenReturn(officersLinkRequest);
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).deleteOfficersLink(any());

        mockMvc.perform(patch(DELETE_OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).deleteOfficersLink(officersLinkRequest);
    }

    @Test()
    @DisplayName("Delete officers link request returns 500 internal server error when a runtime exception is thrown")
    void deleteOfficersLinkInternalServerError() throws Exception {
        LinkRequest officersLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE);
        when(linkRequestFactory.createOfficersLinkRequest(any(), any())).thenReturn(officersLinkRequest);
        doThrow(new RuntimeException())
                .when(companyProfileService).deleteOfficersLink(any());

        mockMvc.perform(patch(DELETE_OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).deleteOfficersLink(officersLinkRequest);
    }

    @Test
    @DisplayName("add PSC Statements link")
    void addPscStatementsLink() throws Exception {
        LinkRequest pscStatementsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE);
        when(linkRequestFactory.createPscStatementsLinkRequest(any(), any())).thenReturn(pscStatementsLinkRequest);
        doNothing().when(companyProfileService).addPscStatementsLink(any());

        mockMvc.perform(patch(PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());
        verify(companyProfileService).addPscStatementsLink(pscStatementsLinkRequest);
    }

    @Test
    @DisplayName("add PSC Statements request returns 404 not found when document not found exception is thrown")
    void addPscStatementsLinkNotFound() throws Exception {
        LinkRequest pscStatementsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE);
        when(linkRequestFactory.createPscStatementsLinkRequest(any(), any())).thenReturn(pscStatementsLinkRequest);
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).addPscStatementsLink(any());

        mockMvc.perform(patch(PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
        verify(companyProfileService).addPscStatementsLink(pscStatementsLinkRequest);
    }

    @Test
    @DisplayName("add PSC Statements request returns 409 not found when resource state conflict exception is thrown")
    void addPscStatementsLinkConflict() throws Exception {
        LinkRequest pscStatementsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE);
        when(linkRequestFactory.createPscStatementsLinkRequest(any(), any())).thenReturn(pscStatementsLinkRequest);
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).addPscStatementsLink(any());

        mockMvc.perform(patch(PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isConflict());
        verify(companyProfileService).addPscStatementsLink(pscStatementsLinkRequest);
    }

    @Test
    @DisplayName("add PSC Statements link request returns 503 service unavailable when a service unavailable exception is thrown")
    void addPscStatementsLinkServiceUnavailable() throws Exception {
        LinkRequest pscStatementsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE);
        when(linkRequestFactory.createPscStatementsLinkRequest(any(), any())).thenReturn(pscStatementsLinkRequest);
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).addPscStatementsLink(any());

        mockMvc.perform(patch(PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).addPscStatementsLink(pscStatementsLinkRequest);
    }

    @Test
    @DisplayName("add PSC Statements link request returns 503 service unavailable when a service unavailable exception is thrown")
    void addPscStatementsLinkInternalServerError() throws Exception {
        LinkRequest pscStatementsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE);
        when(linkRequestFactory.createPscStatementsLinkRequest(any(), any())).thenReturn(pscStatementsLinkRequest);
        doThrow(new RuntimeException())
                .when(companyProfileService).addPscStatementsLink(any());

        mockMvc.perform(patch(PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).addPscStatementsLink(pscStatementsLinkRequest);
    }
    @Test
    @DisplayName("Delete PSC Statements link")
    void deletePscStatementsLink() throws Exception {
        LinkRequest pscStatementsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE);
        when(linkRequestFactory.createPscStatementsLinkRequest(any(), any())).thenReturn(pscStatementsLinkRequest);
        doNothing().when(companyProfileService).deletePscStatementsLink(any());

        mockMvc.perform(patch(DELETE_PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());
        verify(companyProfileService).deletePscStatementsLink(pscStatementsLinkRequest);
    }

    @Test
    @DisplayName("Delete PSC Statements request returns 404 not found when document not found exception is thrown")
    void deletePscStatementsLinkNotFound() throws Exception {
        LinkRequest pscStatementsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE);
        when(linkRequestFactory.createPscStatementsLinkRequest(any(), any())).thenReturn(pscStatementsLinkRequest);
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).deletePscStatementsLink(any());

        mockMvc.perform(patch(DELETE_PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
        verify(companyProfileService).deletePscStatementsLink(pscStatementsLinkRequest);
    }

    @Test
    @DisplayName("Delete PSC Statements request returns 409 not found when resource state conflict exception is thrown")
    void deletePscStatementsLinkConflict() throws Exception {
        LinkRequest pscStatementsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE);
        when(linkRequestFactory.createPscStatementsLinkRequest(any(), any())).thenReturn(pscStatementsLinkRequest);
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).deletePscStatementsLink(any());

        mockMvc.perform(patch(DELETE_PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isConflict());
        verify(companyProfileService).deletePscStatementsLink(pscStatementsLinkRequest);
    }

    @Test
    @DisplayName("Delete PSC Statements link request returns 503 service unavailable when a service unavailable exception is thrown")
    void deletePscStatementsLinkServiceUnavailable() throws Exception {
        LinkRequest pscStatementsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE);
        when(linkRequestFactory.createPscStatementsLinkRequest(any(), any())).thenReturn(pscStatementsLinkRequest);
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).deletePscStatementsLink(any());

        mockMvc.perform(patch(DELETE_PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).deletePscStatementsLink(pscStatementsLinkRequest);
    }

    @Test
    @DisplayName("Delete PSC Statements link request returns 503 service unavailable when a service unavailable exception is thrown")
    void deletePscStatementsLinkInternalServerError() throws Exception {
        LinkRequest pscStatementsLinkRequest = new LinkRequest("123456", MOCK_COMPANY_NUMBER, PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE);
        when(linkRequestFactory.createPscStatementsLinkRequest(any(), any())).thenReturn(pscStatementsLinkRequest);
        doThrow(new RuntimeException())
                .when(companyProfileService).deletePscStatementsLink(any());

        mockMvc.perform(patch(DELETE_PSC_STATEMENTS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).deletePscStatementsLink(pscStatementsLinkRequest);
    }


}