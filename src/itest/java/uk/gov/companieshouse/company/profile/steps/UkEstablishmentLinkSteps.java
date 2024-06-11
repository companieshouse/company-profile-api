package uk.gov.companieshouse.company.profile.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
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

public class UkEstablishmentLinkSteps {

    private String contextId;
    private static final String UK_ESTABLISHMENTS_LINK = "/company/%s/uk-establishments";
    private static final String DELETE_UK_ESTABLISHMENTS_LINK = "/company/%s/links/uk-establishments/delete";

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

    @And("a UK establishment link should be added for {string}")
    public void verifyUkEstablishmentsLinkExists(String companyNumber) {
        getUkEstablishmentLink(companyNumber);
    }
    @And("a UK establishment link does exist for {string}")
    public void theUkEstablishmentLinkDoesExistFor(String parentCompanyNumber) {
        getUkEstablishmentLink(parentCompanyNumber);
    }
    @And("the UK establishment link should still exist for {string}")
    public void theUkEstablishmentLinkShouldStillExistFor(String parentCompanyNumber) {
        getUkEstablishmentLink(parentCompanyNumber);
    }

    private void getUkEstablishmentLink(String parentCompanyNumber) {
        Optional<CompanyProfileDocument> document = companyProfileRepository.findById(parentCompanyNumber);
        assertThat(document).isPresent();
        System.out.println(document.get().getCompanyProfile().getLinks());
        assertThat(document.get().getCompanyProfile().getLinks().getUkEstablishments()).isEqualTo(String.format(UK_ESTABLISHMENTS_LINK, parentCompanyNumber));
    }

    @And("a UK establishment link does not exist for {string}")
    public void theUkEstablishmentsLinkDoesNotExistFor(String parentCompanyNumber) {
        ukEstablishmentLinkShouldNotExist(parentCompanyNumber);
    }

    @And("the UK establishment link should be removed from {string}")
    public void theUkEstablishmentLinkShouldBeRemovedFrom(String parentCompanyNumber) {
        ukEstablishmentLinkShouldNotExist(parentCompanyNumber);
    }

    private void ukEstablishmentLinkShouldNotExist(String companyNumber) {
        Optional<CompanyProfileDocument> document = companyProfileRepository.findById(companyNumber);
        assertThat(document).isPresent();
        assertThat(document.get().getCompanyProfile().getLinks().getUkEstablishments()).isNullOrEmpty();
    }

    @When("a PATCH request is sent to the delete UK establishments link endpoint for {string}")
    public void sendDeleteUkEstablishmentLinkPatchRequest(String parentCompanyNumber) {
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
                String.format(DELETE_UK_ESTABLISHMENTS_LINK, parentCompanyNumber), HttpMethod.PATCH, request, Void.class, parentCompanyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @When("a PATCH request is sent to the delete UK establishments endpoint for {string} without ERIC headers")
    public void deleteUKEstablishmentLinkWithoutAuthenticationOrAuthorisation(String parentCompanyNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);

        HttpEntity<String> request = new HttpEntity<String>(null, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                String.format(DELETE_UK_ESTABLISHMENTS_LINK, parentCompanyNumber), HttpMethod.PATCH, request, Void.class, parentCompanyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }
}
