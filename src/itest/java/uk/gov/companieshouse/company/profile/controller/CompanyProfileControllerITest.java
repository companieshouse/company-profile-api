package uk.gov.companieshouse.company.profile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.exception.DocumentNotFoundException;
import uk.gov.companieshouse.api.model.CompanyProfileDocument;
import uk.gov.companieshouse.api.model.Updated;
import uk.gov.companieshouse.company.profile.service.CompanyProfileService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CompanyProfileControllerITest {
    private static final String MOCK_COMPANY_NUMBER = "6146287";
    private static final String MOCK_CONTEXT_ID = "123456";
    private static final String COMPANY_URL = String.format("/company/%s/links",
            MOCK_COMPANY_NUMBER);

    private static final String DELETE_COMPANY_PROFILE_URL = String.format("/company/%s", MOCK_COMPANY_NUMBER);

    @MockBean
    private CompanyProfileService companyProfileService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Retrieve a company profile containing a given company number")
    void getCompanyProfileWithMatchingCompanyNumber() throws Exception {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        CompanyProfile mockCompanyProfile = new CompanyProfile().data(companyData);
        LocalDateTime localDateTime = LocalDateTime.now();
        //Updated updated = mock(Updated.class);
        Updated updated = new Updated(LocalDateTime.now(),
                "abc", "company_delta");
        CompanyProfileDocument mockCompanyProfileDocument = new CompanyProfileDocument(companyData, localDateTime, updated, false);
        mockCompanyProfileDocument.setId(MOCK_COMPANY_NUMBER);

        when(companyProfileService.get(MOCK_COMPANY_NUMBER)).thenReturn(Optional.of(mockCompanyProfileDocument));

        HttpHeaders headers = new HttpHeaders();
        headers.add("ERIC-Identity", "SOME_IDENTITY");
        headers.add("ERIC-Identity-Type", "key");

        ResponseEntity<CompanyProfile> companyProfileResponse = restTemplate.exchange(
                COMPANY_URL, HttpMethod.GET, new HttpEntity<Object>(headers),
                CompanyProfile.class);

        assertThat(companyProfileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Return 401 Unauthorised response when retrieving a company profile without passing Eric headers")
    void getCompanyProfileWithMatchingCompanyNumberWithoutSettingEricHeaders() throws Exception {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        CompanyProfile mockCompanyProfile = new CompanyProfile().data(companyData);
        LocalDateTime localDateTime = LocalDateTime.now();
        Updated updated = new Updated(LocalDateTime.now(),
                "abc", "company_delta");
        CompanyProfileDocument mockCompanyProfileDocument = new CompanyProfileDocument(companyData, localDateTime, updated, false);
        mockCompanyProfileDocument.setId(MOCK_COMPANY_NUMBER);

        when(companyProfileService.get(MOCK_COMPANY_NUMBER)).thenReturn(Optional.of(mockCompanyProfileDocument));

        HttpHeaders headers = new HttpHeaders();
        //Not setting Eric headers

        ResponseEntity<CompanyProfile> companyProfileResponse = restTemplate.exchange(
                COMPANY_URL, HttpMethod.GET, new HttpEntity<Object>(headers),
                CompanyProfile.class);

        assertThat(companyProfileResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Return a Resource Not found response when company profile does not exist")
    void getCompanyProfileWhenDoesNotExist() {
        when(companyProfileService.get(MOCK_COMPANY_NUMBER)).thenReturn(Optional.empty());

        HttpHeaders headers = new HttpHeaders();
        headers.add("ERIC-Identity", "SOME_IDENTITY");
        headers.add("ERIC-Identity-Type", "key");

        ResponseEntity<CompanyProfile> companyProfileResponse = restTemplate.exchange(
                COMPANY_URL, HttpMethod.GET, new HttpEntity<Object>(headers),
                CompanyProfile.class);

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
        headers.add("ERIC-Identity", "SOME_IDENTITY");
        headers.add("ERIC-Identity-Type", "key");
        headers.add("ERIC-Authorised-Key-Privileges", "internal-app");

        HttpEntity<CompanyProfile> httpEntity = new HttpEntity<>(mockCompanyProfile, headers);
        ResponseEntity<Void> responseEntity = restTemplate.exchange(
                COMPANY_URL,
                HttpMethod.PATCH, httpEntity, Void.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Return 401 Unauthorised response when trying to PATCH insolvency links without setting Eric headers")
    void patchInsolvencyLinksWithoutSettingEricHeaders() throws Exception {
        CompanyProfile mockCompanyProfile = new CompanyProfile();
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        mockCompanyProfile.setData(companyData);
        doNothing().when(companyProfileService).updateInsolvencyLink(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER, mockCompanyProfile);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("x-request-id", "123456");
        //Not setting Eric headers

        HttpEntity<CompanyProfile> httpEntity = new HttpEntity<>(mockCompanyProfile, headers);
        ResponseEntity<Void> responseEntity = restTemplate.exchange(
                COMPANY_URL,
                HttpMethod.PATCH, httpEntity, Void.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("PATCH insolvency links NOT FOUND")
    void patchInsolvencyLinksNotFound() throws Exception {
        CompanyProfile mockCompanyProfile = new CompanyProfile();
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        mockCompanyProfile.setData(companyData);
        doThrow(new DocumentNotFoundException("Not found"))
                .when(companyProfileService)
                .updateInsolvencyLink(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER, mockCompanyProfile);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("x-request-id", "123456");
        headers.add("ERIC-Identity", "SOME_IDENTITY");
        headers.add("ERIC-Identity-Type", "key");
        headers.add("ERIC-Authorised-Key-Privileges", "internal-app");

        HttpEntity<CompanyProfile> httpEntity = new HttpEntity<>(mockCompanyProfile, headers);
        ResponseEntity<Void> responseEntity = restTemplate.exchange(
                COMPANY_URL,
                HttpMethod.PATCH, httpEntity, Void.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isNull();
    }

}
