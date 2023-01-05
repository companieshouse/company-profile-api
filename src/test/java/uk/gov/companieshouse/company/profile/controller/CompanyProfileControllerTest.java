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

    @MockBean
    private Logger logger;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompanyProfileService companyProfileService;

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

        mockMvc.perform(patch(COMPANY_URL).header("ERIC-Identity", "SOME_IDENTITY").header("ERIC-Identity-Type", "key").contentType(APPLICATION_JSON).header("x-request-id", "123456")
                .content(gson.toJson(request))).andExpect(status().isOk());
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
                        .content(gson.toJson(new CompanyProfile())))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Add company exemptions link")
    void addExemptionsLink() throws Exception {
        doNothing().when(companyProfileService).addLink(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(patch(EXEMPTIONS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456"))
                .andExpect(status().isOk());
        verify(companyProfileService).addLink("123456", MOCK_COMPANY_NUMBER, "exemptions", "exemption_delta");
    }

    @Test
    @DisplayName("Add exemptions link request returns 404 not found when document not found exception is thrown")
    void addExemptionsLinkNotFound() throws Exception {
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).addLink(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(patch(EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key"))
                .andExpect(status().isNotFound());
        verify(companyProfileService).addLink("123456", MOCK_COMPANY_NUMBER, "exemptions", "exemption_delta");
    }

    @Test
    @DisplayName("Add exemptions link request returns 409 not found when resource state conflict exception is thrown")
    void addExemptionsLinkConflict() throws Exception {
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).addLink(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(patch(EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key"))
                .andExpect(status().isConflict());
        verify(companyProfileService).addLink("123456", MOCK_COMPANY_NUMBER, "exemptions", "exemption_delta");
    }

    @Test()
    @DisplayName("Add exemptions link request returns 503 service unavailable when a service unavailable exception is thrown")
    void addExemptionsLinkServiceUnavailable() throws Exception {
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).addLink(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(patch(EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).addLink("123456", MOCK_COMPANY_NUMBER, "exemptions", "exemption_delta");
    }

    @Test()
    @DisplayName("Add exemptions link request returns 500 internal server error when a runtime exception is thrown")
    void addExemptionsLinkInternalServerError() throws Exception {
        doThrow(new RuntimeException()).when(companyProfileService).addLink(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(patch(EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).addLink("123456", MOCK_COMPANY_NUMBER, "exemptions", "exemption_delta");
    }

    @Test
    @DisplayName("Delete company exemptions link")
    void deleteExemptionsLink() throws Exception {
        doNothing().when(companyProfileService).deleteExemptionsLink(anyString(), anyString());

        mockMvc.perform(patch(DELETE_EXEMPTIONS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456"))
                .andExpect(status().isOk());
        verify(companyProfileService).deleteExemptionsLink("123456", MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete exemptions link request returns 404 not found when document not found exception is thrown")
    void deleteExemptionsLinkNotFound() throws Exception {
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).deleteExemptionsLink(anyString(), anyString());

        mockMvc.perform(patch(DELETE_EXEMPTIONS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456"))
                .andExpect(status().isNotFound());
        verify(companyProfileService).deleteExemptionsLink("123456", MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete exemptions link request returns 409 not found when resource state conflict exception is thrown")
    void deleteExemptionsLinkConflict() throws Exception {
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).deleteExemptionsLink(anyString(), anyString());

        mockMvc.perform(patch(DELETE_EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key"))
                .andExpect(status().isConflict());
        verify(companyProfileService).deleteExemptionsLink("123456", MOCK_COMPANY_NUMBER);
    }

    @Test()
    @DisplayName("Delete exemptions link request returns 503 service unavailable when a service unavailable exception is thrown")
    void deleteExemptionsLinkServiceUnavailable() throws Exception {
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).deleteExemptionsLink(anyString(), anyString());

        mockMvc.perform(patch(DELETE_EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).deleteExemptionsLink("123456", MOCK_COMPANY_NUMBER);
    }

    @Test()
    @DisplayName("Delete exemptions link request returns 500 internal server error when a runtime exception is thrown")
    void deleteExemptionsLinkInternalServerError() throws Exception {
        doThrow(new RuntimeException())
                .when(companyProfileService).deleteExemptionsLink(anyString(), anyString());

        mockMvc.perform(patch(DELETE_EXEMPTIONS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).deleteExemptionsLink("123456", MOCK_COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Add officers link")
    void addOfficersLink() throws Exception {
        doNothing().when(companyProfileService).addLink(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(patch(OFFICERS_LINK_URL)
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key")
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456"))
                .andExpect(status().isOk());
        verify(companyProfileService).addLink("123456", MOCK_COMPANY_NUMBER, "officers", "officer_delta");
    }

    @Test
    @DisplayName("Add officers link request returns 404 not found when document not found exception is thrown")
    void addOfficersLinkNotFound() throws Exception {
        doThrow(new DocumentNotFoundException("Not Found"))
                .when(companyProfileService).addLink(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(patch(OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key"))
                .andExpect(status().isNotFound());
        verify(companyProfileService).addLink("123456", MOCK_COMPANY_NUMBER, "officers", "officer_delta");
    }

    @Test
    @DisplayName("Add officers link request returns 409 not found when resource state conflict exception is thrown")
    void addOfficersLinkConflict() throws Exception {
        doThrow(new ResourceStateConflictException("Conflict in resource state"))
                .when(companyProfileService).addLink(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(patch(OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key"))
                .andExpect(status().isConflict());
        verify(companyProfileService).addLink("123456", MOCK_COMPANY_NUMBER, "officers", "officer_delta");
    }

    @Test()
    @DisplayName("Add officers link request returns 503 service unavailable when a service unavailable exception is thrown")
    void addOfficersLinkServiceUnavailable() throws Exception {
        doThrow(new ServiceUnavailableException("Service unavailable - connection issue"))
                .when(companyProfileService).addLink(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(patch(OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key"))
                .andExpect(status().isServiceUnavailable());
        verify(companyProfileService).addLink("123456", MOCK_COMPANY_NUMBER, "officers", "officer_delta");
    }

    @Test()
    @DisplayName("Add officers link request returns 500 internal server error when a runtime exception is thrown")
    void addOfficersLinkInternalServerError() throws Exception {
        doThrow(new RuntimeException()).when(companyProfileService).addLink(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(patch(OFFICERS_LINK_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456")
                        .header("ERIC-Identity", "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "key"))
                .andExpect(status().isInternalServerError());
        verify(companyProfileService).addLink("123456", MOCK_COMPANY_NUMBER, "officers", "officer_delta");
    }
}