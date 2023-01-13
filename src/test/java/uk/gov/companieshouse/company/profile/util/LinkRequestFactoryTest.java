package uk.gov.companieshouse.company.profile.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.EXEMPTIONS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.EXEMPTIONS_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.OFFICERS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.OFFICERS_LINK_TYPE;

class LinkRequestFactoryTest {

    private static final String CONTEXT_ID = "123456";
    private static final String COMPANY_NUMBER = "00006400";
    private final LinkRequestFactory linkRequestFactory = new LinkRequestFactory();

    @Test
    void shouldCreateExemptionsLinkRequest() {
        LinkRequest exemptionsLinkRequest = linkRequestFactory.createExemptionsLinkRequest(CONTEXT_ID, COMPANY_NUMBER);
        assertEquals(CONTEXT_ID, exemptionsLinkRequest.getContextId());
        assertEquals(COMPANY_NUMBER, exemptionsLinkRequest.getCompanyNumber());
        assertEquals(EXEMPTIONS_LINK_TYPE, exemptionsLinkRequest.getLinkType());
        assertEquals(EXEMPTIONS_DELTA_TYPE, exemptionsLinkRequest.getDeltaType());
    }

    @Test
    void createOfficersLinkRequest() {
        LinkRequest officersLinkRequest = linkRequestFactory.createOfficersLinkRequest(CONTEXT_ID, COMPANY_NUMBER);
        assertEquals(CONTEXT_ID, officersLinkRequest.getContextId());
        assertEquals(COMPANY_NUMBER, officersLinkRequest.getCompanyNumber());
        assertEquals(OFFICERS_LINK_TYPE, officersLinkRequest.getLinkType());
        assertEquals(OFFICERS_DELTA_TYPE, officersLinkRequest.getDeltaType());
    }
}
