package uk.gov.companieshouse.company.profile.model;

import uk.gov.companieshouse.api.model.CompanyProfileDocument;

public class UnversionedCompanyProfileDocument  extends CompanyProfileDocument {

    private Long version;

    UnversionedCompanyProfileDocument() {
    }

    /**
     * Copy constructor.
     *
     * @param copy Document to copy
     */
    public UnversionedCompanyProfileDocument(CompanyProfileDocument copy) {
        this.setId(copy.getId());
        this.setHasMortgages(copy.isHasMortgages());
        this.setCompanyProfile(copy.getCompanyProfile());
        this.setDeltaAt(copy.getDeltaAt());
        this.setUpdated(copy.getUpdated());
        this.setParentCompanyNumber(copy.getParentCompanyNumber());
        this.version = 0L;
    }

    public Long getVersion() {
        return version;
    }

    public UnversionedCompanyProfileDocument version(Long version) {
        this.version = version;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        UnversionedCompanyProfileDocument that = (UnversionedCompanyProfileDocument) o;
        return version.equals(that.version);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }
}