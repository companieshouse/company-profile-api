package uk.gov.companieshouse.company.profile.transform;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Links;
import uk.gov.companieshouse.api.model.CompanyProfileDocument;
import uk.gov.companieshouse.company.profile.util.TestHelper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class CompanyProfileTransformerTest {

    private CompanyProfileTransformer transformer;

    private CompanyProfile COMPANY_PROFILE;

    private CompanyProfile COMPANY_PROFILE_WITHOUT_LINKS;

    private Links EXISTING_LINKS;

    @BeforeEach
    public void setUp() throws IOException {
        transformer = new CompanyProfileTransformer();
        TestHelper testHelper = new TestHelper();
        COMPANY_PROFILE = testHelper.createCompanyProfileObject();
        EXISTING_LINKS = testHelper.createExistingLinks();
        COMPANY_PROFILE_WITHOUT_LINKS = testHelper.createCompanyProfileWithoutLinks();
    }

    @Test
    void shouldTransformCompanyProfileWithLinksWhenThereAreNoExistingLinks(){
        EXISTING_LINKS = null;

        CompanyProfileDocument document = transformer.transform(COMPANY_PROFILE, COMPANY_PROFILE.getData().getCompanyNumber(), EXISTING_LINKS);

        Assertions.assertEquals(COMPANY_PROFILE.getData().getCompanyNumber(), document.getCompanyProfile().getCompanyNumber());
        Assertions.assertEquals(COMPANY_PROFILE.getData(), document.getCompanyProfile());
        Assertions.assertEquals(COMPANY_PROFILE.getDeltaAt(), document.getDeltaAt().format(DateTimeFormatter
                .ofPattern("yyyyMMddHHmmssSSSSSS")));
        Assertions.assertEquals(COMPANY_PROFILE.getHasMortgages(), document.isHasMortgages());
        Assertions.assertNotNull(document.getCompanyProfile().getLinks());
        Assertions.assertEquals(COMPANY_PROFILE.getData().getLinks(), document.getCompanyProfile().getLinks());
        Assertions.assertTrue(LocalDateTime.now().toEpochSecond(ZoneOffset.MIN)
                - document.getUpdated().getAt().toEpochSecond(ZoneOffset.MIN) < 2);
    }

    @Test
    void shouldTransformCompanyProfileWithLinksWhenThereAreExistingLinks(){
        CompanyProfileDocument document = transformer.transform(COMPANY_PROFILE, COMPANY_PROFILE.getData().getCompanyNumber(), EXISTING_LINKS);

        Assertions.assertEquals(COMPANY_PROFILE.getData().getCompanyNumber(), document.getCompanyProfile().getCompanyNumber());
        Assertions.assertEquals(COMPANY_PROFILE.getData(), document.getCompanyProfile());
        Assertions.assertEquals(COMPANY_PROFILE.getDeltaAt(), document.getDeltaAt().format(DateTimeFormatter
                .ofPattern("yyyyMMddHHmmssSSSSSS")));
        Assertions.assertEquals(COMPANY_PROFILE.getHasMortgages(), document.isHasMortgages());
        Assertions.assertNotNull(document.getCompanyProfile().getLinks());
        Assertions.assertEquals(COMPANY_PROFILE.getData().getLinks(), document.getCompanyProfile().getLinks());
        Assertions.assertTrue(LocalDateTime.now().toEpochSecond(ZoneOffset.MIN)
                - document.getUpdated().getAt().toEpochSecond(ZoneOffset.MIN) < 2);
    }

    @Test
    void shouldTransformCompanyProfileWithNoLinksWhenThereAreExistingLinks(){
        CompanyProfileDocument document = transformer.transform(COMPANY_PROFILE_WITHOUT_LINKS, COMPANY_PROFILE_WITHOUT_LINKS.getData().getCompanyNumber(), EXISTING_LINKS);

        Assertions.assertEquals(COMPANY_PROFILE_WITHOUT_LINKS.getData().getCompanyNumber(), document.getCompanyProfile().getCompanyNumber());
        Assertions.assertEquals(COMPANY_PROFILE_WITHOUT_LINKS.getData(), document.getCompanyProfile());
        Assertions.assertEquals(COMPANY_PROFILE_WITHOUT_LINKS.getDeltaAt(), document.getDeltaAt().format(DateTimeFormatter
                .ofPattern("yyyyMMddHHmmssSSSSSS")));
        Assertions.assertEquals(COMPANY_PROFILE_WITHOUT_LINKS.getHasMortgages(), document.isHasMortgages());
        Assertions.assertNotNull(document.getCompanyProfile().getLinks());
        Assertions.assertEquals(EXISTING_LINKS, document.getCompanyProfile().getLinks());
        Assertions.assertTrue(LocalDateTime.now().toEpochSecond(ZoneOffset.MIN)
                - document.getUpdated().getAt().toEpochSecond(ZoneOffset.MIN) < 2);
    }

    @Test
    void shouldTransformCompanyProfileWithNoLinksWhenThereAreNoExistingLinks(){
        EXISTING_LINKS = null;

        CompanyProfileDocument document = transformer.transform(COMPANY_PROFILE_WITHOUT_LINKS, COMPANY_PROFILE_WITHOUT_LINKS.getData().getCompanyNumber(), EXISTING_LINKS);

        Assertions.assertEquals(COMPANY_PROFILE_WITHOUT_LINKS.getData().getCompanyNumber(), document.getCompanyProfile().getCompanyNumber());
        Assertions.assertEquals(COMPANY_PROFILE_WITHOUT_LINKS.getData(), document.getCompanyProfile());
        Assertions.assertEquals(COMPANY_PROFILE_WITHOUT_LINKS.getDeltaAt(), document.getDeltaAt().format(DateTimeFormatter
                .ofPattern("yyyyMMddHHmmssSSSSSS")));
        Assertions.assertEquals(COMPANY_PROFILE_WITHOUT_LINKS.getHasMortgages(), document.isHasMortgages());
        Assertions.assertNull(document.getCompanyProfile().getLinks());
        Assertions.assertTrue(LocalDateTime.now().toEpochSecond(ZoneOffset.MIN)
                - document.getUpdated().getAt().toEpochSecond(ZoneOffset.MIN) < 2);
    }

}
