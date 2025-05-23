package uk.gov.companieshouse.company.profile.util;

import static uk.gov.companieshouse.company.profile.util.LinkRequest.CHARGES_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.CHARGES_GET;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.CHARGES_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.EXEMPTIONS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.EXEMPTIONS_GET;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.EXEMPTIONS_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.FILING_HISTORY_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.FILING_HISTORY_GET;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.FILING_HISTORY_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.INSOLVENCY_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.INSOLVENCY_GET;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.INSOLVENCY_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.OFFICERS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.OFFICERS_GET;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.OFFICERS_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_GET;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_STATEMENTS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_STATEMENTS_GET;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_STATEMENTS_LINK_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.UK_ESTABLISHMENTS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.UK_ESTABLISHMENTS_GET;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.UK_ESTABLISHMENTS_LINK_TYPE;

import java.util.Map;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.exception.BadRequestException;
import uk.gov.companieshouse.company.profile.logging.DataMapHolder;

@Component
public class LinkRequestFactory {

    final Map<String, LinkTypeData> linkRequestMap = Map.of(
            CHARGES_LINK_TYPE,
            new LinkTypeData(CHARGES_DELTA_TYPE, CHARGES_GET),
            EXEMPTIONS_LINK_TYPE,
            new LinkTypeData(EXEMPTIONS_DELTA_TYPE, EXEMPTIONS_GET),
            FILING_HISTORY_LINK_TYPE,
            new LinkTypeData(FILING_HISTORY_DELTA_TYPE, FILING_HISTORY_GET),
            INSOLVENCY_LINK_TYPE,
            new LinkTypeData(INSOLVENCY_DELTA_TYPE, INSOLVENCY_GET),
            OFFICERS_LINK_TYPE,
            new LinkTypeData(OFFICERS_DELTA_TYPE, OFFICERS_GET),
            PSC_LINK_TYPE,
            new LinkTypeData(PSC_DELTA_TYPE, PSC_GET),
            PSC_STATEMENTS_LINK_TYPE,
            new LinkTypeData(PSC_STATEMENTS_DELTA_TYPE, PSC_STATEMENTS_GET),
            UK_ESTABLISHMENTS_LINK_TYPE,
            new LinkTypeData(UK_ESTABLISHMENTS_DELTA_TYPE, UK_ESTABLISHMENTS_GET));

    /**
     * Creates linkRequest object.
     */
    public LinkRequest createLinkRequest(String linkType, String companyNumber) {
        if (!linkRequestMap.containsKey(linkType)) {
            throw new BadRequestException("invalid link type");
        }
        LinkTypeData linkTypeData = linkRequestMap.get(linkType);
        return new LinkRequest(DataMapHolder.getRequestId(), companyNumber, linkType,
                linkTypeData.getDeltaType(), linkTypeData.getLinkGetter());
    }
}
