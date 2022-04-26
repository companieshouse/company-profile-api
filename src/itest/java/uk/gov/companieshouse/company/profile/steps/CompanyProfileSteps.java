package uk.gov.companieshouse.company.profile.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.*;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.company.profile.configuration.CucumberContext;
import uk.gov.companieshouse.company.profile.model.CompanyProfileDocument;
import uk.gov.companieshouse.company.profile.model.Updated;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class CompanyProfileSteps {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CompanyProfileRepository companyProfileRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Before
    public void dbCleanUp(){
        companyProfileRepository.deleteAll();
    }

    @When("I send PATCH request with payload {string} and company number {string}")
    public void i_send_put_request_with_payload(String dataFile, String companyNumber) throws IOException {
        File file = new ClassPathResource("/json/input/" + dataFile + ".json").getFile();
        CompanyProfile companyProfile = objectMapper.readValue(file, CompanyProfile.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("x-request-id", "5234234234");

        HttpEntity<CompanyProfile> request = new HttpEntity<>(companyProfile, headers);
        String uri = "/company/{company_number}/links";
        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.PATCH, request, Void.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
    }
    @Then("I should receive {int} status code")
    public void i_should_receive_status_code(Integer statusCode) {
        Integer expectedStatusCode = (Integer) CucumberContext.CONTEXT.get("statusCode");
        Assertions.assertThat(expectedStatusCode).isEqualTo(statusCode);
    }

    @Then("the expected result should match {string} file with company number {string}")
    public void the_expected_result_should_match(String data, String companyNumber) throws IOException {
        File file = new ClassPathResource("/json/output/" + data + ".json").getFile();
        CompanyProfileDocument expected = objectMapper.readValue(file, CompanyProfileDocument.class);

        Optional<CompanyProfileDocument> actual = companyProfileRepository.findById(companyNumber);

        assertThat(actual).isPresent();
        assertThat(actual.get().getCompanyProfile()).isEqualTo(expected.getCompanyProfile());
    }

    @Given("the company links exists for {string}")
    public void the_company_links_exists_for(String dataFile) throws IOException {
        File file = new ClassPathResource("/json/input/" + dataFile + ".json").getFile();
        CompanyProfile companyProfile = objectMapper.readValue(file, CompanyProfile.class);
        LocalDateTime localDateTime = LocalDateTime.now(ZoneOffset.UTC);
        //Updated updated = mock(Updated.class);
        Updated updated = new Updated(LocalDateTime.now(ZoneOffset.UTC),
                "abc", "company_delta");
        CompanyProfileDocument companyProfileDocument =
                new CompanyProfileDocument(companyProfile.getData(), localDateTime, updated);
        companyProfileDocument.setId(companyProfile.getData().getCompanyNumber());

        mongoTemplate.save(companyProfileDocument);
    }

    @When("I send GET request with company number {string}")
    public void i_send_get_request_with_company_number(String companyNumber) {
        String uri = "/company/{company_number}/links";
        ResponseEntity<Data> response = restTemplate.exchange(uri, HttpMethod.GET, null,
                Data.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
        CucumberContext.CONTEXT.set("getResponseBody", response.getBody());
    }

    @Then("the Get call response body should match {string} file")
    public void the_get_call_response_body_should_match_file(String dataFile) throws IOException {
        File file = new ClassPathResource("/json/output/" + dataFile + ".json").getFile();

        Data expected = objectMapper.readValue(file, Data.class);

        Data actual = (Data) CucumberContext.CONTEXT.get("getResponseBody");

        assertThat(actual).isEqualTo(expected);
    }

}
