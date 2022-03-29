package uk.gov.companieshouse.company.profile.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.NoSuchElementException;
import java.util.Optional;

import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.company.profile.model.CompanyProfileDocument;
import uk.gov.companieshouse.company.profile.service.CompanyProfileService;

// Need to set context configuration otherwise non-dependent beans (the repository) will be created.
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = CompanyProfileController.class)
@ContextConfiguration(classes = CompanyProfileController.class)
class CompanyProfileControllerTest {
    private static final String MOCK_COMPANY_NUMBER = "6146287";
    private static final String COMPANY_URL = String.format("/company/%s", MOCK_COMPANY_NUMBER);

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

        CompanyProfileDocument mockCompanyProfileDocument = new CompanyProfileDocument(companyData);

        when(companyProfileService.get(MOCK_COMPANY_NUMBER)).thenReturn(Optional.of(mockCompanyProfileDocument));

        mockMvc.perform(get(COMPANY_URL))
                .andExpect(status().isOk())
                .andExpect(content().string(objectMapper.writeValueAsString(mockCompanyProfile.getData())));
    }

    @Test
    @DisplayName(
            "Given a company number with no matching company profile return a not found response")
    void getCompanyProfileNotFound() throws Exception {
        when(companyProfileService.get(MOCK_COMPANY_NUMBER)).thenReturn(Optional.empty());

        mockMvc.perform(get(COMPANY_URL))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test()
    @DisplayName("Search for bankrupt officers - returns a 500 INTERNAL SERVER ERROR")
    void getCompanyProfileInternalServerError() throws Exception {
        when(companyProfileService.get(any())).thenThrow(RuntimeException.class);

        assertThatThrownBy(() ->
                mockMvc.perform(get(COMPANY_URL))
                        .andExpect(status().isInternalServerError())
                        .andExpect(content().string(""))
        ).hasCause(new RuntimeException());
    }

    @Test
    @DisplayName("Company Profile PATCH request")
    public void callCompanyProfilePatch() throws Exception {
        CompanyProfile request = new CompanyProfile();
        String url = String.format("/company/%s/links", "02588581");

        doNothing().when(companyProfileService).updateInsolvencyLink(anyString(), anyString(), isA(CompanyProfile.class));

        mockMvc.perform(patch(url).contentType(APPLICATION_JSON).header("x-request-id", "123456")
                .content(gson.toJson(request))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Company Profile PATCH request, not found")
    public void callCompanyProfilePatchNotFound() throws Exception {
        CompanyProfile request = new CompanyProfile();
        String url = String.format("/company/%s/links", "02588581");

        doThrow(new NoSuchElementException()).when(companyProfileService).updateInsolvencyLink(anyString(), anyString(), any());

        mockMvc.perform(patch(url).contentType(APPLICATION_JSON).header("x-request-id", "123456")
                .content(gson.toJson(request))).andExpect(status().isNotFound());
    }
}