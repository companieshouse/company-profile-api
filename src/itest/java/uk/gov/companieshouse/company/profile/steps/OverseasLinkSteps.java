package uk.gov.companieshouse.company.profile.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.companieshouse.api.model.CompanyProfileDocument;
import uk.gov.companieshouse.company.profile.configuration.WiremockTestConfig;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.companieshouse.company.profile.configuration.AbstractMongoConfig.mongoDBContainer;

public class OverseasLinkSteps {

    private static final String OVERSEAS_LINK = "/company/%s";

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

    @And("an Overseas link to {string} does exist for {string}")
    public void theOverseasLinkDoesExistFor(String parentCompanyNumber, String companyNumber) {
        getOverseasLink(companyNumber, parentCompanyNumber);
    }
    @And("an Overseas link does not exist for {string}")
    public void theOverseasLinkDoesNotExistFor(String companyNumber) {
        overseasLinkShouldNotExist(companyNumber);
    }
    @And("an Overseas link to {string} should have been added for {string}")
    public void theOverseasLinkHasBeenAddedFor(String parentCompanyNumber, String companyNumber) {
        getOverseasLink(companyNumber, parentCompanyNumber);
    }
    @And("the Overseas link to {string} should still exist for {string}")
    public void theOverseasLinkStillExistsFor(String parentCompanyNumber, String companyNumber) {
        getOverseasLink(companyNumber, parentCompanyNumber);
    }

    private void getOverseasLink(String companyNumber, String parentCompanyNumber) {
        Optional<CompanyProfileDocument> document = companyProfileRepository.findById(companyNumber);
        assertThat(document).isPresent();
        System.out.println(document.get().getCompanyProfile().getLinks());
        assertThat(document.get().getCompanyProfile().getLinks().getOverseas()).isEqualTo(String.format(OVERSEAS_LINK, parentCompanyNumber));
    }

    private void overseasLinkShouldNotExist(String companyNumber) {
        Optional<CompanyProfileDocument> document = companyProfileRepository.findById(companyNumber);
        assertThat(document).isPresent();
        assertThat(document.get().getCompanyProfile().getLinks().getOverseas()).isNullOrEmpty();
    }
}
