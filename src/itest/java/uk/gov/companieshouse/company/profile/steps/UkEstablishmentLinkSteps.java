package uk.gov.companieshouse.company.profile.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.api.model.CompanyProfileDocument;
import uk.gov.companieshouse.company.profile.configuration.CucumberContext;
import uk.gov.companieshouse.company.profile.configuration.WiremockTestConfig;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.companieshouse.company.profile.configuration.AbstractMongoConfig.mongoDBContainer;

public class UkEstablishmentLinkSteps {

    private String contextId;
    private static final String ADD_PUT_ENDPOINT = "/company/00006400";
    private static final String UK_ESTABLISHMENTS_LINK = "/company/00006401/uk-establishments";

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

    @And("the uk-establishment link exists for {string}")
    public void verifyUkEstablishmentsLinkExists(String companyNumber) {
        Optional<CompanyProfileDocument> document = companyProfileRepository.findById(companyNumber);

        assertThat(document).isPresent();
        System.out.println(document.get().getCompanyProfile().getLinks());
        assertThat(document.get().getCompanyProfile().getLinks().getUkEstablishments()).isEqualTo(UK_ESTABLISHMENTS_LINK);
    }

    @And("the uk-establishment link does not exist for {string}")
    public void theUkEstablishmentsLinkDoesNotExistFor(String parentCompanyNumber) {
        Optional<CompanyProfileDocument> document = companyProfileRepository.findById(parentCompanyNumber);

        assertThat(document).isPresent();
        assertThat(document.get().getCompanyProfile().getLinks().getUkEstablishments()).isNullOrEmpty();
    }

    @And("the uk-establishment link does exist for {string}")
    public void theUkEstablishmentLinkDoesExistFor(String parentCompanyNumber) {
        Optional<CompanyProfileDocument> document = companyProfileRepository.findById(parentCompanyNumber);

        assertThat(document).isPresent();
        System.out.println(document.get().getCompanyProfile().getLinks());
        assertThat(document.get().getCompanyProfile().getLinks().getUkEstablishments()).isEqualTo("link");
    }
}
