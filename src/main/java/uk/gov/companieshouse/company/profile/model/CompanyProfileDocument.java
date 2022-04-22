package uk.gov.companieshouse.company.profile.model;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;
import uk.gov.companieshouse.api.company.Data;

@Document(collection = "#{@environment.getProperty('spring.data.mongodb.collection')}")
public class CompanyProfileDocument {
    @Id
    private String id;

    @Field("data")
    public Data companyProfile;

    @Field("delta_at")
    @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
    )
    private LocalDateTime deltaAt;

    private Updated updated;

    public CompanyProfileDocument(Updated updated) {
        this.updated = updated;
    }

    public CompanyProfileDocument() {
    }

    /** .
     * @param companyProfile companyProfile
     * @param deltaAt deltaAt
     * @param updated updated
     */
    public CompanyProfileDocument(Data companyProfile, LocalDateTime deltaAt,
                                  Updated updated) {
        this.companyProfile = companyProfile;
        this.deltaAt = deltaAt;
        this.updated = updated;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Data getCompanyProfile() {
        return companyProfile;
    }

    public void setCompanyProfile(Data companyProfile) {
        this.companyProfile = companyProfile;
    }

    public LocalDateTime getDeltaAt() {
        return deltaAt;
    }

    public void setDeltaAt(LocalDateTime deltaAt) {
        this.deltaAt = deltaAt;
    }

    public Updated getUpdated() {
        return updated;
    }

    public void setUpdated(Updated updated) {
        this.updated = updated;
    }
}