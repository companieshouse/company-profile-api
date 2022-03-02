package uk.gov.companieshouse.company.profile.repository;

import org.junit.jupiter.api.Assertions;
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
import uk.gov.companieshouse.api.model.company.CompanyProfileApi;

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


    @Test
    void should_return_mongodb_as_running() {
        Assertions.assertTrue(mongoDBContainer.isRunning());
    }

    @Test
    void should_return_company_record_when_one_exists() {
        CompanyProfileApi companyProfile = new CompanyProfileApi();
        companyProfile.setCompanyNumber(MOCK_COMPANY_NUMBER);
        this.companyProfileRepository.save(companyProfile);

        Assertions.assertTrue(
                this.companyProfileRepository.findCompanyProfileApiByCompanyNumber(MOCK_COMPANY_NUMBER).isPresent());
    }

    @Test
    void should_return_empty_optional_when_company_record_does_not_exist() {
        CompanyProfileApi companyProfile = new CompanyProfileApi();
        companyProfile.setCompanyNumber("242424");
        this.companyProfileRepository.save(companyProfile);

        Assertions.assertTrue(
                this.companyProfileRepository.findCompanyProfileApiByCompanyNumber("othernumber").isEmpty());
    }

}
