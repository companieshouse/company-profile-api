package uk.gov.companieshouse.company.profile.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.gov.companieshouse.api.company.CompanyProfile;

@Document(collection = "company_profile")
public class CompanyProfileDocument {
    @Id
    private String id;

    @JsonProperty("companyProfile")
    public CompanyProfile companyProfile;

    public CompanyProfileDocument(CompanyProfile companyProfile) {
        this.companyProfile = companyProfile;
    }
}