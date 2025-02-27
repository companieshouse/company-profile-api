package uk.gov.companieshouse.company.profile.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.companieshouse.api.company.Links;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static uk.gov.companieshouse.company.profile.util.LinkRequest.EXEMPTIONS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.EXEMPTIONS_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.OFFICERS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.OFFICERS_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_STATEMENTS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_STATEMENTS_LINK_TYPE;
import org.junit.jupiter.api.function.Executable;
import uk.gov.companieshouse.api.exception.BadRequestException;

@RunWith(SpringRunner.class)
public class LinkRequestFactoryTest {

    private LinkRequestFactory linkRequestFactory;
    private static final String MOCK_CONTEXT_ID = "uninitialised";
    private static final String MOCK_COMPANY_NUMBER = "123456";

    @Before
    public void setUp() {
        linkRequestFactory = new LinkRequestFactory();
    }

    @Test
    public void createLinkRequestForExemptions() {
        LinkRequest expectedLinkRequest = new LinkRequest(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER, EXEMPTIONS_LINK_TYPE,
                EXEMPTIONS_DELTA_TYPE, Links::getExemptions);
        LinkRequest linkRequest = linkRequestFactory
                .createLinkRequest(EXEMPTIONS_LINK_TYPE, MOCK_COMPANY_NUMBER);
        assertThat(expectedLinkRequest).usingRecursiveComparison().isEqualTo(linkRequest);
    }

    @Test
    public void createLinkRequestForOfficers() {
        LinkRequest expectedLinkRequest = new LinkRequest(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER, OFFICERS_LINK_TYPE,
                OFFICERS_DELTA_TYPE, Links::getOfficers);
        LinkRequest linkRequest = linkRequestFactory
                .createLinkRequest(OFFICERS_LINK_TYPE, MOCK_COMPANY_NUMBER);
        assertThat(expectedLinkRequest).usingRecursiveComparison().isEqualTo(linkRequest);
    }

    @Test
    public void createLinkRequestForPscStatements() {
        LinkRequest expectedLinkRequest = new LinkRequest(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER,
                PSC_STATEMENTS_LINK_TYPE, PSC_STATEMENTS_DELTA_TYPE,
                Links::getPersonsWithSignificantControlStatements);
        LinkRequest linkRequest = linkRequestFactory
                .createLinkRequest(PSC_STATEMENTS_LINK_TYPE, MOCK_COMPANY_NUMBER);
        assertThat(expectedLinkRequest).usingRecursiveComparison().isEqualTo(linkRequest);
    }

    @Test
    public void createLinkRequestThrows() {
        Executable executable  = () -> linkRequestFactory
                .createLinkRequest("office", MOCK_COMPANY_NUMBER);
        assertThrows(BadRequestException.class,executable);
    }
}
