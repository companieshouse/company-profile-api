package uk.gov.companieshouse.company.profile.model;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;

import uk.gov.companieshouse.api.company.Data;

@Document(collection = "#{@environment.getProperty('spring.data.mongodb.collection')}")
public class CompanyProfileDocument {
    @Id
    private String id;

    @Field("has_mortgages")
    public Boolean hasMortgages;

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
                                  Updated updated, boolean hasMortgages) {
        this.companyProfile = companyProfile;
        this.deltaAt = deltaAt;
        this.updated = updated;
        this.hasMortgages = hasMortgages;
    }

    public String getId() {
        return id;
    }

    public CompanyProfileDocument setId(String id) {
        this.id = id;
        return this;
    }

    public Data getCompanyProfile() {
        return companyProfile;
    }

    public CompanyProfileDocument setCompanyProfile(Data companyProfile) {
        this.companyProfile = companyProfile;
        return this;
    }

    public LocalDateTime getDeltaAt() {
        return deltaAt;
    }

    public CompanyProfileDocument setDeltaAt(LocalDateTime deltaAt) {
        this.deltaAt = deltaAt;
        return this;
    }

    public Updated getUpdated() {
        return updated;
    }

    public CompanyProfileDocument setUpdated(Updated updated) {
        this.updated = updated;
        return this;
    }

    public boolean isHasMortgages() {
        return hasMortgages;
    }

    public void setHasMortgages(boolean hasMortgages) {
        this.hasMortgages = hasMortgages;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CompanyProfileDocument that = (CompanyProfileDocument) obj;
        return id.equals(that.id) && companyProfile.equals(
                that.companyProfile)
                && updated.equals(that.updated)
                && hasMortgages == that.hasMortgages;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, companyProfile, updated, hasMortgages);
    }
}