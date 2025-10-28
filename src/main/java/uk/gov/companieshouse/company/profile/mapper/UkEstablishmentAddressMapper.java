package uk.gov.companieshouse.company.profile.mapper;

import uk.gov.companieshouse.api.model.company.RegisteredOfficeAddressApi;
import uk.gov.companieshouse.api.model.ukestablishments.PrivateUkEstablishmentsAddressApi;
import uk.gov.companieshouse.company.profile.model.VersionedCompanyProfileDocument;

public class UkEstablishmentAddressMapper {

    private UkEstablishmentAddressMapper() {
    }

    public static PrivateUkEstablishmentsAddressApi mapToUkEstablishmentAddress(
            VersionedCompanyProfileDocument versionedCompanyProfileDocument) {
        if (versionedCompanyProfileDocument == null || versionedCompanyProfileDocument.getCompanyProfile() == null) {
            throw new IllegalArgumentException("VersionedCompanyProfileDocument or its CompanyProfile cannot be null");
        }
        RegisteredOfficeAddressApi registeredOfficeAddressApi = RegisteredOfficeAddressMapper.mapToApi(
                versionedCompanyProfileDocument.getCompanyProfile().getRegisteredOfficeAddress());
        PrivateUkEstablishmentsAddressApi ukEstablishmentAddress = new PrivateUkEstablishmentsAddressApi();
        ukEstablishmentAddress.setCompanyNumber(versionedCompanyProfileDocument.getCompanyProfile().getCompanyNumber());
        ukEstablishmentAddress.setRegisteredOfficeAddress(registeredOfficeAddressApi);
        return ukEstablishmentAddress;
    }

}
