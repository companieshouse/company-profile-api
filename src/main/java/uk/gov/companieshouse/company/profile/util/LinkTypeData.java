package uk.gov.companieshouse.company.profile.util;

import java.util.function.Function;
import uk.gov.companieshouse.api.company.Links;

public class LinkTypeData {

    private final String deltaType;
    private final Function<Links, String> linkGetter;

    public LinkTypeData(String deltaType, Function<Links, String> linkGetter) {
        this.deltaType = deltaType;
        this.linkGetter = linkGetter;
    }

    public String getDeltaType() {
        return deltaType;
    }

    public Function<Links, String> getLinkGetter() {
        return linkGetter;
    }
}
