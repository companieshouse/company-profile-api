package uk.gov.companieshouse.company.profile.steps;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class CommonApiSteps {

    private ResponseEntity<String> lastResponse;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Given("Company profile api service is running")
    public void theApplicationRunning() {
        assertThat(restTemplate).isNotNull();
        lastResponse = null;
    }

    @When("the client invokes {string} endpoint")
    public void theClientInvokesAnEndpoint(String url) {
        lastResponse = restTemplate.getForEntity(url, String.class);
    }

    @Then("the client receives a status code of {int}")
    public void theClientReceivesStatusCodeOf(int code) {
        assertThat(lastResponse.getStatusCode()).isEqualTo(HttpStatus.valueOf(code));
    }

    @And("the client receives a response body of {string}")
    public void theClientReceivesRawResponse(String response) {
        assertThat(lastResponse.getBody()).isEqualTo(response);
    }

}
