package uk.gov.companieshouse.company.profile.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import uk.gov.companieshouse.api.model.CompanyProfileDocument;
import uk.gov.companieshouse.company.profile.configuration.CucumberContext;
import uk.gov.companieshouse.company.profile.configuration.WiremockTestConfig;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.companieshouse.company.profile.configuration.AbstractMongoConfig.mongoDBContainer;

public class ChargesLinkSteps {
    private String contextId;
    private static final String ADD_CHARGES_LINK_ENDPOINT = "/company/00006400/links/charges";
    private static final String DELETE_CHARGES_LINK_ENDPOINT = "/company/00006400/links/charges/delete";
    private static final String CHARGES_LINK = "/company/00006400/charges";

    @Autowired
    private ObjectMapper objectMapper;

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

    @And("the charges link does not exist for {string}")
    public void checkChargesLinkIsNotPresent(String companyNumber) {
        Optional<CompanyProfileDocument> document = companyProfileRepository.findById(companyNumber);

        assertThat(document).isPresent();
        assertThat(document.get().getCompanyProfile().getLinks().getCharges()).isNullOrEmpty();
    }

    @When("a PATCH request is sent to the add charges link endpoint for {string}")
    public void addChargesLink(String companyNumber) {
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
                ADD_CHARGES_LINK_ENDPOINT, HttpMethod.PATCH, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @When("a PATCH request is sent to the delete charges link endpoint for {string}")
    public void deleteChargesLink(String companyNumber) {
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
                DELETE_CHARGES_LINK_ENDPOINT, HttpMethod.PATCH, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @Then("the response code should be {int} for charges")
    public void verifyStatusCodeReturned(int expectedStatusCode) {
        int actualStatusCode = CucumberContext.CONTEXT.get("statusCode");
        assertThat(actualStatusCode).isEqualTo(expectedStatusCode);
    }

    @And("the charges link exists for {string}")
    public void verifyChargesLinkExists(String companyNumber) {
        Optional<CompanyProfileDocument> document = companyProfileRepository.findById(companyNumber);

        assertThat(document).isPresent();
        assertThat(document.get().getCompanyProfile().getLinks().getCharges()).isEqualTo(CHARGES_LINK);
    }

    @When("a PATCH request is sent to the add charges endpoint for {string} without ERIC headers")
    public void addChargesLinkWithoutAuthenticationOrAuthorisation(String companyNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);

        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                ADD_CHARGES_LINK_ENDPOINT, HttpMethod.PATCH, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @When("a PATCH request is sent to the delete charges endpoint for {string} without ERIC headers")
    public void deleteChargesLinkWithoutAuthenticationOrAuthorisation(String companyNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);

        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                DELETE_CHARGES_LINK_ENDPOINT, HttpMethod.PATCH, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }
}