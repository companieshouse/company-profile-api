package uk.gov.companieshouse.company.profile.transform;

import static uk.gov.companieshouse.company.profile.util.TestHelper.createExistingCompanyProfile;
import static uk.gov.companieshouse.company.profile.util.TestHelper.createExistingLinks;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Links;
import uk.gov.companieshouse.api.company.RegisteredOfficeAddress;
import uk.gov.companieshouse.company.profile.model.VersionedCompanyProfileDocument;
import uk.gov.companieshouse.company.profile.util.TestHelper;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.StructuredLogger;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

class CompanyProfileTransformerTest {

    Logger logger = new StructuredLogger("CompanyProfileTransformerTest");

    private TestHelper testHelper;
    private CompanyProfileTransformer transformer;
    private CompanyProfile COMPANY_PROFILE;
    private CompanyProfile COMPANY_PROFILE_WITHOUT_LINKS;
    private Links EXISTING_LINKS;
    private final String NEW_CHARGES_LINK = "/company/00019993/charges";
    private final String EXISTING_CHARGES_LINK = "/company/00010001/charges";
    private final String EXISTING_INSOLVENCY_LINK = "/company/00010001/insolvency";

    @BeforeEach
    public void setUp() throws IOException {
        transformer = new CompanyProfileTransformer(logger);
        TestHelper testHelper = new TestHelper();
        COMPANY_PROFILE = testHelper.createCompanyProfileObject();
        EXISTING_LINKS = createExistingLinks();
        COMPANY_PROFILE_WITHOUT_LINKS = testHelper.createCompanyProfileWithoutLinks();
    }

    @Test
    void shouldTransformCompanyProfileWithLinksWhenThereAreNoExistingLinks(){
        EXISTING_LINKS = null;

        VersionedCompanyProfileDocument document = transformer.transform(new VersionedCompanyProfileDocument(), COMPANY_PROFILE, EXISTING_LINKS);

        Assertions.assertEquals(COMPANY_PROFILE.getData().getCompanyNumber(), document.getCompanyProfile().getCompanyNumber());
        Assertions.assertEquals(COMPANY_PROFILE.getData(), document.getCompanyProfile());
        Assertions.assertEquals(COMPANY_PROFILE.getDeltaAt(), document.getDeltaAt().format(DateTimeFormatter
                .ofPattern("yyyyMMddHHmmssSSSSSS")));
        Assertions.assertEquals(COMPANY_PROFILE.getHasMortgages(), document.isHasMortgages());
        Assertions.assertNotNull(document.getCompanyProfile().getLinks());
        Assertions.assertEquals(COMPANY_PROFILE.getData().getLinks(), document.getCompanyProfile().getLinks());
        Assertions.assertEquals(NEW_CHARGES_LINK, document.getCompanyProfile().getLinks().getCharges());
        Assertions.assertNull(document.getCompanyProfile().getLinks().getInsolvency());

        Assertions.assertTrue(LocalDateTime.now().toEpochSecond(ZoneOffset.MIN)
                - document.getUpdated().getAt().toEpochSecond(ZoneOffset.MIN) < 2);
        Assertions.assertEquals(COMPANY_PROFILE.getData().getDateOfCreation(), document.getCompanyProfile().getDateOfCreation());
    }

    @Test
    void shouldTransformCompanyProfileWithLinksWhenThereAreExistingLinks(){
        VersionedCompanyProfileDocument document = transformer.transform(createExistingCompanyProfile(), COMPANY_PROFILE, EXISTING_LINKS);

        Assertions.assertEquals(COMPANY_PROFILE.getData().getCompanyNumber(), document.getCompanyProfile().getCompanyNumber());
        Assertions.assertEquals(COMPANY_PROFILE.getData(), document.getCompanyProfile());
        Assertions.assertEquals(COMPANY_PROFILE.getDeltaAt(), document.getDeltaAt().format(DateTimeFormatter
                .ofPattern("yyyyMMddHHmmssSSSSSS")));
        Assertions.assertEquals(COMPANY_PROFILE.getHasMortgages(), document.isHasMortgages());
        Assertions.assertNotNull(document.getCompanyProfile().getLinks());
        Assertions.assertEquals(COMPANY_PROFILE.getData().getLinks(), document.getCompanyProfile().getLinks());
        Assertions.assertEquals(NEW_CHARGES_LINK, document.getCompanyProfile().getLinks().getCharges());
        Assertions.assertEquals(EXISTING_INSOLVENCY_LINK, document.getCompanyProfile().getLinks().getInsolvency());

        Assertions.assertTrue(LocalDateTime.now().toEpochSecond(ZoneOffset.MIN)
                - document.getUpdated().getAt().toEpochSecond(ZoneOffset.MIN) < 2);
        Assertions.assertEquals(COMPANY_PROFILE.getData().getDateOfCreation(), document.getCompanyProfile().getDateOfCreation());
    }

    @Test
    void shouldTransformCompanyProfileWithNoLinksWhenThereAreExistingLinks(){
        VersionedCompanyProfileDocument document = transformer.transform(createExistingCompanyProfile(), COMPANY_PROFILE_WITHOUT_LINKS, EXISTING_LINKS);

        Assertions.assertEquals(COMPANY_PROFILE_WITHOUT_LINKS.getData().getCompanyNumber(), document.getCompanyProfile().getCompanyNumber());
        Assertions.assertEquals(COMPANY_PROFILE_WITHOUT_LINKS.getData(), document.getCompanyProfile());
        Assertions.assertEquals(COMPANY_PROFILE_WITHOUT_LINKS.getDeltaAt(), document.getDeltaAt().format(DateTimeFormatter
                .ofPattern("yyyyMMddHHmmssSSSSSS")));
        Assertions.assertEquals(COMPANY_PROFILE_WITHOUT_LINKS.getHasMortgages(), document.isHasMortgages());
        Assertions.assertNotNull(document.getCompanyProfile().getLinks());
        Assertions.assertEquals(EXISTING_LINKS, document.getCompanyProfile().getLinks());
        Assertions.assertEquals(EXISTING_CHARGES_LINK, document.getCompanyProfile().getLinks().getCharges());
        Assertions.assertEquals(EXISTING_INSOLVENCY_LINK, document.getCompanyProfile().getLinks().getInsolvency());

        Assertions.assertTrue(LocalDateTime.now().toEpochSecond(ZoneOffset.MIN)
                - document.getUpdated().getAt().toEpochSecond(ZoneOffset.MIN) < 2);
    }

    @Test
    void shouldTransformCompanyProfileWithNoLinksWhenThereAreNoExistingLinks(){
        EXISTING_LINKS = null;

        VersionedCompanyProfileDocument document = transformer.transform(new VersionedCompanyProfileDocument(), COMPANY_PROFILE_WITHOUT_LINKS, EXISTING_LINKS);

        Assertions.assertEquals(COMPANY_PROFILE_WITHOUT_LINKS.getData().getCompanyNumber(), document.getCompanyProfile().getCompanyNumber());
        Assertions.assertEquals(COMPANY_PROFILE_WITHOUT_LINKS.getData(), document.getCompanyProfile());
        Assertions.assertEquals(COMPANY_PROFILE_WITHOUT_LINKS.getDeltaAt(), document.getDeltaAt().format(DateTimeFormatter
                .ofPattern("yyyyMMddHHmmssSSSSSS")));
        Assertions.assertEquals(COMPANY_PROFILE_WITHOUT_LINKS.getHasMortgages(), document.isHasMortgages());
        Assertions.assertEquals(COMPANY_PROFILE_WITHOUT_LINKS.getParentCompanyNumber(), document.getParentCompanyNumber());
        Assertions.assertNull(document.getCompanyProfile().getLinks());

        Assertions.assertTrue(LocalDateTime.now().toEpochSecond(ZoneOffset.MIN)
                - document.getUpdated().getAt().toEpochSecond(ZoneOffset.MIN) < 2);
    }

    @Test
    void shouldTransformCompanyProfileWithCareOfName(){
        RegisteredOfficeAddress roa = new RegisteredOfficeAddress();
        roa.setCareOfName("careOfName");
        roa.setCareOf("careOf");
        COMPANY_PROFILE_WITHOUT_LINKS.getData().setRegisteredOfficeAddress(roa);

        EXISTING_LINKS = null;
        VersionedCompanyProfileDocument document = transformer.transform(createExistingCompanyProfile(), COMPANY_PROFILE_WITHOUT_LINKS, EXISTING_LINKS);

        Assertions.assertEquals("careOfName", document.getCompanyProfile().getRegisteredOfficeAddress().getCareOfName());
        Assertions.assertNull(document.getCompanyProfile().getRegisteredOfficeAddress().getCareOf());

        Assertions.assertTrue(LocalDateTime.now().toEpochSecond(ZoneOffset.MIN)
                - document.getUpdated().getAt().toEpochSecond(ZoneOffset.MIN) < 2);
    }

    @Test
    void shouldTransformCompanyProfileWithOnlyCareOf(){
        RegisteredOfficeAddress roa = new RegisteredOfficeAddress();
        roa.setCareOf("careOf");
        COMPANY_PROFILE_WITHOUT_LINKS.getData().setRegisteredOfficeAddress(roa);

        EXISTING_LINKS = null;
        VersionedCompanyProfileDocument document = transformer.transform(createExistingCompanyProfile(), COMPANY_PROFILE_WITHOUT_LINKS, EXISTING_LINKS);

        Assertions.assertEquals("careOf", document.getCompanyProfile().getRegisteredOfficeAddress().getCareOfName());
        Assertions.assertNull(document.getCompanyProfile().getRegisteredOfficeAddress().getCareOf());

        Assertions.assertTrue(LocalDateTime.now().toEpochSecond(ZoneOffset.MIN)
                - document.getUpdated().getAt().toEpochSecond(ZoneOffset.MIN) < 2);
    }
}
