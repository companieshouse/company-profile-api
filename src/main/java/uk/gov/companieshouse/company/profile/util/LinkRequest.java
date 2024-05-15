package uk.gov.companieshouse.company.profile.util;

import java.util.function.Function;
import uk.gov.companieshouse.api.company.Links;

public class LinkRequest {

    public static final String CHARGES_LINK_TYPE = "charges";
    public static final String CHARGES_DELTA_TYPE = "charges_delta";
    public static final Function<Links, String> CHARGES_GET = Links::getCharges;

    public static final String EXEMPTIONS_LINK_TYPE = "exemptions";
    public static final String EXEMPTIONS_DELTA_TYPE = "exemption_delta";
    public static final Function<Links, String> EXEMPTIONS_GET = Links::getExemptions;

    public static final String INSOLVENCY_LINK_TYPE = "insolvency";
    public static final String INSOLVENCY_DELTA_TYPE = "insolvency_delta";
    public static final Function<Links, String> INSOLVENCY_GET = Links::getInsolvency;

    public static final String OFFICERS_LINK_TYPE = "officers";
    public static final String OFFICERS_DELTA_TYPE = "officer_delta";
    public static final Function<Links, String> OFFICERS_GET = Links::getOfficers;

    public static final String PSC_LINK_TYPE = "persons-with-significant-control";
    public static final String PSC_DELTA_TYPE = "psc_delta";
    public static final Function<Links, String> PSC_GET = Links::getPersonsWithSignificantControl;

    public static final String PSC_STATEMENTS_LINK_TYPE =
            "persons-with-significant-control-statements";
    public static final String PSC_STATEMENTS_DELTA_TYPE = "psc_statement_delta";
    public static final Function<Links, String> PSC_STATEMENTS_GET =
            Links::getPersonsWithSignificantControlStatements;

    public static final String FILING_HISTORY_LINK_TYPE = "filing-history";
    public static final String FILING_HISTORY_DELTA_TYPE = "filing_history_delta";
    public static final Function<Links, String> FILING_HISTORY_GET =
            Links::getFilingHistory;

    public static final String UK_ESTABLISHMENTS_LINK_TYPE = "uk-establishments";
    public static final String UK_ESTABLISHMENTS_DELTA_TYPE = "uk_establishment_delta";
    public static final Function<Links, String> UK_ESTABLISHMENTS_GET =
            Links::getUkEstablishments;

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
