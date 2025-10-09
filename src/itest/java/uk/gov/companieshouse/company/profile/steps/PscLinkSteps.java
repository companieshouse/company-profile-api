package uk.gov.companieshouse.company.profile.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.companieshouse.company.profile.configuration.AbstractMongoConfig.mongoDBContainer;

import java.util.Collections;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.company.profile.configuration.CucumberContext;
import uk.gov.companieshouse.company.profile.configuration.WiremockTestConfig;
import uk.gov.companieshouse.company.profile.model.VersionedCompanyProfileDocument;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;

public class PscLinkSteps {
    private String contextId;
    private static final String ADD_PSC_LINK_ENDPOINT = "/company/00006400/links/persons-with-significant-control";
    private static final String DELETE_PSC_LINK_ENDPOINT = "/company/00006400/links/persons-with-significant-control/delete";
    private static final String PSC_LINK = "/company/00006400/persons-with-significant-control";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CompanyProfileRepository companyProfileRepository;

    @Before
    public void dbCleanUp() {
        WiremockTestConfig.setupWiremock();

        if (mongoDBContainer.getContainerId() == null) {
            mongoDBContainer.start();
        }
        companyProfileRepository.deleteAll();
    }

    @And("the psc link does not exist for {string}")
    public void checkPscLinkIsNotPresent(String companyNumber) {
        Optional<VersionedCompanyProfileDocument> document = companyProfileRepository.findById(companyNumber);

        assertThat(document).isPresent();
        assertThat(document.get().getCompanyProfile().getLinks().getPersonsWithSignificantControl())
                .isNullOrEmpty();
    }
    
    @When("a PATCH request is sent to the add psc link endpoint for {string}")
    public void addPscLink(String companyNumber) throws ApiErrorResponseException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);
        headers.set("ERIC-Identity", "TEST-IDENTITY");
        headers.set("ERIC-Identity-Type", "KEY");
        headers.add("ERIC-Authorised-Key-Privileges", "internal-app");

        HttpEntity<String> request = new HttpEntity<String>(null, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                ADD_PSC_LINK_ENDPOINT, HttpMethod.PATCH, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @When("a PATCH request is sent to the delete psc link endpoint for {string}")
    public void deletePscLink(String companyNumber) throws ApiErrorResponseException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);
        headers.set("ERIC-Identity", "TEST-IDENTITY");
        headers.set("ERIC-Identity-Type", "KEY");
        headers.add("ERIC-Authorised-Key-Privileges", "internal-app");

        HttpEntity<String> request = new HttpEntity<String>(null, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                DELETE_PSC_LINK_ENDPOINT, HttpMethod.PATCH, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @And("the psc link exists for {string}")
    public void verifyPscLinkExists(String companyNumber) {
        Optional<VersionedCompanyProfileDocument> document = companyProfileRepository.findById(companyNumber);

        assertThat(document).isPresent();
        assertThat(document.get().getCompanyProfile().getLinks().getPersonsWithSignificantControl())
                .isEqualTo(PSC_LINK);
    }

    @When("a PATCH request is sent to the add psc endpoint for {string} without ERIC headers")
    public void addPscLinkWithoutAuthenticationOrAuthorisation(String companyNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);

        HttpEntity<String> request = new HttpEntity<String>(null, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                ADD_PSC_LINK_ENDPOINT, HttpMethod.PATCH, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @When("a PATCH request is sent to the delete psc endpoint for {string} without ERIC headers")
    public void deletePscLinkWithoutAuthenticationOrAuthorisation(String companyNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);

        HttpEntity<String> request = new HttpEntity<String>(null, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                DELETE_PSC_LINK_ENDPOINT, HttpMethod.PATCH, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }
}
