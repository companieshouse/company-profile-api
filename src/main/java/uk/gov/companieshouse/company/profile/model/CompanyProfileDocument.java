package uk.gov.companieshouse.company.profile.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import uk.gov.companieshouse.api.company.Data;

@Document(collection = "company_profile")
public class CompanyProfileDocument {
    @Id
    private String id;

    @Field("data")
    public Data companyProfile;

    public CompanyProfileDocument(Data companyProfile) {
        this.companyProfile = companyProfile;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}