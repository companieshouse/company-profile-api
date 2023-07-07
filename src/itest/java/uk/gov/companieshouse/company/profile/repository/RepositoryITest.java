package uk.gov.companieshouse.company.profile.repository;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.model.CompanyProfileDocument;
import uk.gov.companieshouse.api.model.Updated;
import uk.gov.companieshouse.company.profile.configuration.AbstractMongoConfig;

import java.time.LocalDateTime;

@Testcontainers
@DataMongoTest(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
class RepositoryITest extends AbstractMongoConfig {

    private static final String MOCK_COMPANY_NUMBER = "6146287";

    @Autowired
    private CompanyProfileRepository companyProfileRepository;


    @BeforeAll
    static void init(){
        mongoDBContainer.start();
    }

    @BeforeEach
    void setup() {
        this.companyProfileRepository.deleteAll();
    }

    @Test
    void should_return_company_record_when_one_exists() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        LocalDateTime localDateTime = LocalDateTime.now();
        Updated updated = new Updated(LocalDateTime.now(),
                "abc", "company_delta");
        CompanyProfileDocument companyProfileDocument = new CompanyProfileDocument(companyData, localDateTime, updated, false);
        companyProfileDocument.setId(MOCK_COMPANY_NUMBER);

        this.companyProfileRepository.save(companyProfileDocument);

        Assertions.assertTrue(
                this.companyProfileRepository.findById(MOCK_COMPANY_NUMBER).isPresent());
    }

    @Test
    void should_return_empty_optional_when_company_record_does_not_exist() {
        Data companyData = new Data().companyNumber("242424");
        LocalDateTime localDateTime = LocalDateTime.now();
        Updated updated = new Updated(LocalDateTime.now(),
                "abc", "company_delta");
        CompanyProfileDocument companyProfileDocument = new CompanyProfileDocument(companyData, localDateTime, updated, false);
        companyProfileDocument.setId("242424");

        this.companyProfileRepository.save(companyProfileDocument);

        Assertions.assertTrue(
                this.companyProfileRepository.findById("othernumber").isEmpty());
    }

    @AfterAll
    static void tear(){
        mongoDBContainer.stop();
    }
}
