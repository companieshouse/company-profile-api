package uk.gov.companieshouse.company.profile.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.api.model.company.CompanyProfileApi;
import uk.gov.companieshouse.company.profile.service.CompanyProfileService;

// Need to set context configuration otherwise non-dependent beans (the repository) will be created.
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = CompanyProfileController.class)
@ContextConfiguration(classes = CompanyProfileController.class)
class CompanyProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompanyProfileService companyProfileService;

    @Test
    @DisplayName("Retrieve a company profile containing a given company number")
    void getCompanyProfile() throws Exception {
        CompanyProfileApi companyProfile = new CompanyProfileApi();
        companyProfile.setCompanyNumber("123456");
        when(companyProfileService.get("123456")).thenReturn(Optional.of(companyProfile));

        String companyUrl = String.format("/company/%s", "123456");

        mockMvc.perform(get(companyUrl))
                .andExpect(status().isOk())
                .andExpect(content().string(objectMapper.writeValueAsString(companyProfile)));

    }

    @Test
    @DisplayName(
            "Given a company number with no matching company profile return a not found response")
    void getCompanyProfileNotFound() throws Exception {
        when(companyProfileService.get("123456")).thenReturn(Optional.empty());

        String companyUrl = String.format("/company/%s", "123456");

        mockMvc.perform(get(companyUrl))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }
}