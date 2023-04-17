package uk.gov.companieshouse.company.profile.util;

import static uk.gov.companieshouse.company.profile.util.LinkRequest.EXEMPTIONS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.EXEMPTIONS_GET;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.OFFICERS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.OFFICERS_GET;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_STATEMENTS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.PSC_STATEMENTS_GET;

import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.company.Links;

@Component
public class LinkRequestFactory {

    Map<String, Map<String, Function<Links, String>>> linkRequestMap = Map.ofEntries(
            Map.entry(LinkRequest.EXEMPTIONS_LINK_TYPE,
                    Map.ofEntries(Map.entry(EXEMPTIONS_DELTA_TYPE, EXEMPTIONS_GET))),
            Map.entry(LinkRequest.OFFICERS_LINK_TYPE,
                    Map.ofEntries(Map.entry(OFFICERS_DELTA_TYPE, OFFICERS_GET))),
            Map.entry(LinkRequest.PSC_STATEMENTS_LINK_TYPE,
                    Map.ofEntries(Map.entry(PSC_STATEMENTS_DELTA_TYPE, PSC_STATEMENTS_GET))));

    /**
     * Creates linkRequest object.
     */
    public LinkRequest createLinkRequest(String linkType, String contextId, String companyNumber)
            throws NoSuchFieldException {
        Map<String, Function<Links, String>> linkRequestData;
        try {
            linkRequestData = linkRequestMap.get(linkType);
        } catch (Exception ex) {
            throw new NoSuchFieldException("no mapping for linkType: " + linkType);
        }

        return new LinkRequest(
                    contextId, companyNumber, linkType,
                linkRequestData.keySet().stream().findFirst().orElseThrow(() ->
                        new NoSuchFieldException("no delta type key found for linkType in map: "
                                + linkType)),
                linkRequestData.values().stream().findFirst().orElseThrow(() ->
                        new NoSuchFieldException("no links GET value found for linkType in map: "
                                + linkType)));
    }
}
