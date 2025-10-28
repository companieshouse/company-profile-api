package uk.gov.companieshouse.company.profile.util;

import java.util.function.Function;
import uk.gov.companieshouse.api.company.Links;

public record LinkTypeData(String deltaType, Function<Links, String> linkGetter) {

}
