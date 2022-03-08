package uk.gov.companieshouse.company.profile.repository;

import com.google.gson.Gson;
import org.junit.Before;
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
import uk.gov.companieshouse.company.profile.domain.CompanyProfileDao;

@Testcontainers
@DataMongoTest(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
class RepositoryITest {

    private static final String MOCK_COMPANY_NUMBER = "6146287";

    // static, so container starts before the application context and we can set properties
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.10"));

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private CompanyProfileRepository companyProfileRepository;

    @BeforeEach
    void setup() {
        this.companyProfileRepository.deleteAll();
    }

    @Test
    void should_return_mongodb_as_running() {
        Assertions.assertTrue(mongoDBContainer.isRunning());
    }

    @Test
    void should_return_company_record_when_one_exists() {
        CompanyProfile companyProfile = new CompanyProfile();
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        companyProfile.setData(companyData);
        CompanyProfileDao companyProfileDao = new CompanyProfileDao(companyProfile);

        this.companyProfileRepository.save(companyProfileDao);

        System.out.println(this.companyProfileRepository.findAll().get(0));

        Assertions.assertTrue(
                this.companyProfileRepository.findCompanyProfileDaoByCompanyProfile_Data_CompanyNumber(MOCK_COMPANY_NUMBER).isPresent());
    }

    @Test
    void should_return_empty_optional_when_company_record_does_not_exist() {
        CompanyProfile companyProfile = new CompanyProfile();
        Data companyData = new Data().companyNumber("242424");
        companyProfile.setData(companyData);
        CompanyProfileDao companyProfileDao = new CompanyProfileDao(companyProfile);

        this.companyProfileRepository.save(companyProfileDao);

        Assertions.assertTrue(
                this.companyProfileRepository.findCompanyProfileDaoByCompanyProfile_Data_CompanyNumber("othernumber").isEmpty());
    }

}
