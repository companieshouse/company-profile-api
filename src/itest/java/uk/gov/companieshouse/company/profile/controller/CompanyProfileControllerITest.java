package uk.gov.companieshouse.company.profile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.company.profile.model.CompanyProfileDocument;
import uk.gov.companieshouse.company.profile.service.CompanyProfileService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CompanyProfileControllerITest {
    private static final String MOCK_COMPANY_NUMBER = "6146287";
    private static final String COMPANY_URL = String.format("/company/%s", MOCK_COMPANY_NUMBER);
    private static final String PATCH_INSOLVENCY_URL = String.format("/company/%s/links", MOCK_COMPANY_NUMBER);

    @MockBean
    private CompanyProfileService companyProfileService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Retrieve a company profile containing a given company number")
    void getCompanyProfileWithMatchingCompanyNumber() throws Exception {
        CompanyProfile mockCompanyProfile = new CompanyProfile();
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        mockCompanyProfile.setData(companyData);
        CompanyProfileDocument mockCompanyProfileDocument = new CompanyProfileDocument(mockCompanyProfile);

        when(companyProfileService.get(MOCK_COMPANY_NUMBER)).thenReturn(Optional.of(mockCompanyProfileDocument));

        ResponseEntity<CompanyProfile> companyProfileResponse =
                restTemplate.getForEntity(COMPANY_URL, CompanyProfile.class);

        assertThat(companyProfileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(companyProfileResponse.getBody()).usingRecursiveComparison().isEqualTo(mockCompanyProfile);
    }

    @Test
    @DisplayName("Return a not found response when company profile does not exist")
    void getCompanyProfileWhenDoesNotExist() {
        when(companyProfileService.get(MOCK_COMPANY_NUMBER)).thenReturn(Optional.empty());

        ResponseEntity<CompanyProfile> companyProfileResponse =
                restTemplate.getForEntity(COMPANY_URL, CompanyProfile.class);

        assertThat(companyProfileResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(companyProfileResponse.getBody()).isNull();
    }

    @Test
    @DisplayName("PATCH insolvency links")
    void patchInsolvencyLinks() {
        CompanyProfile mockCompanyProfile = new CompanyProfile();
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        mockCompanyProfile.setData(companyData);
        doNothing().when(companyProfileService).updateInsolvencyLink(mockCompanyProfile);

        HttpEntity<CompanyProfile> httpEntity = new HttpEntity<>(mockCompanyProfile, null);
        ResponseEntity<Void> responseEntity = restTemplate.exchange(
                PATCH_INSOLVENCY_URL,
                HttpMethod.PATCH, httpEntity, Void.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
