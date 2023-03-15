package uk.gov.companieshouse.company.profile.util;

import static uk.gov.companieshouse.company.profile.util.LinkRequest.EXEMPTIONS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.EXEMPTIONS_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.OFFICERS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.OFFICERS_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSCSTATEMENTS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSCSTATEMENTS_LINK_TYPE;

import org.springframework.stereotype.Component;




@Component
public class LinkRequestFactory {
    public LinkRequest createExemptionsLinkRequest(String contextId, String companyNumber) {
        return new LinkRequest(
                contextId, companyNumber, EXEMPTIONS_LINK_TYPE, EXEMPTIONS_DELTA_TYPE);
    }

    public LinkRequest createOfficersLinkRequest(String contextId, String companyNumber) {
        return new LinkRequest(
                contextId, companyNumber, OFFICERS_LINK_TYPE, OFFICERS_DELTA_TYPE);
    }

    public LinkRequest createPscStatementsLinkRequest(String contextId, String companyNumber) {
        return new LinkRequest(
                contextId, companyNumber, PSCSTATEMENTS_LINK_TYPE, PSCSTATEMENTS_DELTA_TYPE);
    }
}