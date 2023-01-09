package uk.gov.companieshouse.company.profile.steps.links;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.company.profile.configuration.WiremockTestConfig;
import uk.gov.companieshouse.company.profile.model.CompanyProfileDocument;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;
import uk.gov.companieshouse.company.profile.configuration.CucumberContext;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.companieshouse.company.profile.configuration.AbstractMongoConfig.mongoDBContainer;

public class OfficersLinkSteps {
    private String contextId;
    private static final String ADD_OFFICERS_LINK_ENDPOINT = "/company/00006400/links/officers";
    private static final String OFFICERS_LINK = "/company/00006400/officers";

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

    @Given("CHS Kafka API is available")
    public void kafkaIsAvailable() {
        WiremockTestConfig.stubKafkaApi(HttpStatus.OK.value());
    }

    @And("the company profile resource {string} exists for {string}")
    public void saveCompanyProfileResourceToDatabase(String dataFile, String companyNumber) throws IOException {
        File source = new ClassPathResource(String.format("/json/input/%s.json", dataFile)).getFile();

        Data companyProfileData = objectMapper.readValue(source, CompanyProfile.class).getData();

        CompanyProfileDocument companyProfile = new CompanyProfileDocument();
        companyProfile.setCompanyProfile(companyProfileData).setId(companyNumber);

        companyProfileRepository.save(companyProfile);
        CucumberContext.CONTEXT.set("companyProfileData", companyProfileData);
    }

    @And("the company profile resource for {string} does not already have an officers link")
    public void checkOfficersLinkIsNotPresent(String companyNumber) {
        Optional<CompanyProfileDocument> document = companyProfileRepository.findById(companyNumber);

        assertThat(document).isPresent();
        assertThat(document.get().getCompanyProfile().getLinks().getOfficers()).isNullOrEmpty();
    }

    @When("a PATCH request is sent to the add officers link endpoint for {string}")
    public void addOfficersLink(String companyNumber) throws ApiErrorResponseException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);
        headers.set("ERIC-Identity", "TEST-IDENTITY");
        headers.set("ERIC-Identity-Type", "KEY");

        HttpEntity<String> request = new HttpEntity<String>(null, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                ADD_OFFICERS_LINK_ENDPOINT, HttpMethod.PATCH, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @Then("the response code should be {int}")
    public void verifyStatusCodeReturned(int expectedStatusCode) {
        int actualStatusCode = CucumberContext.CONTEXT.get("statusCode");
        assertThat(actualStatusCode).isEqualTo(expectedStatusCode);
    }

    @And("the officers link exists for {string}")
    public void verifyOfficersLinkExists(String companyNumber) {
        Optional<CompanyProfileDocument> document = companyProfileRepository.findById(companyNumber);

        assertThat(document).isPresent();
        assertThat(document.get().getCompanyProfile().getLinks().getOfficers()).isEqualTo(OFFICERS_LINK);
    }

    @When("a PATCH request is sent to {string} without ERIC headers")
    public void addOfficersLinkWithoutAuthenticationOrAuthorisation(String companyNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);

        HttpEntity<String> request = new HttpEntity<String>(null, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                ADD_OFFICERS_LINK_ENDPOINT, HttpMethod.PATCH, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @Given("CHS Kafka API is unavailable")
    public void kafkaIsUnavailable() {
        WiremockTestConfig.stubKafkaApi(HttpStatus.SERVICE_UNAVAILABLE.value());
    }

    @Given("MongoDB is unavailable")
    public void mongoIsUnavailable() {
        mongoDBContainer.stop();
    }
}