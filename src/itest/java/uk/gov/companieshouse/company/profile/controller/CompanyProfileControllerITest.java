package uk.gov.companieshouse.company.profile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.company.profile.model.CompanyProfileDocument;
import uk.gov.companieshouse.company.profile.service.CompanyProfileService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CompanyProfileControllerITest {
    private static final String MOCK_COMPANY_NUMBER = "6146287";
    private static final String MOCK_CONTEXT_ID = "123456";
    private static final String COMPANY_URL = String.format("/company/%s", MOCK_COMPANY_NUMBER);
    private static final String PATCH_INSOLVENCY_URL = String.format("/company/%s/links", MOCK_COMPANY_NUMBER);

    @MockBean
    private CompanyProfileService companyProfileService;

    @Autowired
    private TestRestTemplate restTemplate;

    // TODO: Fix this integration test.

    //    @Test
    //    @DisplayName("Retrieve a company profile containing a given company number")
    //    void getCompanyProfileWithMatchingCompanyNumber() throws Exception {
    //        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
    //        CompanyProfile mockCompanyProfile = new CompanyProfile().data(companyData);
    //        CompanyProfileDocument mockCompanyProfileDocument = new CompanyProfileDocument(companyData);
    //        mockCompanyProfileDocument.setId(MOCK_COMPANY_NUMBER);
    //
    //        when(companyProfileService.get(MOCK_COMPANY_NUMBER)).thenReturn(Optional.of(mockCompanyProfileDocument));
    //
    //        ResponseEntity<CompanyProfile> companyProfileResponse =
    //                restTemplate.getForEntity(COMPANY_URL, CompanyProfile.class);
    //
    //        assertThat(companyProfileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    //        assertThat(companyProfileResponse.getBody()).usingRecursiveComparison().isEqualTo(
    //                mockCompanyProfile
    //        );
    //    }

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
    void patchInsolvencyLinks() throws Exception {
        CompanyProfile mockCompanyProfile = new CompanyProfile();
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        mockCompanyProfile.setData(companyData);
        doNothing().when(companyProfileService).updateInsolvencyLink(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER, mockCompanyProfile);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("x-request-id", "123456");

        HttpEntity<CompanyProfile> httpEntity = new HttpEntity<>(mockCompanyProfile, headers);
        ResponseEntity<Void> responseEntity = restTemplate.exchange(
                PATCH_INSOLVENCY_URL,
                HttpMethod.PATCH, httpEntity, Void.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("PATCH insolvency links NOT FOUND")
    void patchInsolvencyLinksNotFound() throws Exception {
        CompanyProfile mockCompanyProfile = new CompanyProfile();
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        mockCompanyProfile.setData(companyData);
        doThrow(new NoSuchElementException()).when(companyProfileService).updateInsolvencyLink(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER, mockCompanyProfile);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("x-request-id", "123456");

        HttpEntity<CompanyProfile> httpEntity = new HttpEntity<>(mockCompanyProfile, headers);
        ResponseEntity<Void> responseEntity = restTemplate.exchange(
                PATCH_INSOLVENCY_URL,
                HttpMethod.PATCH, httpEntity, Void.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isNull();
    }
}
