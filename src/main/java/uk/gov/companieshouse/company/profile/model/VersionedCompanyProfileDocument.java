package uk.gov.companieshouse.company.profile.model;

import org.springframework.data.annotation.Version;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.model.CompanyProfileDocument;
import uk.gov.companieshouse.api.model.Updated;
import java.time.LocalDateTime;
import java.util.Objects;

public class VersionedCompanyProfileDocument extends CompanyProfileDocument {

    @Version
    private Long version;

    public VersionedCompanyProfileDocument() {
    }

    public VersionedCompanyProfileDocument(Data companyProfile, LocalDateTime deltaAt, Updated updated, boolean hasMortgages) {
        super(companyProfile, deltaAt, updated, hasMortgages);
    }

    public Long getVersion() {
        return version;
    }

    public VersionedCompanyProfileDocument version(Long version) {
        this.version = version;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        VersionedCompanyProfileDocument that = (VersionedCompanyProfileDocument) o;
        return Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(version);
        return result;
    }
}
