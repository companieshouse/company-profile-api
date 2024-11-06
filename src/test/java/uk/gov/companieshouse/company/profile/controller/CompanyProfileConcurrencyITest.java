package uk.gov.companieshouse.company.profile.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.company.Links;
import uk.gov.companieshouse.api.model.CompanyProfileDocument;
import uk.gov.companieshouse.company.profile.api.CompanyProfileApiService;
import uk.gov.companieshouse.company.profile.model.VersionedCompanyProfileDocument;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;
import uk.gov.companieshouse.company.profile.service.CompanyProfileService;
import java.util.Optional;

@Testcontainers
@SpringBootTest
class CompanyProfileConcurrencyITest {

    private static final String COMPANY_NUMBER = "6146287";
    private static final String CONTEXT_ID = "123456";

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:5.0.12");

    @Autowired
    private CompanyProfileService companyProfileService;

    @Autowired
    private CompanyProfileRepository companyProfileRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @MockBean
    private CompanyProfileApiService companyProfileApiService;

    @BeforeAll
    static void start() {
        System.setProperty("spring.data.mongodb.uri", mongoDBContainer.getReplicaSetUrl());
    }

    @BeforeEach
    void setup() {
        companyProfileRepository.deleteAll();
    }

    @Test
    void shouldCreateNewVersionDocument() {
        // given
        CompanyProfile companyProfile = makeBaseCompanyProfile();

        // when
        companyProfileService.processCompanyProfile(CONTEXT_ID, COMPANY_NUMBER, companyProfile);

        // then
        Optional<VersionedCompanyProfileDocument> actual = companyProfileRepository.findById(COMPANY_NUMBER);
        assertTrue(actual.isPresent());
        assertEquals(0, actual.get().getVersion());
    }

    @Test
    void shouldUpdateLegacyUnversionedDocumentWithAVersion() {
        // given
        CompanyProfileDocument document = buildLegacyCompanyProfileDocument();

        mongoTemplate.save(document);
        CompanyProfile companyProfile = makeBaseCompanyProfile();

        // when
        companyProfileService.processCompanyProfile(CONTEXT_ID, COMPANY_NUMBER, companyProfile);

        // then
        Optional<VersionedCompanyProfileDocument> actual = companyProfileRepository.findById(COMPANY_NUMBER);
        assertTrue(actual.isPresent());
        assertEquals(0, actual.get().getVersion());
    }

    @Test
    void shouldUpdateVersionedDocument() {
        // given
        VersionedCompanyProfileDocument document = buildCompanyProfileDocument();

        document = companyProfileRepository.insert(document);
        assertEquals(0, document.getVersion());
        CompanyProfile companyProfile = makeBaseCompanyProfile();

        // when
        companyProfileService.processCompanyProfile(CONTEXT_ID, COMPANY_NUMBER, companyProfile);

        // then
        Optional<VersionedCompanyProfileDocument> actual = companyProfileRepository.findById(COMPANY_NUMBER);
        assertTrue(actual.isPresent());
        assertEquals(1, actual.get().getVersion());
    }

    @Test
    void shouldDeleteVersionedDocument() {
        // given
        VersionedCompanyProfileDocument document = buildCompanyProfileDocument();

        document = companyProfileRepository.insert(document);
        assertEquals(0, document.getVersion());

        // when
        companyProfileService.deleteCompanyProfile(CONTEXT_ID, COMPANY_NUMBER);

        // then
        Optional<VersionedCompanyProfileDocument> actual = companyProfileRepository.findById(COMPANY_NUMBER);
        assertTrue(actual.isEmpty());
    }

    @Test
    void shouldDeleteLegacyUnversionedDocument() {
        // given
        CompanyProfileDocument document = buildLegacyCompanyProfileDocument();

        mongoTemplate.save(document);

        // when
        companyProfileService.deleteCompanyProfile(CONTEXT_ID, COMPANY_NUMBER);

        // then
        Optional<VersionedCompanyProfileDocument> actual = companyProfileRepository.findById(COMPANY_NUMBER);
        assertTrue(actual.isEmpty());
    }

    @Test
    void shouldUpdateVersionedDocumentWhenPatchLinksRequest() {
        // given
        VersionedCompanyProfileDocument document = buildCompanyProfileDocument();

        document = companyProfileRepository.insert(document);
        assertEquals(0, document.getVersion());

        // when
//        companyProfileService.processLinkRequest();

        // then
    }

     private static VersionedCompanyProfileDocument buildCompanyProfileDocument() {
        VersionedCompanyProfileDocument companyProfileDocument = new VersionedCompanyProfileDocument();
                companyProfileDocument.setId(COMPANY_NUMBER);
                companyProfileDocument.setCompanyProfile(new Data()
                        .links(new Links()
                                .self("/company/" + COMPANY_NUMBER)));
        companyProfileDocument.setHasMortgages(false);
        companyProfileDocument.version(0L);
        return companyProfileDocument;
    }

    private static CompanyProfileDocument buildLegacyCompanyProfileDocument() {
        CompanyProfileDocument companyProfileDocument = new CompanyProfileDocument();
        companyProfileDocument.setId(COMPANY_NUMBER);
        companyProfileDocument.setCompanyProfile(new Data()
                .links(new Links()
                        .self("/company/" + COMPANY_NUMBER)));
        companyProfileDocument.setHasMortgages(false);
        return companyProfileDocument;
    }

    private static @NotNull CompanyProfile makeBaseCompanyProfile() {
        CompanyProfile companyProfile = new CompanyProfile();
        companyProfile.setHasMortgages(false);
        companyProfile.setData(new Data());
        companyProfile.getData().setLinks(new Links());
        companyProfile.getData().setHasCharges(null);
        companyProfile.getData().setHasBeenLiquidated(false);
        companyProfile.getData().setCompanyNumber(COMPANY_NUMBER);
        return companyProfile;
    }
}
