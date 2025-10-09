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

public class ExemptionsLinkSteps {

    private String contextId;
    private static final String ADD_EXEMPTIONS_LINK_ENDPOINT = "/company/00006400/links/exemptions";
    private static final String DELETE_EXEMPTIONS_LINK_ENDPOINT = "/company/00006400/links/exemptions/delete";
    private static final String EXEMPTIONS_LINK = "/company/00006400/exemptions";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CompanyProfileRepository companyProfileRepository;

    @Before
    public void dbCleanUp(){
        WiremockTestConfig.setupWiremock();

        if (mongoDBContainer.getContainerId() == null) {
            mongoDBContainer.start();
        }
        companyProfileRepository.deleteAll();
    }

    @When("a PATCH request is sent to the add exemptions link endpoint for {string}")
    public void addExemptionsLink(String companyNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);
        headers.set("ERIC-Identity", "TEST-IDENTITY");
        headers.set("ERIC-Identity-Type", "KEY");
        headers.add("ERIC-Authorised-Key-Privileges", "internal-app");

        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                ADD_EXEMPTIONS_LINK_ENDPOINT, HttpMethod.PATCH, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @And("the exemptions link does not exist for {string}")
    public void checkExemptionsLinkIsNotPresent(String companyNumber) {
        Optional<VersionedCompanyProfileDocument> document = companyProfileRepository.findById(companyNumber);

        assertThat(document).isPresent();
        assertThat(document.get().getCompanyProfile().getLinks().getExemptions()).isNullOrEmpty();
    }

    @And("the exemptions link exists for {string}")
    public void verifyExemptionsLinkExists(String companyNumber) {
        Optional<VersionedCompanyProfileDocument> document = companyProfileRepository.findById(companyNumber);

        assertThat(document).isPresent();
        assertThat(document.get().getCompanyProfile().getLinks().getExemptions()).isEqualTo(EXEMPTIONS_LINK);
    }

    @When("a PATCH request is sent to the add exemptions endpoint for {string} without ERIC headers")
    public void addExemptionsLinkWithoutAuthenticationOrAuthorisation(String companyNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);

        HttpEntity<String> request = new HttpEntity<String>(null, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                ADD_EXEMPTIONS_LINK_ENDPOINT, HttpMethod.PATCH, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @When("a PATCH request is sent to the delete exemptions link endpoint for {string}")
    public void deleteOfficersLink(String companyNumber) throws ApiErrorResponseException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);
        headers.set("ERIC-Identity", "TEST-IDENTITY");
        headers.set("ERIC-Identity-Type", "KEY");
        headers.add("ERIC-Authorised-Key-Privileges", "internal-app");

        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                DELETE_EXEMPTIONS_LINK_ENDPOINT, HttpMethod.PATCH, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @When("a PATCH request is sent to the delete exemptions endpoint for {string} without ERIC headers")
    public void deleteOfficersLinkWithoutAuthenticationOrAuthorisation(String companyNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);

        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                DELETE_EXEMPTIONS_LINK_ENDPOINT, HttpMethod.PATCH, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }
}
