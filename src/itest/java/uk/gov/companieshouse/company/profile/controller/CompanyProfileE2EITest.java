package uk.gov.companieshouse.company.profile.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import com.fasterxml.jackson.core.io.BigDecimalParser;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.company.Links;
import uk.gov.companieshouse.api.model.CompanyProfileDocument;
import uk.gov.companieshouse.api.model.Updated;
import uk.gov.companieshouse.company.profile.CompanyProfileApiApplication;
import uk.gov.companieshouse.company.profile.api.CompanyProfileApiService;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(classes = CompanyProfileApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class CompanyProfileE2EITest {

    private static final String COMPANY_NUMBER = "12345678";
    private static final String COMPANY_PROFILE_COLLECTION = "company_profile";
    private static final String ADD_LINK_ENDPOINT = "/company/{company_number}/links/{link_type}";

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.0.10");

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompanyProfileApiService companyProfileApiService;

    @BeforeAll
    static void start() {
        System.setProperty("spring.data.mongodb.uri", mongoDBContainer.getReplicaSetUrl());
    }

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(COMPANY_PROFILE_COLLECTION);
        mongoTemplate.createCollection(COMPANY_PROFILE_COLLECTION);
    }

    @ParameterizedTest
    @CsvSource({
            "filing-history , filing_history_delta",
            "exemptions , exemption_delta",
            "officers , officer_delta",
            "persons-with-significant-control , psc_delta",
            "persons-with-significant-control-statements , psc_statement_delta"
    })
    @DisplayName("Successfully add link to company profile")
    void testAddLink(final String linkType, final String deltaType) throws Exception {
        // given
        final String expectedLink = String.format("/company/%s/%s", COMPANY_NUMBER, linkType);
        final String oldEtag = "oldEtag";

        CompanyProfileDocument document = new CompanyProfileDocument()
                .setId(COMPANY_NUMBER)
                .setCompanyProfile(new Data()
                        .etag(oldEtag)
                        .links(new Links()
                                .self("/company/" + COMPANY_NUMBER)));

        mongoTemplate.save(document, COMPANY_PROFILE_COLLECTION);

        // when
        final ResultActions result = mockMvc.perform(patch(ADD_LINK_ENDPOINT, COMPANY_NUMBER, linkType)
                .header("ERIC-Identity", "123")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("x-request-id", "context_id")
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final CompanyProfileDocument actualDocument = Objects.requireNonNull(mongoTemplate.findById(COMPANY_NUMBER, CompanyProfileDocument.class));
        final Updated updated = actualDocument.getUpdated();
        final Data companyProfile = actualDocument.getCompanyProfile();

        final String actualLink = filterLinkType(linkType, companyProfile.getLinks());

        assertEquals(expectedLink, actualLink);
        assertNotNull(updated.getAt());
        assertEquals(deltaType, updated.getType());
        assertEquals("context_id", updated.getBy());
        assertNotEquals(oldEtag, companyProfile.getEtag());
        verify(companyProfileApiService).invokeChsKafkaApi("context_id", COMPANY_NUMBER);
    }

    private String filterLinkType(final String linkType, Links links) {
        String actualLink;
        switch (linkType) {
            case "filing-history":
                actualLink = links.getFilingHistory();
                break;
            case "exemptions":
                actualLink = links.getExemptions();
                break;
            case "officers":
                actualLink = links.getOfficers();
                break;
            case "persons-with-significant-control":
                actualLink = links.getPersonsWithSignificantControl();
                break;
            case "persons-with-significant-control-statements":
                actualLink = links.getPersonsWithSignificantControlStatements();
                break;
            default: actualLink = "DID NOT MATCH LINK TYPE";
        }
        return actualLink;
    }
}
