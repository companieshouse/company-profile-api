package uk.gov.companieshouse.company.profile.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bson.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import uk.gov.companieshouse.company.profile.CompanyProfileApiApplication;
import uk.gov.companieshouse.company.profile.model.VersionedCompanyProfileDocument;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(classes = CompanyProfileApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CompanyProfileRepositoryTest {
    private static final String COMPANY_NUMBER = "BR123456";
    private static final String PARENT_COMPANY_NUMBER = "FR123456";
    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:5");
    @Autowired
    private CompanyProfileRepository repository;
    @Autowired
    private MongoTemplate mongoTemplate;
    private Document templateDocument;

    @BeforeAll
    static void start() {
        System.setProperty("spring.data.mongodb.uri", mongoDBContainer.getReplicaSetUrl());
    }

    @BeforeEach
    void setup() throws IOException {
        mongoTemplate.dropCollection("company_profile");
        mongoTemplate.createCollection("company_profile");

        templateDocument = Document.parse(
                IOUtils.resourceToString("/parent-company-number-data.json", StandardCharsets.UTF_8));
    }

    @Test
    void findAllCompaniesWithSameParentCompanyNumber() {
        // given
        insertCompanyProfile(COMPANY_NUMBER + 1, PARENT_COMPANY_NUMBER);
        insertCompanyProfile(COMPANY_NUMBER + 2, PARENT_COMPANY_NUMBER);
        insertCompanyProfile(COMPANY_NUMBER + 3, PARENT_COMPANY_NUMBER + 1);

        // when
        List<VersionedCompanyProfileDocument> result = repository.findAllByParentCompanyNumber(PARENT_COMPANY_NUMBER);

        // then
        assertEquals(COMPANY_NUMBER + 1, result.get(0).getId());
        assertEquals(COMPANY_NUMBER + 2, result.get(1).getId());
        assertEquals(2, result.size());
    }

    private void insertCompanyProfile(String companyNumber, String parentCompanyNumber) {
        templateDocument.put("_id", companyNumber);
        templateDocument.put("parent_company_number", parentCompanyNumber);

        mongoTemplate.insert(templateDocument, "company_profile");
    }
}
