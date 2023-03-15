package uk.gov.companieshouse.company.profile.util;

public class LinkRequest {

    public static final String EXEMPTIONS_LINK_TYPE = "exemptions";
    public static final String EXEMPTIONS_DELTA_TYPE = "exemption_delta";
    public static final String OFFICERS_LINK_TYPE = "officers";
    public static final String OFFICERS_DELTA_TYPE = "officer_delta";
    public static final String PSC_STATEMENTS_LINK_TYPE =
            "persons_with_significant_control_statements";
    public static final String PSC_STATEMENTS_DELTA_TYPE = "psc_statement_delta";

    private final String contextId;
    private final String companyNumber;
    private final String linkType;
    private final String deltaType;

    /**
     * Holds LinkRequest data.
     * @param contextId The x-request-id from the request header
     * @param companyNumber The company number and ID of the company
     * @param linkType The type of link
     * @param deltaType The type of delta
     */
    public LinkRequest(String contextId, String companyNumber, String linkType, String deltaType) {
        this.contextId = contextId;
        this.companyNumber = companyNumber;
        this.linkType = linkType;
        this.deltaType = deltaType;
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
}
