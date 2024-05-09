package uk.gov.companieshouse.company.profile.util;

import static uk.gov.companieshouse.company.profile.util.LinkRequest.EXEMPTIONS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.EXEMPTIONS_GET;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.FILING_HISTORY_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.FILING_HISTORY_GET;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.OFFICERS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.OFFICERS_GET;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_GET;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_STATEMENTS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_STATEMENTS_GET;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.UK_ESTABLISHMENTS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.UK_ESTABLISHMENTS_GET;

import java.util.Map;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.exception.BadRequestException;

@Component
public class LinkRequestFactory {
    Map<String, LinkTypeData> linkRequestMap = Map.of(
            LinkRequest.EXEMPTIONS_LINK_TYPE,
            new LinkTypeData(EXEMPTIONS_DELTA_TYPE, EXEMPTIONS_GET),
            LinkRequest.OFFICERS_LINK_TYPE,
            new LinkTypeData(OFFICERS_DELTA_TYPE, OFFICERS_GET),
            LinkRequest.PSC_LINK_TYPE,
            new LinkTypeData(PSC_DELTA_TYPE, PSC_GET),
            LinkRequest.PSC_STATEMENTS_LINK_TYPE,
            new LinkTypeData(PSC_STATEMENTS_DELTA_TYPE, PSC_STATEMENTS_GET),
            LinkRequest.FILING_HISTORY_LINK_TYPE,
            new LinkTypeData(FILING_HISTORY_DELTA_TYPE, FILING_HISTORY_GET),
            LinkRequest.UK_ESTABLISHMENTS_LINK_TYPE,
            new LinkTypeData(UK_ESTABLISHMENTS_DELTA_TYPE, UK_ESTABLISHMENTS_GET));

    /**
     * Creates linkRequest object.
     */
    public LinkRequest createLinkRequest(String linkType, String contextId, String companyNumber) {
        if (!linkRequestMap.containsKey(linkType)) {
            throw new BadRequestException("invalid link type");
        }
        LinkTypeData linkTypeData = linkRequestMap.get(linkType);
        return new LinkRequest(contextId, companyNumber, linkType,
                linkTypeData.getDeltaType(), linkTypeData.getLinkGetter());
    }
}
