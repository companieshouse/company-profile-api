package uk.gov.companieshouse.company.profile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.company.profile.domain.CompanyProfileDao;
import uk.gov.companieshouse.company.profile.service.CompanyProfileService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CompanyProfileControllerITest {

    @MockBean
    private CompanyProfileService companyProfileService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Retrieve a company profile containing a given company number")
    void getCompanyProfileWithMatchingCompanyNumber() throws Exception {
        CompanyProfile mockCompanyProfile = new CompanyProfile();
        Data companyData = new Data().companyNumber("123456");
        mockCompanyProfile.setData(companyData);
        CompanyProfileDao mockCompanyProfileDao = new CompanyProfileDao(mockCompanyProfile);

        String companyUrl = String.format("/company/%s", "123456");

        when(companyProfileService.get("123456")).thenReturn(Optional.of(mockCompanyProfileDao));

        ResponseEntity<CompanyProfile> companyProfileResponse =
                restTemplate.getForEntity(companyUrl, CompanyProfile.class);

        assertThat(companyProfileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(companyProfileResponse.getBody()).usingRecursiveComparison().isEqualTo(mockCompanyProfile);
    }

    @Test
    @DisplayName("Return a not found response when company profile does not exist")
    void getCompanyProfileWhenDoesNotExist() {
        when(companyProfileService.get("123456")).thenReturn(Optional.empty());
        String companyUrl = String.format("/company/%s", "123456");

        ResponseEntity<CompanyProfile> companyProfileResponse =
                restTemplate.getForEntity(companyUrl, CompanyProfile.class);

        assertThat(companyProfileResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(companyProfileResponse.getBody()).isNull();
    }
}
