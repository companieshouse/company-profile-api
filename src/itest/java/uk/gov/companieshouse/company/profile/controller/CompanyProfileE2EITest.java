package uk.gov.companieshouse.company.profile.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
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
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.company.Links;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.CompanyProfileDocument;
import uk.gov.companieshouse.api.model.Updated;
import uk.gov.companieshouse.company.profile.CompanyProfileApiApplication;
import uk.gov.companieshouse.company.profile.api.CompanyProfileApiService;
import uk.gov.companieshouse.company.profile.model.VersionedCompanyProfileDocument;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;
import java.util.Objects;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(classes = CompanyProfileApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class CompanyProfileE2EITest {

    private static final String COMPANY_NUMBER = "12345678";
    private static final String UK_ESTABLISHMENTS_LINK = String.format("/company/%s/uk-establishments", COMPANY_NUMBER);
    private static final String PERSONS_WITH_SIGNIFICANT_CONTROL_STATEMENTS_LINK = String.format("/company/%s/persons-with-significant-control-statements", COMPANY_NUMBER);
    private static final String PERSONS_WITH_SIGNIFICANT_CONTROL_LINK = String.format("/company/%s/persons-with-significant-control", COMPANY_NUMBER);
    private static final String OFFICERS_LINK = String.format("/company/%s/officers", COMPANY_NUMBER);
    private static final String INSOLVENCY_LINK = String.format("/company/%s/insolvency", COMPANY_NUMBER);
    private static final String FILING_HISTORY_LINK = String.format("/company/%s/filing-history", COMPANY_NUMBER);
    private static final String EXEMPTIONS_LINK = String.format("/company/%s/exemptions", COMPANY_NUMBER);
    private static final String CHARGES_LINK = String.format("/company/%s/charges", COMPANY_NUMBER);
    private static final String SELF_LINK = String.format("/company/%s", COMPANY_NUMBER);
    private static final String CONTEXT_ID = "context_id";
    private static final String ADD_LINK_ENDPOINT = "/company/{company_number}/links/{link_type}";
    private static final String DELETE_LINK_ENDPOINT = "/company/{company_number}/links/{link_type}/delete";
    private static final String ADD_LINK_ENDPOINT_LEGACY = "/company/{company_number}/links";


    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:5.0.12");

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private CompanyProfileRepository companyProfileRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

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

    @ParameterizedTest
    @CsvSource({
            "charges, charges_delta",
            "exemptions , exemption_delta",
            "filing-history , filing_history_delta",
            "insolvency, insolvency_delta",
            "officers , officer_delta",
            "persons-with-significant-control , psc_delta",
            "persons-with-significant-control-statements , psc_statement_delta",
            "uk-establishments , uk_establishment_delta",
            "registers, registers_delta"
    })
    @DisplayName("Successfully add link to existing versioned company profile document")
    void testAddLinkExistingDocument(final String linkType, final String deltaType) throws Exception {
        // given
        final String expectedLink = String.format("/company/%s/%s", COMPANY_NUMBER, linkType);
        final String oldEtag = "oldEtag";

        VersionedCompanyProfileDocument document = new VersionedCompanyProfileDocument();
                document.setId(COMPANY_NUMBER)
                .setCompanyProfile(new Data()
                        .etag(oldEtag)
                        .links(new Links()
                                .self("/company/" + COMPANY_NUMBER)));
                document.version(0L);

        companyProfileRepository.insert(document);

        // when
        final ResultActions result = mockMvc.perform(patch(ADD_LINK_ENDPOINT, COMPANY_NUMBER, linkType)
                .header("ERIC-Identity", "123")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("x-request-id", CONTEXT_ID)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final VersionedCompanyProfileDocument actualDocument = Objects.requireNonNull(mongoTemplate.findById(COMPANY_NUMBER, VersionedCompanyProfileDocument.class));
        final Updated updated = actualDocument.getUpdated();
        final Data companyProfile = actualDocument.getCompanyProfile();

        final String actualLink = filterLinkType(linkType, companyProfile.getLinks());

        assertEquals(expectedLink, actualLink);
        assertNotNull(updated.getAt());
        assertEquals(deltaType, updated.getType());
        assertEquals(CONTEXT_ID, updated.getBy());
        assertEquals(1L, actualDocument.getVersion());
        assertNotEquals(oldEtag, companyProfile.getEtag());
        verify(companyProfileApiService).invokeChsKafkaApi(CONTEXT_ID, COMPANY_NUMBER);
    }

    @ParameterizedTest
    @CsvSource({
            "charges, charges_delta",
            "exemptions , exemption_delta",
            "filing-history , filing_history_delta",
            "insolvency, insolvency_delta",
            "officers , officer_delta",
            "persons-with-significant-control , psc_delta",
            "persons-with-significant-control-statements , psc_statement_delta",
            "uk-establishments , uk_establishment_delta",
            "registers, registers_delta"
    })
    @DisplayName("Successfully add link to existing versioned company profile document")
    void testAddLinkLegacyDocument(final String linkType, final String deltaType) throws Exception {
        // given
        final String expectedLink = String.format("/company/%s/%s", COMPANY_NUMBER, linkType);
        final String oldEtag = "oldEtag";

        CompanyProfileDocument companyProfileDocument = new CompanyProfileDocument();
        companyProfileDocument.setId(COMPANY_NUMBER);
        companyProfileDocument.setCompanyProfile(new Data()
                .links(new Links()
                        .self("/company/" + COMPANY_NUMBER)));
        companyProfileDocument.setHasMortgages(false);

        mongoTemplate.save(companyProfileDocument);

        // when
        final ResultActions result = mockMvc.perform(patch(ADD_LINK_ENDPOINT, COMPANY_NUMBER, linkType)
                .header("ERIC-Identity", "123")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("x-request-id", CONTEXT_ID)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final VersionedCompanyProfileDocument actualDocument = Objects.requireNonNull(mongoTemplate.findById(COMPANY_NUMBER, VersionedCompanyProfileDocument.class));
        final Updated updated = actualDocument.getUpdated();
        final Data companyProfile = actualDocument.getCompanyProfile();

        final String actualLink = filterLinkType(linkType, companyProfile.getLinks());

        assertEquals(expectedLink, actualLink);
        assertNotNull(updated.getAt());
        assertEquals(deltaType, updated.getType());
        assertEquals(CONTEXT_ID, updated.getBy());
        assertEquals(0L, actualDocument.getVersion());
        assertNotEquals(oldEtag, companyProfile.getEtag());
        verify(companyProfileApiService).invokeChsKafkaApi(CONTEXT_ID, COMPANY_NUMBER);
    }

    @ParameterizedTest
    @CsvSource({
            "charges",
            "insolvency",
            "registers"
    })
    void testAddLinkLegacyEndpoint(final String linkType) throws Exception {
        // given
        final String expectedLink = String.format("/company/%s/%s", COMPANY_NUMBER, linkType);
        final String oldEtag = "oldEtag";

        VersionedCompanyProfileDocument document = new VersionedCompanyProfileDocument();
        document.setId(COMPANY_NUMBER)
                .setCompanyProfile(new Data()
                        .etag(oldEtag)
                        .links(new Links()
                                .self("/company/" + COMPANY_NUMBER)));
        document.version(0L);

        companyProfileRepository.insert(document);

        when(companyProfileApiService.invokeChsKafkaApi(any(), any())).thenReturn(new ApiResponse<>(200, null));

        // when
        final ResultActions result = mockMvc.perform(patch(ADD_LINK_ENDPOINT_LEGACY, COMPANY_NUMBER)
                .header("ERIC-Identity", "123")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("x-request-id", CONTEXT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeBaseLegacyLinksCompanyProfile())));

        // then
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final VersionedCompanyProfileDocument actualDocument = Objects.requireNonNull(mongoTemplate.findById(COMPANY_NUMBER, VersionedCompanyProfileDocument.class));
        final Updated updated = actualDocument.getUpdated();
        final Data companyProfile = actualDocument.getCompanyProfile();

        final String actualLink = filterLinkType(linkType, companyProfile.getLinks());

        assertEquals(expectedLink, actualLink);
        assertNotNull(updated.getAt());
        assertEquals(CONTEXT_ID, updated.getBy());
        assertEquals(1L, actualDocument.getVersion());
        assertNotEquals(oldEtag, companyProfile.getEtag());
        verify(companyProfileApiService).invokeChsKafkaApi(CONTEXT_ID, COMPANY_NUMBER);
    }

    @ParameterizedTest
    @CsvSource({
            "charges",
            "insolvency",
            "registers"
    })
    void testAddLinkLegacyEndpointLegacyDocument(final String linkType) throws Exception {
        // given
        final String expectedLink = String.format("/company/%s/%s", COMPANY_NUMBER, linkType);
        final String oldEtag = "oldEtag";

        CompanyProfileDocument companyProfileDocument = new CompanyProfileDocument();
        companyProfileDocument.setId(COMPANY_NUMBER);
        companyProfileDocument.setCompanyProfile(new Data()
                .links(new Links()
                        .self("/company/" + COMPANY_NUMBER)));
        companyProfileDocument.setHasMortgages(false);

        mongoTemplate.save(companyProfileDocument);

        when(companyProfileApiService.invokeChsKafkaApi(any(), any())).thenReturn(new ApiResponse<>(200, null));

        // when
        final ResultActions result = mockMvc.perform(patch(ADD_LINK_ENDPOINT_LEGACY, COMPANY_NUMBER)
                .header("ERIC-Identity", "123")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("x-request-id", CONTEXT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(makeBaseLegacyLinksCompanyProfile())));

        // then
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final VersionedCompanyProfileDocument actualDocument = Objects.requireNonNull(mongoTemplate.findById(COMPANY_NUMBER, VersionedCompanyProfileDocument.class));
        final Updated updated = actualDocument.getUpdated();
        final Data companyProfile = actualDocument.getCompanyProfile();

        final String actualLink = filterLinkType(linkType, companyProfile.getLinks());

        assertEquals(expectedLink, actualLink);
        assertNotNull(updated.getAt());
        assertEquals(CONTEXT_ID, updated.getBy());
        assertEquals(0L, actualDocument.getVersion());
        assertNotEquals(oldEtag, companyProfile.getEtag());
        verify(companyProfileApiService).invokeChsKafkaApi(CONTEXT_ID, COMPANY_NUMBER);
    }

    @ParameterizedTest
    @CsvSource({
            "charges, charges_delta",
            "exemptions , exemption_delta",
            "filing-history , filing_history_delta",
            "insolvency, insolvency_delta",
            "officers , officer_delta",
            "persons-with-significant-control , psc_delta",
            "persons-with-significant-control-statements , psc_statement_delta",
            "uk-establishments , uk_establishment_delta"
    })
    @DisplayName("Successfully add link to existing versioned company profile document")
    void testDeleteLink(final String linkType, final String deltaType) throws Exception {
        // given
        final String oldEtag = "oldEtag";

        VersionedCompanyProfileDocument document = new VersionedCompanyProfileDocument();
        document.setId(COMPANY_NUMBER)
                .setCompanyProfile(new Data()
                        .etag(oldEtag)
                        .links(new Links()
                                .self(SELF_LINK)
                                .charges(CHARGES_LINK)
                                .exemptions(EXEMPTIONS_LINK)
                                .filingHistory(FILING_HISTORY_LINK)
                                .insolvency(INSOLVENCY_LINK)
                                .officers(OFFICERS_LINK)
                                .personsWithSignificantControl(PERSONS_WITH_SIGNIFICANT_CONTROL_LINK)
                                .personsWithSignificantControlStatements(PERSONS_WITH_SIGNIFICANT_CONTROL_STATEMENTS_LINK)
                                .ukEstablishments(UK_ESTABLISHMENTS_LINK)));
        document.version(0L);

        companyProfileRepository.insert(document);

        // when
        final ResultActions result = mockMvc.perform(patch(DELETE_LINK_ENDPOINT, COMPANY_NUMBER, linkType)
                .header("ERIC-Identity", "123")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("x-request-id", CONTEXT_ID)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final VersionedCompanyProfileDocument actualDocument = Objects.requireNonNull(mongoTemplate.findById(COMPANY_NUMBER, VersionedCompanyProfileDocument.class));
        final Updated updated = actualDocument.getUpdated();
        final Data companyProfile = actualDocument.getCompanyProfile();

        final String actualLink = filterLinkType(linkType, companyProfile.getLinks());

        assertNull(actualLink);
        assertNotNull(updated.getAt());
        assertEquals(deltaType, updated.getType());
        assertEquals(CONTEXT_ID, updated.getBy());
        assertEquals(1L, actualDocument.getVersion());
        assertNotEquals(oldEtag, companyProfile.getEtag());
        verify(companyProfileApiService).invokeChsKafkaApi(CONTEXT_ID, COMPANY_NUMBER);
    }

    @ParameterizedTest
    @CsvSource({
            "charges, charges_delta",
            "exemptions , exemption_delta",
            "filing-history , filing_history_delta",
            "insolvency, insolvency_delta",
            "officers , officer_delta",
            "persons-with-significant-control , psc_delta",
            "persons-with-significant-control-statements , psc_statement_delta",
            "uk-establishments , uk_establishment_delta"
    })
    @DisplayName("Successfully delete link from existing versioned company profile document")
    void testDeleteLinkLegacyDocument(final String linkType, final String deltaType) throws Exception {
        // given
        final String oldEtag = "oldEtag";

        CompanyProfileDocument document = new CompanyProfileDocument();
        document.setId(COMPANY_NUMBER)
                .setCompanyProfile(new Data()
                        .etag(oldEtag)
                        .links(new Links()
                                .self(SELF_LINK)
                                .charges(CHARGES_LINK)
                                .exemptions(EXEMPTIONS_LINK)
                                .filingHistory(FILING_HISTORY_LINK)
                                .insolvency(INSOLVENCY_LINK)
                                .officers(OFFICERS_LINK)
                                .personsWithSignificantControl(PERSONS_WITH_SIGNIFICANT_CONTROL_LINK)
                                .personsWithSignificantControlStatements(PERSONS_WITH_SIGNIFICANT_CONTROL_STATEMENTS_LINK)
                                .ukEstablishments(UK_ESTABLISHMENTS_LINK)));
        document.setHasMortgages(true);

        mongoTemplate.save(document);

        // when
        final ResultActions result = mockMvc.perform(patch(DELETE_LINK_ENDPOINT, COMPANY_NUMBER, linkType)
                .header("ERIC-Identity", "123")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("x-request-id", CONTEXT_ID)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final VersionedCompanyProfileDocument actualDocument = Objects.requireNonNull(mongoTemplate.findById(COMPANY_NUMBER, VersionedCompanyProfileDocument.class));
        final Updated updated = actualDocument.getUpdated();
        final Data companyProfile = actualDocument.getCompanyProfile();

        final String actualLink = filterLinkType(linkType, companyProfile.getLinks());

        assertNull(actualLink);
        assertNotNull(updated.getAt());
        assertEquals(deltaType, updated.getType());
        assertEquals(CONTEXT_ID, updated.getBy());
        assertEquals(0L, actualDocument.getVersion());
        assertNotEquals(oldEtag, companyProfile.getEtag());
        verify(companyProfileApiService).invokeChsKafkaApi(CONTEXT_ID, COMPANY_NUMBER);
    }


    private String filterLinkType(final String linkType, Links links) {
        return switch (linkType) {
            case "charges" -> links.getCharges();
            case "exemptions" -> links.getExemptions();
            case "filing-history" -> links.getFilingHistory();
            case "insolvency" -> links.getInsolvency();
            case "officers" -> links.getOfficers();
            case "persons-with-significant-control" -> links.getPersonsWithSignificantControl();
            case "persons-with-significant-control-statements" ->
                    links.getPersonsWithSignificantControlStatements();
            case "uk-establishments" -> links.getUkEstablishments();
            case "registers" -> links.getRegisters();
            default -> "DID NOT MATCH LINK TYPE";
        };
    }

    private static @NotNull CompanyProfile makeBaseLegacyLinksCompanyProfile() {
        CompanyProfile companyProfile = new CompanyProfile();
        companyProfile.setHasMortgages(false);
        companyProfile.setData(new Data());
        companyProfile.getData().setLinks(new Links()
                .charges(CHARGES_LINK)
                .insolvency(INSOLVENCY_LINK)
                .registers(String.format("/company/%s/registers", COMPANY_NUMBER)));
        companyProfile.getData().setHasCharges(null);
        companyProfile.getData().setHasBeenLiquidated(false);
        companyProfile.getData().setCompanyNumber(COMPANY_NUMBER);
        return companyProfile;
    }
}
