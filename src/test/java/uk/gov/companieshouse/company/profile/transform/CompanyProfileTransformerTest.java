package uk.gov.companieshouse.company.profile.transform;

import static uk.gov.companieshouse.company.profile.util.TestHelper.createExistingCompanyProfile;
import static uk.gov.companieshouse.company.profile.util.TestHelper.createExistingLinks;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Links;
import uk.gov.companieshouse.api.company.RegisteredOfficeAddress;
import uk.gov.companieshouse.api.company.SensitiveData;
import uk.gov.companieshouse.company.profile.model.VersionedCompanyProfileDocument;
import uk.gov.companieshouse.company.profile.util.TestHelper;

class CompanyProfileTransformerTest {

    private CompanyProfileTransformer transformer;
    private CompanyProfile companyProfile;
    private CompanyProfile companyProfileWithoutLinks;
    private Links existingLinks;
    private final String newChargesLink = "/company/00019993/charges";
    private final String existingChargesLink = "/company/00010001/charges";
    private final String existingInsolvencyLink = "/company/00010001/insolvency";

    @BeforeEach
    void setUp() throws IOException {
        transformer = new CompanyProfileTransformer();
        TestHelper testHelper = new TestHelper();
        companyProfile = testHelper.createCompanyProfileObject();
        existingLinks = createExistingLinks();
        companyProfileWithoutLinks = testHelper.createCompanyProfileWithoutLinks();
    }

    @Test
    void shouldTransformCompanyProfileWithLinksWhenThereAreNoExistingLinks() {
        existingLinks = null;

        VersionedCompanyProfileDocument document = transformer.transform(new VersionedCompanyProfileDocument(), companyProfile, existingLinks);

        Assertions.assertEquals(companyProfile.getData().getCompanyNumber(), document.getCompanyProfile().getCompanyNumber());
        Assertions.assertEquals(companyProfile.getData(), document.getCompanyProfile());
        Assertions.assertEquals(companyProfile.getDeltaAt(), document.getDeltaAt().format(DateTimeFormatter
                .ofPattern("yyyyMMddHHmmssSSSSSS")));
        Assertions.assertEquals(companyProfile.getHasMortgages(), document.isHasMortgages());
        Assertions.assertNotNull(document.getCompanyProfile().getLinks());
        Assertions.assertEquals(companyProfile.getData().getLinks(), document.getCompanyProfile().getLinks());
        Assertions.assertEquals(newChargesLink, document.getCompanyProfile().getLinks().getCharges());
        Assertions.assertNull(document.getCompanyProfile().getLinks().getInsolvency());

        // Check that sensitive data is not included in the resulting document
        Assertions.assertNull(document.getSensitiveData());

        Assertions.assertTrue(LocalDateTime.now().toEpochSecond(ZoneOffset.MIN)
                - document.getUpdated().getAt().toEpochSecond(ZoneOffset.MIN) < 2);
        Assertions.assertEquals(companyProfile.getData().getDateOfCreation(), document.getCompanyProfile().getDateOfCreation());
    }

    @Test
    void shouldTransformCompanyProfileWithLinksWhenThereAreExistingLinks() {
        VersionedCompanyProfileDocument document = transformer.transform(createExistingCompanyProfile(), companyProfile, existingLinks);

        Assertions.assertEquals(companyProfile.getData().getCompanyNumber(), document.getCompanyProfile().getCompanyNumber());
        Assertions.assertEquals(companyProfile.getData(), document.getCompanyProfile());
        Assertions.assertEquals(companyProfile.getDeltaAt(), document.getDeltaAt().format(DateTimeFormatter
                .ofPattern("yyyyMMddHHmmssSSSSSS")));
        Assertions.assertEquals(companyProfile.getHasMortgages(), document.isHasMortgages());
        Assertions.assertNotNull(document.getCompanyProfile().getLinks());
        Assertions.assertEquals(companyProfile.getData().getLinks(), document.getCompanyProfile().getLinks());
        Assertions.assertEquals(newChargesLink, document.getCompanyProfile().getLinks().getCharges());
        Assertions.assertEquals(existingInsolvencyLink, document.getCompanyProfile().getLinks().getInsolvency());

        Assertions.assertTrue(LocalDateTime.now().toEpochSecond(ZoneOffset.MIN)
                - document.getUpdated().getAt().toEpochSecond(ZoneOffset.MIN) < 2);
        Assertions.assertEquals(companyProfile.getData().getDateOfCreation(), document.getCompanyProfile().getDateOfCreation());
    }

    @Test
    void shouldTransformCompanyProfileWithNoLinksWhenThereAreExistingLinks() {
        VersionedCompanyProfileDocument document = transformer.transform(createExistingCompanyProfile(), companyProfileWithoutLinks, existingLinks);

        Assertions.assertEquals(companyProfileWithoutLinks.getData().getCompanyNumber(), document.getCompanyProfile().getCompanyNumber());
        Assertions.assertEquals(companyProfileWithoutLinks.getData(), document.getCompanyProfile());
        Assertions.assertEquals(companyProfileWithoutLinks.getDeltaAt(), document.getDeltaAt().format(DateTimeFormatter
                .ofPattern("yyyyMMddHHmmssSSSSSS")));
        Assertions.assertEquals(companyProfileWithoutLinks.getHasMortgages(), document.isHasMortgages());
        Assertions.assertNotNull(document.getCompanyProfile().getLinks());
        Assertions.assertEquals(existingLinks, document.getCompanyProfile().getLinks());
        Assertions.assertEquals(existingChargesLink, document.getCompanyProfile().getLinks().getCharges());
        Assertions.assertEquals(existingInsolvencyLink, document.getCompanyProfile().getLinks().getInsolvency());

        Assertions.assertTrue(LocalDateTime.now().toEpochSecond(ZoneOffset.MIN)
                - document.getUpdated().getAt().toEpochSecond(ZoneOffset.MIN) < 2);
    }

    @Test
    void shouldTransformCompanyProfileWithNoLinksWhenThereAreNoExistingLinks() {
        existingLinks = null;

        VersionedCompanyProfileDocument document = transformer.transform(new VersionedCompanyProfileDocument(), companyProfileWithoutLinks, existingLinks);

        Assertions.assertEquals(companyProfileWithoutLinks.getData().getCompanyNumber(), document.getCompanyProfile().getCompanyNumber());
        Assertions.assertEquals(companyProfileWithoutLinks.getData(), document.getCompanyProfile());
        Assertions.assertEquals(companyProfileWithoutLinks.getDeltaAt(), document.getDeltaAt().format(DateTimeFormatter
                .ofPattern("yyyyMMddHHmmssSSSSSS")));
        Assertions.assertEquals(companyProfileWithoutLinks.getHasMortgages(), document.isHasMortgages());
        Assertions.assertEquals(companyProfileWithoutLinks.getParentCompanyNumber(), document.getParentCompanyNumber());
        Assertions.assertNull(document.getCompanyProfile().getLinks());

        Assertions.assertTrue(LocalDateTime.now().toEpochSecond(ZoneOffset.MIN)
                - document.getUpdated().getAt().toEpochSecond(ZoneOffset.MIN) < 2);
    }

    @Test
    void shouldTransformCompanyProfileWithCareOfName() {
        RegisteredOfficeAddress roa = new RegisteredOfficeAddress();
        roa.setCareOfName("careOfName");
        roa.setCareOf("careOf");
        companyProfileWithoutLinks.getData().setRegisteredOfficeAddress(roa);

        existingLinks = null;
        VersionedCompanyProfileDocument document = transformer.transform(createExistingCompanyProfile(), companyProfileWithoutLinks, existingLinks);

        Assertions.assertEquals("careOfName", document.getCompanyProfile().getRegisteredOfficeAddress().getCareOfName());
        Assertions.assertNull(document.getCompanyProfile().getRegisteredOfficeAddress().getCareOf());

        Assertions.assertTrue(LocalDateTime.now().toEpochSecond(ZoneOffset.MIN)
                - document.getUpdated().getAt().toEpochSecond(ZoneOffset.MIN) < 2);
    }

    @Test
    void shouldTransformCompanyProfileWithOnlyCareOf() {
        RegisteredOfficeAddress roa = new RegisteredOfficeAddress();
        roa.setCareOf("careOf");
        companyProfileWithoutLinks.getData().setRegisteredOfficeAddress(roa);

        existingLinks = null;
        VersionedCompanyProfileDocument document = transformer.transform(createExistingCompanyProfile(), companyProfileWithoutLinks, existingLinks);

        Assertions.assertEquals("careOf", document.getCompanyProfile().getRegisteredOfficeAddress().getCareOfName());
        Assertions.assertNull(document.getCompanyProfile().getRegisteredOfficeAddress().getCareOf());

        Assertions.assertTrue(LocalDateTime.now().toEpochSecond(ZoneOffset.MIN)
                - document.getUpdated().getAt().toEpochSecond(ZoneOffset.MIN) < 2);
    }

    @Test
    void shouldTransformCompanyDeltaWithRegisteredEmailAddressToSensitiveData() {
        companyProfile.setSensitiveData(new SensitiveData());
        companyProfile.getSensitiveData().setRegisteredEmailAddress("john@example.com");

        VersionedCompanyProfileDocument document = transformer.transform(
                new VersionedCompanyProfileDocument(), companyProfile, existingLinks);

        Assertions.assertNotNull(document.getSensitiveData());
        Assertions.assertEquals("john@example.com",
                document.getSensitiveData().getRegisteredEmailAddress());
    }
}
