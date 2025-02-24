package uk.gov.companieshouse.company.profile.controller;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.api.company.AccountingReferenceDate;
import uk.gov.companieshouse.api.company.AccountingRequirement;
import uk.gov.companieshouse.api.company.Accounts;
import uk.gov.companieshouse.api.company.BranchCompanyDetails;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.company.ForeignCompanyDetails;
import uk.gov.companieshouse.api.company.LastAccounts;
import uk.gov.companieshouse.api.company.Links;
import uk.gov.companieshouse.api.company.NextAccounts;
import uk.gov.companieshouse.api.company.OriginatingRegistry;
import uk.gov.companieshouse.api.company.PreviousCompanyNames;
import uk.gov.companieshouse.api.company.RegisteredOfficeAddress;
import uk.gov.companieshouse.company.profile.CompanyProfileApiApplication;
import uk.gov.companieshouse.company.profile.api.CompanyProfileApiService;
import uk.gov.companieshouse.company.profile.model.VersionedCompanyProfileDocument;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(classes = CompanyProfileApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class CompanyProfileFullE2EITest {

    private static final String CHILD_COMPANY_NUMBER = "BR005209";
    private static final String PARENT_COMPANY_NUMBER = "FC022112";
    private static final String CONTEXT_ID = "context_id";
    private static final String PUT_ENDPOINT = "/company/{company_number}/internal";
    private static final String DELETE_ENDPOINT = "/company/{company_number}/internal";
    private static final String UK_ESTABLISHMENT_TYPE = "uk-establishment";
    private static final String OVERSEA_COMPANY_TYPE = "oversea-company";
    private static final String CHILD_SELF_LINK = String.format("/company/%s", CHILD_COMPANY_NUMBER);
    private static final String PARENT_SELF_LINK = String.format("/company/%s", PARENT_COMPANY_NUMBER);
    private static final String UK_ESTABLISHMENT_LINK = String.format("/company/%s/uk-establishments", PARENT_COMPANY_NUMBER);
    private static final String OLD_ETAG = "oldEtag";
    private static final String STALE_DELTA_AT = "20250121064805856963";
    private static final String DELTA_AT = "20250121154805856963";
    private static final String NEW_DELTA_AT = "20250121174805856963";
    private static final DateTimeFormatter DELTA_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS")
            .withZone(UTC);

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0.20");

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private CompanyProfileRepository companyProfileRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
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
    void shouldPersistBaseParentFCCompanyProfileCorrectlyWhenBRDeltaReceivedOnPUTEndpoint() throws Exception {
        // given
        CompanyProfile request = makeBRPutRequest(DELTA_AT);

        // when
        final ResultActions result = mockMvc.perform(put(PUT_ENDPOINT, CHILD_COMPANY_NUMBER)
                .header("ERIC-Identity", "123")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("x-request-id", CONTEXT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final VersionedCompanyProfileDocument childDocument = Objects.requireNonNull(mongoTemplate.findById(CHILD_COMPANY_NUMBER, VersionedCompanyProfileDocument.class));
        final Data childCompanyProfile = childDocument.getCompanyProfile();

        final VersionedCompanyProfileDocument baseParentDocument = mongoTemplate.findById(PARENT_COMPANY_NUMBER, VersionedCompanyProfileDocument.class);

        assertNotNull(baseParentDocument);
        assertNull(baseParentDocument.getDeltaAt());
        assertEquals(UK_ESTABLISHMENT_LINK, baseParentDocument.getCompanyProfile().getLinks().getUkEstablishments());
        assertEquals(0L, baseParentDocument.getVersion());

        assertEquals(CHILD_SELF_LINK, childCompanyProfile.getLinks().getSelf());
        assertEquals(PARENT_SELF_LINK, childCompanyProfile.getLinks().getOverseas());
        assertNotNull(childDocument.getUpdated().getAt());
        assertEquals(UK_ESTABLISHMENT_TYPE, childCompanyProfile.getType());
        assertEquals(0L, childDocument.getVersion());
        assertNotEquals(OLD_ETAG, childCompanyProfile.getEtag());
        verify(companyProfileApiService).invokeChsKafkaApi(CONTEXT_ID, CHILD_COMPANY_NUMBER);
    }

    @ParameterizedTest
    @CsvSource({
            STALE_DELTA_AT,
            NEW_DELTA_AT
    })
    void shouldUpdateBaseFCCompanyProfileRegardlessOfDeltaAtTime(String deltaAt) throws Exception {
        // given
        VersionedCompanyProfileDocument document = new VersionedCompanyProfileDocument();
        document.setId(PARENT_COMPANY_NUMBER)
                .setCompanyProfile(new Data()
                        .links(new Links()
                                .ukEstablishments(UK_ESTABLISHMENT_LINK)));
        document.version(0L);
        companyProfileRepository.insert(document);

        CompanyProfile request = makeFCPutRequest(deltaAt);
        // when
        final ResultActions result = mockMvc.perform(put(PUT_ENDPOINT, PARENT_COMPANY_NUMBER)
                .header("ERIC-Identity", "123")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("x-request-id", CONTEXT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final VersionedCompanyProfileDocument parentDocument = mongoTemplate.findById(PARENT_COMPANY_NUMBER, VersionedCompanyProfileDocument.class);

        assertNotNull(parentDocument);
        assertNotNull(parentDocument.getDeltaAt());
        assertEquals(PARENT_SELF_LINK, parentDocument.getCompanyProfile().getLinks().getSelf());
        assertEquals(UK_ESTABLISHMENT_LINK, parentDocument.getCompanyProfile().getLinks().getUkEstablishments());
        assertEquals(OVERSEA_COMPANY_TYPE, parentDocument.getCompanyProfile().getType());
        assertEquals(1L, parentDocument.getVersion());
        verify(companyProfileApiService).invokeChsKafkaApi(CONTEXT_ID, PARENT_COMPANY_NUMBER);
    }

    @Test
    void shouldPersistFCCompanyProfileCorrectlyIfProcessedBeforeBCCompany() throws Exception {
        // given
        CompanyProfile request = makeFCPutRequest(DELTA_AT);

        // when
        final ResultActions result = mockMvc.perform(put(PUT_ENDPOINT, PARENT_COMPANY_NUMBER)
                .header("ERIC-Identity", "123")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("x-request-id", CONTEXT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final VersionedCompanyProfileDocument parentDocument = mongoTemplate.findById(PARENT_COMPANY_NUMBER, VersionedCompanyProfileDocument.class);

        assertNotNull(parentDocument);
        assertNotNull(parentDocument.getDeltaAt());
        assertEquals(PARENT_SELF_LINK, parentDocument.getCompanyProfile().getLinks().getSelf());
        assertNull(parentDocument.getCompanyProfile().getLinks().getUkEstablishments());
        assertEquals(OVERSEA_COMPANY_TYPE, parentDocument.getCompanyProfile().getType());
        assertEquals(0L, parentDocument.getVersion());
        verify(companyProfileApiService).invokeChsKafkaApi(CONTEXT_ID, PARENT_COMPANY_NUMBER);
    }

    @Test
    void shouldAddUKEstablishmentsLinkToFCCompanyIfBCDeltaProcessedSecond() throws Exception {
        // given
        VersionedCompanyProfileDocument document = new VersionedCompanyProfileDocument();
        document.setId(PARENT_COMPANY_NUMBER)
                .setCompanyProfile(makeFCPutRequest(DELTA_AT).getData())
                .setDeltaAt(LocalDateTime.parse(DELTA_AT, DELTA_AT_FORMATTER));
        document.setHasMortgages(false);
        document.version(0L);

        companyProfileRepository.insert(document);
        CompanyProfile request = makeBRPutRequest(STALE_DELTA_AT);

        // when
        final ResultActions result = mockMvc.perform(put(PUT_ENDPOINT, CHILD_COMPANY_NUMBER)
                .header("ERIC-Identity", "123")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("x-request-id", CONTEXT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final VersionedCompanyProfileDocument parentDocument = mongoTemplate.findById(PARENT_COMPANY_NUMBER, VersionedCompanyProfileDocument.class);

        assertNotNull(parentDocument);
        assertNotNull(parentDocument.getDeltaAt());
        assertEquals(PARENT_SELF_LINK, parentDocument.getCompanyProfile().getLinks().getSelf());
        assertEquals(UK_ESTABLISHMENT_LINK, parentDocument.getCompanyProfile().getLinks().getUkEstablishments());
        assertEquals(OVERSEA_COMPANY_TYPE, parentDocument.getCompanyProfile().getType());
        assertEquals(1L, parentDocument.getVersion());
        verify(companyProfileApiService).invokeChsKafkaApi(CONTEXT_ID, PARENT_COMPANY_NUMBER);


        final VersionedCompanyProfileDocument childDocument = Objects.requireNonNull(mongoTemplate.findById(CHILD_COMPANY_NUMBER, VersionedCompanyProfileDocument.class));
        final Data childCompanyProfile = childDocument.getCompanyProfile();

        assertEquals(CHILD_SELF_LINK, childCompanyProfile.getLinks().getSelf());
        assertEquals(PARENT_SELF_LINK, childCompanyProfile.getLinks().getOverseas());
        assertNotNull(childDocument.getUpdated().getAt());
        assertEquals(UK_ESTABLISHMENT_TYPE, childCompanyProfile.getType());
        assertEquals(0L, childDocument.getVersion());
        assertNotEquals(OLD_ETAG, childCompanyProfile.getEtag());
        verify(companyProfileApiService).invokeChsKafkaApi(CONTEXT_ID, CHILD_COMPANY_NUMBER);
    }

    @Test
    void shouldDeleteBaseFCOverseaCompanyIfDeleteDeltaReceived() throws Exception {
        // given
        VersionedCompanyProfileDocument document = new VersionedCompanyProfileDocument();
        document.setId(PARENT_COMPANY_NUMBER)
                .setCompanyProfile(new Data()
                        .links(new Links()
                                .ukEstablishments(UK_ESTABLISHMENT_LINK)));
        document.version(0L);
        companyProfileRepository.insert(document);

        // when
        final ResultActions result = mockMvc.perform(delete(DELETE_ENDPOINT, PARENT_COMPANY_NUMBER)
                .header("ERIC-Identity", "123")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("x-request-id", CONTEXT_ID)
                .header("X-DELTA-AT", DELTA_AT)
                .contentType(MediaType.APPLICATION_JSON));


        // then
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final VersionedCompanyProfileDocument parentDocument = mongoTemplate.findById(PARENT_COMPANY_NUMBER, VersionedCompanyProfileDocument.class);

        assertNull(parentDocument);
        verify(companyProfileApiService).invokeChsKafkaApiWithDeleteEvent(CONTEXT_ID, PARENT_COMPANY_NUMBER, document.getCompanyProfile());
    }

    @Test
    void shouldDeleteFCParentOverseaCompanyIfDeleteDeltaReceived() throws Exception {
        // given
        VersionedCompanyProfileDocument document = new VersionedCompanyProfileDocument();
        document.setId(PARENT_COMPANY_NUMBER)
                .setCompanyProfile(makeFCPutRequest(DELTA_AT).getData())
                .setDeltaAt(LocalDateTime.parse(DELTA_AT, DELTA_AT_FORMATTER));
        document.setHasMortgages(false);
        document.version(0L);

        companyProfileRepository.insert(document);

        // when
        final ResultActions result = mockMvc.perform(delete(DELETE_ENDPOINT, PARENT_COMPANY_NUMBER)
                .header("ERIC-Identity", "123")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("x-request-id", CONTEXT_ID)
                .header("X-DELTA-AT", DELTA_AT)
                .contentType(MediaType.APPLICATION_JSON));


        // then
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final VersionedCompanyProfileDocument parentDocument = mongoTemplate.findById(PARENT_COMPANY_NUMBER, VersionedCompanyProfileDocument.class);

        assertNull(parentDocument);
        verify(companyProfileApiService).invokeChsKafkaApiWithDeleteEvent(CONTEXT_ID, PARENT_COMPANY_NUMBER, document.getCompanyProfile());
    }

    @Test
    void shouldDeleteBRChildUkEstablishmentIfDeleteDeltaReceivedAndParentNotPresent() throws Exception {
        // given
        VersionedCompanyProfileDocument document = new VersionedCompanyProfileDocument();
        document.setId(CHILD_COMPANY_NUMBER)
                .setCompanyProfile(makeBRPutRequest(DELTA_AT).getData())
                .setDeltaAt(LocalDateTime.parse(DELTA_AT, DELTA_AT_FORMATTER));
        document.setHasMortgages(false);
        document.version(0L);

        companyProfileRepository.insert(document);

        // when
        final ResultActions result = mockMvc.perform(delete(DELETE_ENDPOINT, CHILD_COMPANY_NUMBER)
                .header("ERIC-Identity", "123")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("x-request-id", CONTEXT_ID)
                .header("X-DELTA-AT", NEW_DELTA_AT)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final VersionedCompanyProfileDocument childDocument = mongoTemplate.findById(CHILD_COMPANY_NUMBER, VersionedCompanyProfileDocument.class);

        assertNull(childDocument);
        verify(companyProfileApiService).invokeChsKafkaApiWithDeleteEvent(CONTEXT_ID, CHILD_COMPANY_NUMBER, document.getCompanyProfile());
    }

    @Test
    void shouldDeleteBRChildUkEstablishmentAndLinkFromParentIfDeleteDeltaReceivedAndParentStillPresent() throws Exception {
        // given
        VersionedCompanyProfileDocument parentDocument = new VersionedCompanyProfileDocument();
        parentDocument.setId(PARENT_COMPANY_NUMBER)
                .setCompanyProfile(makeFCPutRequest(DELTA_AT).getData())
                .setDeltaAt(LocalDateTime.parse(DELTA_AT, DELTA_AT_FORMATTER));
        parentDocument.setHasMortgages(false);
        parentDocument.version(0L);

        VersionedCompanyProfileDocument document = new VersionedCompanyProfileDocument();
        document.setId(CHILD_COMPANY_NUMBER)
                .setCompanyProfile(makeBRPutRequest(DELTA_AT).getData())
                .setDeltaAt(LocalDateTime.parse(DELTA_AT, DELTA_AT_FORMATTER));
        document.setHasMortgages(false);
        document.version(0L);

        companyProfileRepository.insert(parentDocument);
        companyProfileRepository.insert(document);

        // when
        final ResultActions result = mockMvc.perform(delete(DELETE_ENDPOINT, CHILD_COMPANY_NUMBER)
                .header("ERIC-Identity", "123")
                .header("ERIC-Identity-Type", "key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("x-request-id", CONTEXT_ID)
                .header("X-DELTA-AT", NEW_DELTA_AT)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final VersionedCompanyProfileDocument parentRetrieved = mongoTemplate.findById(PARENT_COMPANY_NUMBER, VersionedCompanyProfileDocument.class);
        final VersionedCompanyProfileDocument childDocument = mongoTemplate.findById(CHILD_COMPANY_NUMBER, VersionedCompanyProfileDocument.class);

        assertNotNull(parentRetrieved);
        assertNotNull(parentRetrieved.getCompanyProfile().getLinks().getSelf());
        assertNull(parentRetrieved.getCompanyProfile().getLinks().getUkEstablishments());
        assertNull(childDocument);
        verify(companyProfileApiService).invokeChsKafkaApiWithDeleteEvent(CONTEXT_ID, CHILD_COMPANY_NUMBER, document.getCompanyProfile());
    }

    private CompanyProfile makeBRPutRequest(String deltaAt) {
        return new CompanyProfile()
                .data(new Data()
                        .branchCompanyDetails(new BranchCompanyDetails()
                                .businessActivity("Custom Software Consulting, Research & Development")
                                .parentCompanyName("PRECISE SOLUTION (CJR TEST1)")
                                .parentCompanyNumber(PARENT_COMPANY_NUMBER))
                        .companyName("PRECISE SOLUTION (CJR TEST1)")
                        .companyNumber("BR005209")
                        .companyStatus("open")
                        .dateOfCreation(LocalDate.parse("1999-10-11"))
                        .hasCharges(null)
                        .links(new Links()
                                .self("/company/BR005209"))
                        .registeredOfficeAddress(new RegisteredOfficeAddress()
                                .addressLine1("Masters Lodge, Ste 3")
                                .locality("London")
                                .postalCode("E1 0BE")
                                .region("Johnson Street"))
                        .type("uk-establishment")
                        .hasSuperSecurePscs(false))
                .hasMortgages(false)
                .parentCompanyNumber(PARENT_COMPANY_NUMBER)
                .deltaAt(deltaAt);
    }

    private CompanyProfile makeFCPutRequest(String deltaAt) {
        return new CompanyProfile()
                .data(new Data()
                        .accounts(makeAccounts())
                        .companyName("PRECISE SOLUTION (CJR TEST1)")
                        .companyNumber("FC022112")
                        .companyStatus("active")
                        .dateOfCreation(LocalDate.parse("1999-10-11"))
                        .externalRegistrationNumber("21343-1995")
                        .foreignCompanyDetails(makeForeignCompanyDetails())
                        .hasInsolvencyHistory(false)
                        .links(new Links()
                                .self(PARENT_SELF_LINK))
                        .previousCompanyNames(makePreviousCompanyNamesList())
                        .registeredOfficeAddress(new RegisteredOfficeAddress()
                                .addressLine1("1905 South Eastern Avenue")
                                .addressLine2("Las Vegas")
                                .country("United States")
                                .locality("Nv 89104  Nevada")
                                .region("Usa"))
                        .registeredOfficeIsInDispute(false)
                        .type("oversea-company")
                        .undeliverableRegisteredOfficeAddress(false)
                        .hasSuperSecurePscs(false)
                )
                .hasMortgages(false)
                .deltaAt(deltaAt);
    }

    private static Accounts makeAccounts() {
        return new Accounts()
                .accountingReferenceDate(new AccountingReferenceDate()
                        .day("31")
                        .month("12"))
                .lastAccounts(new LastAccounts()
                        .madeUpTo(LocalDate.parse("2013-12-31"))
                        .periodEndOn(LocalDate.parse("2013-12-31"))
                        .type("full"))
                .nextAccounts(new NextAccounts()
                        .periodEndOn(LocalDate.parse("2014-12-31")))
                .nextMadeUpTo(LocalDate.parse("2014-12-31"));
    }

    private static ForeignCompanyDetails makeForeignCompanyDetails() {
        return new ForeignCompanyDetails()
                .accountingRequirement(new AccountingRequirement()
                        .foreignAccountType("accounting-requirements-of-originating-country-do-not-apply")
                        .termsOfAccountPublication("accounting-reference-date-allocated-by-companies-house"))
                .businessActivity("Data Processing & Research")
                .governedBy("Chapter 78 Of The State Of Nevada Revised Statutes")
                .isACreditFinancialInstitution(false)
                .originatingRegistry(new OriginatingRegistry()
                        .country("UNITED STATES")
                        .name("State Of Nevada Secretary Of State Office"))
                .registrationNumber("21343-1995")
                .legalForm("Us Limited Liabioity \"C\" Corporation, Private Ownership");
    }

    private List<@Valid PreviousCompanyNames> makePreviousCompanyNamesList() {
        ArrayList<PreviousCompanyNames> companyNames = new ArrayList<>();
        companyNames.add(new PreviousCompanyNames()
                .name("PRECISE SOLUTION")
                .effectiveFrom(LocalDate.parse("1999-11-02"))
                .ceasedOn(LocalDate.parse("2025-01-16")));
        return companyNames;
    }
}
