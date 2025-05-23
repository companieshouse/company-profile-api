package uk.gov.companieshouse.company.profile.steps;

import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.companieshouse.company.profile.configuration.AbstractMongoConfig.mongoDBContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.company.UkEstablishmentsList;
import uk.gov.companieshouse.api.exception.ResourceNotFoundException;
import uk.gov.companieshouse.api.model.CompanyProfileDocument;
import uk.gov.companieshouse.api.model.Updated;
import uk.gov.companieshouse.company.profile.api.CompanyProfileApiService;
import uk.gov.companieshouse.company.profile.configuration.CucumberContext;
import uk.gov.companieshouse.company.profile.configuration.WiremockTestConfig;
import uk.gov.companieshouse.company.profile.model.VersionedCompanyProfileDocument;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;
import uk.gov.companieshouse.company.profile.service.CompanyProfileService;
import uk.gov.companieshouse.company.profile.transform.CompanyProfileTransformer;

public class CompanyProfileSteps {

    private static final DateTimeFormatter DELTA_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS");
    private static final String DELTA_AT = "20241229123010123789";
    private String companyNumber;
    private String contextId;
    private ResponseEntity<Data> response;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CompanyProfileRepository companyProfileRepository;

    @Autowired
    private CompanyProfileApiService companyProfileApiService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CompanyProfileService companyProfileService;

    @Autowired
    private CompanyProfileTransformer companyProfileTransformer;

    @Before
    public void dbCleanUp() {
        WiremockTestConfig.setupWiremock();

        if (mongoDBContainer.getContainerId() == null) {
            mongoDBContainer.start();
        }
        companyProfileRepository.deleteAll();

    }

    @Given("the CHS Kafka API is reachable")
    public void the_chs_kafka_api_is_reachable() {
        WiremockTestConfig.stubKafkaApi(HttpStatus.OK.value());
    }

    @Given("CHS kafka API service is unavailable")
    public void chs_kafka_service_unavailable() {
        WiremockTestConfig.stubKafkaApi(HttpStatus.SERVICE_UNAVAILABLE.value());
    }

    @When("I send PATCH request with payload {string} and company number {string}")
    public void i_send_put_request_with_payload(String dataFile, String companyNumber) throws IOException {
        WiremockTestConfig.stubKafkaApi(HttpStatus.OK.value());

        File file = new ClassPathResource("/json/input/" + dataFile + ".json").getFile();
        CompanyProfile companyProfile = objectMapper.readValue(file, CompanyProfile.class);

        this.contextId = "5234234234";
        this.companyNumber = companyNumber;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("x-request-id", "5234234234");
        headers.set("ERIC-Identity", "SOME_IDENTITY");
        headers.set("ERIC-Identity-Type", "key");
        headers.add("ERIC-Authorised-Key-Privileges", "internal-app");

        HttpEntity<CompanyProfile> request = new HttpEntity<>(companyProfile, headers);
        String uri = "/company/{company_number}/links";
        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.PATCH, request, Void.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @When("I send PATCH request with payload {string} and company number {string} and CHS Kafka API unavailable")
    public void i_send_patch_request_with_payload_and_company_number_and_chs_kafka_api_unavailable(String dataFile, String companyNumber)
            throws IOException {
        WiremockTestConfig.stubKafkaApi(HttpStatus.SERVICE_UNAVAILABLE.value());

        File file = new ClassPathResource("/json/input/" + dataFile + ".json").getFile();
        CompanyProfile companyProfile = objectMapper.readValue(file, CompanyProfile.class);

        this.contextId = "5234234234";
        this.companyNumber = companyNumber;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("x-request-id", "5234234234");
        headers.set("ERIC-Identity", "SOME_IDENTITY");
        headers.set("ERIC-Identity-Type", "key");
        headers.add("ERIC-Authorised-Key-Privileges", "internal-app");

        HttpEntity<CompanyProfile> request = new HttpEntity<>(companyProfile, headers);
        String uri = "/company/{company_number}/links";
        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.PATCH, request, Void.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @When("I send PATCH request with payload {string} and company number {string} without setting Eric headers")
    public void i_send_put_request_with_payload_without_setting_Eric_Headers(String dataFile, String companyNumber) throws IOException {
        File file = new ClassPathResource("/json/input/" + dataFile + ".json").getFile();
        CompanyProfile companyProfile = objectMapper.readValue(file, CompanyProfile.class);

        this.contextId = "5234234234";
        this.companyNumber = companyNumber;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("x-request-id", "5234234234");
        //Not setting Eric headers

        HttpEntity<CompanyProfile> request = new HttpEntity<>(companyProfile, headers);
        String uri = "/company/{company_number}/links";
        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.PATCH, request, Void.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @When("I send PATCH request with raw payload {string} and company number {string}")
    public void i_send_put_request_with_raw_payload(String dataFile, String companyNumber) throws IOException {
        File file = new ClassPathResource("/json/input/" + dataFile + ".json").getFile();
        String raw_payload = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("ERIC-Identity", "SOME_IDENTITY");
        headers.set("ERIC-Identity-Type", "key");
        headers.add("ERIC-Authorised-Key-Privileges", "internal-app");

        this.contextId = "5234234234";
        headers.set("x-request-id", this.contextId);

        HttpEntity<?> request = new HttpEntity<>(raw_payload, headers);
        String uri = "/company/{company_number}/links";
        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.PATCH, request, Void.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @Then("I should receive {int} status code")
    public void i_should_receive_status_code(Integer statusCode) {
        Integer expectedStatusCode = CucumberContext.CONTEXT.get("statusCode");
        Assertions.assertThat(expectedStatusCode).isEqualTo(statusCode);
    }

    @Then("the CHS Kafka API is invoked successfully")
    public void chs_kafka_api_invoked() {
        verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/private/resource-changed")));
    }

    @Then("the expected result should match {string} file with company number {string}")
    public void the_expected_result_should_match(String data, String companyNumber) throws IOException {
        File file = new ClassPathResource("/json/output/" + data + ".json").getFile();
        CompanyProfileDocument expected = objectMapper.readValue(file, CompanyProfileDocument.class);

        Optional<VersionedCompanyProfileDocument> actual = companyProfileRepository.findById(companyNumber);

        assertThat(actual).isPresent();
        assertThat(actual.get().getCompanyProfile()).isEqualTo(expected.getCompanyProfile());
    }

    @Given("the company links exists for {string}")
    public void the_company_links_exists_for(String dataFile) throws IOException {
        File file = new ClassPathResource("/json/input/" + dataFile + ".json").getFile();
        CompanyProfile companyProfile = objectMapper.readValue(file, CompanyProfile.class);
        LocalDateTime localDateTime = LocalDateTime.now();
        Updated updated = new Updated(LocalDateTime.now().minusYears(1),
                "abc", "company_delta");
        VersionedCompanyProfileDocument companyProfileDocument =
                new VersionedCompanyProfileDocument(companyProfile.getData(), localDateTime, updated, false);
        companyProfileDocument.setId(companyProfile.getData().getCompanyNumber());
        companyProfileDocument.version(0L);

        companyProfileRepository.insert(companyProfileDocument);
    }

    @When("I send GET request with company number {string}")
    public void i_send_get_request_with_company_number(String companyNumber) {
        String uri = "/company/{company_number}/links";

        HttpHeaders headers = new HttpHeaders();
        headers.add("ERIC-Identity", "SOME_IDENTITY");
        headers.add("ERIC-Identity-Type", "key");

        ResponseEntity<Data> response = restTemplate.exchange(
                uri, HttpMethod.GET, new HttpEntity<>(headers),
                Data.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
        CucumberContext.CONTEXT.set("getResponseBody", response.getBody());
    }

    @When("I send GET request with company number {string} without setting Eric headers")
    public void i_send_get_request_with_company_number_without_setting_Eric_Headers(String companyNumber) {
        String uri = "/company/{company_number}/links";

        HttpHeaders headers = new HttpHeaders();
        //Not setting Eric headers

        ResponseEntity<Data> response = restTemplate.exchange(
                uri, HttpMethod.GET, new HttpEntity<>(headers),
                Data.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
        CucumberContext.CONTEXT.set("getResponseBody", response.getBody());
    }

    @Then("the Get call response body should match {string} file")
    public void the_get_call_response_body_should_match_file(String dataFile) throws IOException {
        String data = FileCopyUtils.copyToString(new InputStreamReader(new FileInputStream("src/itest/resources/json/output/" + dataFile + ".json")));
        Data expected = objectMapper.readValue(data, Data.class);

        Data actual = CucumberContext.CONTEXT.get("getResponseBody");
        assertThat(actual).isEqualTo(expected);
    }

    @Then("the CHS Kafka API is not invoked")
    public void chs_kafka_api_not_invoked() {
        verify(0, postRequestedFor(urlEqualTo("/private/resource-changed")));
        List<ServeEvent> serverEvents = WiremockTestConfig.getServeEvents();
        assertTrue(serverEvents.isEmpty());
    }

    @Then("the CHS Kafka API is invoked successfully for delete for {string}")
    public void chs_kafka_api_invoked_delete(String dataFile) throws IOException {
        verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/private/resource-changed")));

        File file = new FileSystemResource("src/itest/resources/json/output/" + dataFile + "-resourceChanged.json").getFile();
        ChangedResource expectedChangedResource = objectMapper.readValue(file, ChangedResource.class);

        List<ServeEvent> serverEvents = WiremockTestConfig.getServeEvents();
        assertThat(serverEvents).hasSize(1);
        String requestReceived = serverEvents.getFirst().getRequest().getBodyAsString();
        ChangedResource actualChangedResource = objectMapper.convertValue(Document.parse(requestReceived), ChangedResource.class);

        assertEquals(expectedChangedResource.getDeletedData(), actualChangedResource.getDeletedData());
        assertEquals(expectedChangedResource.getResourceUri(), actualChangedResource.getResourceUri());
        assertEquals(expectedChangedResource.getResourceKind(), actualChangedResource.getResourceKind());
        assertEquals(expectedChangedResource.getEvent().getType(), actualChangedResource.getEvent().getType());

        String actualDeletedData = actualChangedResource.getDeletedData().toString();
        assertFalse(actualDeletedData.contains("null"));
    }

    @Then("nothing is persisted in the database")
    public void nothing_persisted_database() {
        List<VersionedCompanyProfileDocument> insolvencyDocuments = companyProfileRepository.findAll();
        assertTrue(insolvencyDocuments.isEmpty());
    }

    @Then("save operation is not invoked")
    public void save_operation_is_not_invoked() {
        Optional<VersionedCompanyProfileDocument> actual = companyProfileRepository.findById(this.companyNumber);
        LocalDateTime at = actual.get().getUpdated().getAt();

        /*
         * Initially the updated year is set to 2021, if the call to db is triggered then the year is set to 2022.
         * Since the save operation is not invoked the updated date remains as initial year.
         */
        assertThat(at.getYear()).isEqualTo(LocalDateTime.now().minusYears(1).getYear());
    }

    @Given("the company profile database is down")
    public void theDatabaseIsDown() {
        mongoDBContainer.stop();
    }

    @Given("a company profile resource does not exist for {string}")
    public void checkResourceDoesNotExist(String companyNumber) {
        assertThat(companyProfileRepository.findById(companyNumber)).isEmpty();
    }

    @And("the company profile resource {string} exists for {string}")
    public void saveCompanyProfileResourceToDatabase(String dataFile, String companyNumber) throws IOException {
        File source = new ClassPathResource(String.format("/json/input/%s.json", dataFile)).getFile();

        CompanyProfile companyProfile = objectMapper.readValue(source, CompanyProfile.class);
        Data companyProfileData = companyProfile.getData();

        VersionedCompanyProfileDocument document = new VersionedCompanyProfileDocument();
        document.setCompanyProfile(companyProfileData)
                .setId(companyNumber);

        if (companyProfile.getDeltaAt() != null) {
            document.setDeltaAt(LocalDateTime.parse(companyProfile.getDeltaAt(), DELTA_AT_FORMATTER));
        }

        companyProfileRepository.save(document);
        assertThat(companyProfileRepository.findById(companyNumber)).isPresent();
        CucumberContext.CONTEXT.set("companyProfileData", companyProfileData);
    }

    @When("I send GET request to retrieve Company Profile using company number {string}")
    public void i_send_get_request_to_retrieve_company_profile(String companyNumber) {
        String uri = "/company/{company_number}";

        HttpHeaders headers = new HttpHeaders();
        headers.add("ERIC-Identity", "SOME_IDENTITY");
        headers.add("ERIC-Identity-Type", "key");
        headers.add("x-request-id", "123456");
        headers.add("ERIC-Authorised-Key-Privileges", "internal-app");

        ResponseEntity<Data> response = restTemplate.exchange(
                uri, HttpMethod.GET, new HttpEntity<>(headers),
                Data.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
        CucumberContext.CONTEXT.set("getResponseBody", response.getBody());
    }


    @When("I send GET request to retrieve Company Profile using company number {string} without setting Eric headers")
    public void i_send_get_request_to_retrieve_company_profile_without_eric_headers(String companyNumber) {
        String uri = "/company/{company_number}";

        HttpHeaders headers = new HttpHeaders();

        ResponseEntity<Data> response = restTemplate.exchange(
                uri, HttpMethod.GET, new HttpEntity<>(headers),
                Data.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
        CucumberContext.CONTEXT.set("getResponseBody", response.getBody());
    }

    @And("a Company Profile exists for {string}")
    public void the_company_profile_exists_for(String dataFile) throws IOException {
        insertCompanyIntoMongo(String.format("src/itest/resources/json/input/%s.json", dataFile), null);
    }

    @When("I send a PUT request with payload {string} file for company number {string}")
    public void i_send_company_profile_put_request_with_payload(
            String dataFile, String companyNumber) throws IOException {
        String data = FileCopyUtils.copyToString(new InputStreamReader(
                new FileInputStream("src/itest/resources/json/input/" + dataFile + ".json")));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);
        headers.set("ERIC-Identity", "TEST-IDENTITY");
        headers.set("ERIC-Identity-Type", "key");
        headers.set("ERIC-Authorised-Key-Roles", "*");
        headers.add("ERIC-Authorised-Key-Privileges", "internal-app");
        headers.set("Content-Type", "application/json");

        HttpEntity<?> request = new HttpEntity<>(data, headers);
        String uri = "/company/{company_number}/internal";
        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.PUT, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @When("I send a PUT request with payload {string} file for company number {string} without setting Eric headers")
    public void i_send_company_profile_put_request_with_payload_without_headers(String dataFile, String companyNumber) throws IOException {
        String data = FileCopyUtils.copyToString(new InputStreamReader(new FileInputStream("src/itest/resources/json/input/" + dataFile + ".json")));

        HttpHeaders headers = new HttpHeaders();

        HttpEntity<?> request = new HttpEntity<>(data, headers);
        String uri = "/company/{company_number}/internal";
        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.PUT, request, Void.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @Then("a company profile exists with id {string}")
    public void company_profile_exists(String companyNumber) {
        Assertions.assertThat(companyProfileRepository.existsById(companyNumber)).isTrue();
    }

    @When("a DELETE request is sent to the company profile endpoint for {string}")
    public void a_DELETE_request_is_sent_to_the_company_profile_endpoint_for(String companyNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);
        String deltaAt = LocalDateTime.now().plusMonths(1).format(DELTA_AT_FORMATTER);
        headers.set("X-DELTA-AT", deltaAt);

        headers.set("ERIC-Identity", "TEST-IDENTITY");
        headers.set("ERIC-Identity-Type", "key");
        headers.set("ERIC-Authorised-Key-Roles", "*");
        headers.add("api-key", "g9yZIA81Zo9J46Kzp3JPbfld6kOqxR47EAYqXbRV");
        headers.add("ERIC-Authorised-Key-Privileges", "internal-app");
        headers.set("Content-Type", "application/json");

        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                "/company/{company_number}/internal", HttpMethod.DELETE, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @When("a DELETE request is sent to the company profile endpoint for {string} without valid ERIC headers")
    public void a_DELETE_request_is_sent_to_the_company_profile_endpoint_for_without_valid_ERIC_headers(String companyNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);
        headers.set("X-DELTA-AT", DELTA_AT);

        HttpEntity<String> request = new HttpEntity<String>(null, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                "/company/{company_number}/internal", HttpMethod.DELETE, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @When("a DELETE request is sent to the company profile endpoint for {string} with stale delta")
    public void a_DELETE_request_is_sent_to_the_company_profile_endpoint_for_with_stale_delta(String companyNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);
        headers.set("X-DELTA-AT", "20000815110718375628");

        headers.set("ERIC-Identity", "TEST-IDENTITY");
        headers.set("ERIC-Identity-Type", "key");
        headers.set("ERIC-Authorised-Key-Roles", "*");
        headers.add("api-key", "g9yZIA81Zo9J46Kzp3JPbfld6kOqxR47EAYqXbRV");
        headers.add("ERIC-Authorised-Key-Privileges", "internal-app");
        headers.set("Content-Type", "application/json");

        HttpEntity<String> request = new HttpEntity<String>(null, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                "/company/{company_number}/internal", HttpMethod.DELETE, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @When("a DELETE request is sent to the company profile endpoint for {string} with blank delta at")
    public void a_DELETE_request_is_sent_to_the_company_profile_endpoint_for_with_blank_delta_at(String companyNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);
        headers.set("X-DELTA-AT", "");

        headers.set("ERIC-Identity", "TEST-IDENTITY");
        headers.set("ERIC-Identity-Type", "key");
        headers.set("ERIC-Authorised-Key-Roles", "*");
        headers.add("api-key", "g9yZIA81Zo9J46Kzp3JPbfld6kOqxR47EAYqXbRV");
        headers.add("ERIC-Authorised-Key-Privileges", "internal-app");
        headers.set("Content-Type", "application/json");

        HttpEntity<String> request = new HttpEntity<String>(null, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                "/company/{company_number}/internal", HttpMethod.DELETE, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @And("the company profile does not exist for {string}")
    public void the_company_profile_does_not_exist_for(String companyNumber) {
        assertThat(companyProfileRepository.existsById(companyNumber)).isFalse();
    }

    @When("I send GET request to retrieve Company details using company number {string}")
    public void iSendGETRequestToRetrieveCompanyDetailsUsingCompanyNumber(String companyNumber) {
        String uri = "/company/{company_number}/company-detail";

        HttpHeaders headers = new HttpHeaders();
        headers.set("ERIC-Identity", "TEST-IDENTITY");
        headers.set("ERIC-Identity-Type", "key");
        headers.set("ERIC-Authorised-Key-Roles", "*");
        headers.add("api-key", "g9yZIA81Zo9J46Kzp3JPbfld6kOqxR47EAYqXbRV");
        headers.add("ERIC-Authorised-Key-Privileges", "internal-app");
        headers.set("Content-Type", "application/json");

        ResponseEntity<Data> response = restTemplate.exchange(
                uri, HttpMethod.GET, new HttpEntity<>(headers),
                Data.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
        CucumberContext.CONTEXT.set("getResponseBody", response.getBody());

    }

    @When("I send GET request to retrieve Company details using company number {string} without setting Eric headers")
    public void iSendGETRequestToRetrieveCompanyDetailsUsingCompanyNumberWithoutSettingEricHeaders(String companyNumber) {
        String uri = "/company/{company_number}/company-detail";

        HttpHeaders headers = new HttpHeaders();

        ResponseEntity<Data> response = restTemplate.exchange(
                uri, HttpMethod.GET, new HttpEntity<>(headers),
                Data.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
        CucumberContext.CONTEXT.set("getResponseBody", response.getBody());
    }

    @And("the Get call response body should match {string} file for company details")
    public void theGetCallResponseBodyShouldMatchFileForCompanyDetails(String dataFile) throws IOException {
        String data = FileCopyUtils.copyToString(new InputStreamReader
                (new FileInputStream("src/itest/resources/json/output/" + dataFile + ".json")));
        CompanyDetails expected = objectMapper.readValue(data, CompanyDetails.class);

        Data actual = CucumberContext.CONTEXT.get("getResponseBody");

        assertThat(actual.getCompanyName()).isEqualTo(expected.getCompanyName());
        assertThat(actual.getCompanyNumber()).isEqualTo(expected.getCompanyNumber());
        assertThat(actual.getCompanyStatus()).isEqualTo(expected.getCompanyStatus());
    }

    @When("a DELETE request is sent to the company profile endpoint for {string} with insufficient access")
    public void aDELETERequestIsSentToTheCompanyProfileEndpointForWithInsufficientAccess(String companyNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);

        headers.set("ERIC-Identity", "TEST-IDENTITY");
        headers.set("ERIC-Identity-Type", "key");
        headers.set("ERIC-Authorised-Key-Roles", "basic-role");
        headers.add("api-key", "g9yZIA81Zo9J46Kzp3JPbfld6kOqxR47EAYqXbRV");
        headers.add("ERIC-Authorised-Key-Privileges", "");
        headers.set("Content-Type", "application/json");

        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<Void> response = restTemplate.exchange(
                "/company/{company_number}", HttpMethod.DELETE, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @When("I send GET request to retrieve Company Profile using company number {string} with insufficient access")
    public void iSendGETRequestToRetrieveCompanyProfileUsingCompanyNumberWithInsufficientAccess(String companyNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);
        headers.set("ERIC-Identity", "TEST-IDENTITY");
        headers.set("ERIC-Identity-Type", "key");
        headers.set("ERIC-Authorised-Key-Roles", "basic-role");
        headers.add("ERIC-Authorised-Key-Privileges", "");
        headers.set("Content-Type", "application/json");

        HttpEntity<String> request = new HttpEntity<>(null, headers);
        ResponseEntity<Data> response = restTemplate.exchange(
                "/company/{company_number}", HttpMethod.GET, request, Data.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @When("I send a PUT request with payload {string} file for company number {string} with insufficient access")
    public void iSendAPUTRequestWithPayloadFileForCompanyNumberWithInsufficientAccess(String companyNumber, String dataFile) throws IOException {
        String data = FileCopyUtils.copyToString(
                new InputStreamReader(new FileInputStream(
                        "src/itest/resources/json/input/" + dataFile + ".json")));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);
        headers.set("ERIC-Identity", "TEST-IDENTITY");
        headers.set("ERIC-Identity-Type", "key");
        headers.set("ERIC-Authorised-Key-Roles", "basic-role");
        headers.add("ERIC-Authorised-Key-Privileges", "");
        headers.set("Content-Type", "application/json");

        HttpEntity<?> request = new HttpEntity<>(data, headers);
        String uri = "/company/{company_number}";
        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.PUT, request, Void.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }

    @When("I send a GET request to retrieve UK establishments using company number {string} without Eric headers")
    public void iSendGETRequestToRetrieveUKEstablishmentsUsingCompanyNumberWithoutSettingEricHeaders(String companyNumber) {
        String uri = "/company/{company_number}/uk-establishments";

        HttpHeaders headers = new HttpHeaders();

        ResponseEntity<Data> response = restTemplate.exchange(
                uri, HttpMethod.GET, new HttpEntity<>(headers),
                Data.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
        CucumberContext.CONTEXT.set("getResponseBody", response.getBody());
    }

    @When("I send a GET request to retrieve UK establishments using company number {string}")
    public void iSendAGETRequestToRetrieveUKEstablishmentsUsingCompanyNumber(String companyNumber) {
        String uri = "/company/{company_number}/uk-establishments";

        HttpHeaders headers = new HttpHeaders();
        headers.set("ERIC-Identity", "TEST-IDENTITY");
        headers.set("ERIC-Identity-Type", "key");
        headers.add("api-key", "g9yZIA81Zo9J46Kzp3JPbfld6kOqxR47EAYqXbRV");
        headers.set("Content-Type", "application/json");

        ResponseEntity<UkEstablishmentsList> response = restTemplate.exchange(
                uri, HttpMethod.GET, new HttpEntity<>(headers),
                UkEstablishmentsList.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
        CucumberContext.CONTEXT.set("getResponseBody", response.getBody());
    }

    @And("UK establishments exists for parent company with number {string}")
    public void ukEstablishmentsExistsForParentCompanyWithNumber(String companyNumber) throws IOException {
        insertCompanyIntoMongo(String.format("src/itest/resources/json/input/%s.json", companyNumber), null);
        insertCompanyIntoMongo("src/itest/resources/json/input/00006404.json", companyNumber);
        insertCompanyIntoMongo("src/itest/resources/json/input/00006405.json", companyNumber);
    }

    @And("a single UK establishment exist for parent company with number {string}")
    public void singleUkEstablishmentExistsForParentCompanyWithNumber(String companyNumber) throws IOException {
        insertCompanyIntoMongo(String.format("src/itest/resources/json/input/%s.json", companyNumber), null);
        insertCompanyIntoMongo("src/itest/resources/json/input/00006404.json", companyNumber);
    }

    private void insertCompanyIntoMongo(String filePath, String parentCompanyNumber) throws IOException {
        String data = FileCopyUtils.copyToString(new InputStreamReader(new FileInputStream(filePath)));
        CompanyProfile companyProfile = objectMapper.readValue(data, CompanyProfile.class);

        LocalDateTime localDateTime = LocalDateTime.now();
        Updated updated = new Updated(LocalDateTime.now().minusYears(1),
                "abc", "company_delta");

        VersionedCompanyProfileDocument companyProfileDocument =
                new VersionedCompanyProfileDocument(companyProfile.getData(), localDateTime, updated, false);
        companyProfileDocument.setId(companyProfile.getData().getCompanyNumber());
        companyProfileDocument.version(0L);
        if (parentCompanyNumber != null) {
        companyProfileDocument.setParentCompanyNumber(
                companyProfile.getData().getBranchCompanyDetails().getParentCompanyNumber());
        }
        companyProfileRepository.insert(companyProfileDocument);
    }

    @And("the GET call response body for the list of uk establishments should match {string}")
    public void theGETCallResponseBodyForTheListOfUkEstablishmentsShouldMatch(String result)
            throws IOException {
        String data = FileCopyUtils.copyToString(new InputStreamReader
                (new FileInputStream("src/itest/resources/json/output/" + result + ".json")));
        UkEstablishmentsList expected = objectMapper.readValue(data, UkEstablishmentsList.class);

        UkEstablishmentsList actual = CucumberContext.CONTEXT.get("getResponseBody");

        assertThat(actual.getLinks()).isEqualTo(expected.getLinks());
        assertThat(actual.getItems().get(0).getCompanyNumber())
                .isEqualTo(expected.getItems().get(0).getCompanyNumber());
        assertThat(actual.getItems().get(1).getCompanyNumber())
                .isEqualTo(expected.getItems().get(1).getCompanyNumber());
        assertThat(actual.getItems().get(0).getLocality())
                .isEqualTo(expected.getItems().get(0).getLocality());
        assertThat(actual.getItems().get(1).getLocality())
                .isEqualTo(expected.getItems().get(1).getLocality());
        assertThat(actual.getItems().get(0).getLinks())
                .isEqualTo(expected.getItems().get(0).getLinks());
        assertThat(actual.getItems().get(1).getLinks())
                .isEqualTo(expected.getItems().get(1).getLinks());
    }

    @And("has_charges is false for {string}")
    public void has_chargesIsTrueFor(String companyNumber) {
        CompanyProfile companyProfile = new CompanyProfile().data(companyProfileService.get(companyNumber).companyProfile);
        assertThat(companyProfile.getData().getHasCharges()).isFalse();
    }

    @And("the has_charges field is true for {string}")
    public void theHas_chargesFieldIsTrueFor(String companyNumber) {
        CompanyProfile companyProfile = new CompanyProfile().data(companyProfileService.get(companyNumber).companyProfile);
        assertThat(companyProfile.getData().getHasCharges()).isTrue();
    }

    @When("I send PATCH request with payload {string} and company number {string} for charges")
    public void iSendPATCHRequestWithPayloadAndCompanyNumberForCharges(String dataFile, String companyNumber) throws IOException {
        WiremockTestConfig.stubKafkaApi(HttpStatus.OK.value());

        File file = new ClassPathResource("/json/input/" + dataFile + ".json").getFile();
        CompanyProfile companyProfile = objectMapper.readValue(file, CompanyProfile.class);

        this.contextId = "5234234234";
        this.companyNumber = companyNumber;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("x-request-id", "5234234234");
        headers.set("ERIC-Identity", "SOME_IDENTITY");
        headers.set("ERIC-Identity-Type", "key");
        headers.add("ERIC-Authorised-Key-Privileges", "internal-app");

        HttpEntity<CompanyProfile> request = new HttpEntity<>(companyProfile, headers);
        String uri = "/company/{company_number}/links/charges";
        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.PATCH, request, Void.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCode().value());
    }
}
