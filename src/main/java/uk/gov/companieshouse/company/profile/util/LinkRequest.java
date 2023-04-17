package uk.gov.companieshouse.company.profile.util;

import java.util.function.Function;
import uk.gov.companieshouse.api.company.Links;

public class LinkRequest {

    public static final String EXEMPTIONS_LINK_TYPE = "exemptions";
    public static final String EXEMPTIONS_DELTA_TYPE = "exemption_delta";
    public static final Function<Links, String> EXEMPTIONS_GET = link -> link.getExemptions();
    public static final String OFFICERS_LINK_TYPE = "officers";
    public static final String OFFICERS_DELTA_TYPE = "officer_delta";
    public static final Function<Links, String> OFFICERS_GET = link -> link.getOfficers();
    public static final String PSC_STATEMENTS_LINK_TYPE =
            "persons-with-significant-control-statements";
    public static final String PSC_STATEMENTS_DELTA_TYPE = "psc_statement_delta";
    public static final Function<Links, String> PSC_STATEMENTS_GET = link ->
            link.getPersonsWithSignificantControlStatements();

    private final String contextId;
    private final String companyNumber;
    private final String linkType;
    private final String deltaType;
    private final Function<Links, String> linksGet;

    /**
     * Holds LinkRequest data.
     *
     * @param contextId     The x-request-id from the request header
     * @param companyNumber The company number and ID of the company
     * @param linkType      The type of link
     * @param deltaType     The type of delta
     * @param linksGet      The get method for link
     */
    public LinkRequest(String contextId, String companyNumber, String linkType,
                       String deltaType, Function<Links, String> linksGet) {
        this.contextId = contextId;
        this.companyNumber = companyNumber;
        this.linkType = linkType;
        this.deltaType = deltaType;
        this.linksGet = linksGet;
    }

    public String getContextId() {
        return contextId;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public String getLinkType() {
        return linkType;
    }

    public String getDeltaType() {
        return deltaType;
    }

    public Function<Links, String> getCheckLink() {
        return linksGet;
    }
}
