package uk.gov.companieshouse.company.profile.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.company.profile.configuration.AbstractMongoConfig;
import uk.gov.companieshouse.company.profile.model.CompanyProfileDocument;

@Testcontainers
@DataMongoTest(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
class RepositoryITest extends AbstractMongoConfig {

    private static final String MOCK_COMPANY_NUMBER = "6146287";

    @Autowired
    private CompanyProfileRepository companyProfileRepository;

    @BeforeEach
    void setup() {
        this.companyProfileRepository.deleteAll();
    }

    @Test
    void should_return_company_record_when_one_exists() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        CompanyProfileDocument companyProfileDocument = new CompanyProfileDocument(companyData);
        companyProfileDocument.setId(MOCK_COMPANY_NUMBER);

        this.companyProfileRepository.save(companyProfileDocument);

        Assertions.assertTrue(
                this.companyProfileRepository.findById(MOCK_COMPANY_NUMBER).isPresent());
    }

    @Test
    void should_return_empty_optional_when_company_record_does_not_exist() {
        Data companyData = new Data().companyNumber("242424");
        CompanyProfileDocument companyProfileDocument = new CompanyProfileDocument(companyData);
        companyProfileDocument.setId("242424");

        this.companyProfileRepository.save(companyProfileDocument);

        Assertions.assertTrue(
                this.companyProfileRepository.findById("othernumber").isEmpty());
    }

}
