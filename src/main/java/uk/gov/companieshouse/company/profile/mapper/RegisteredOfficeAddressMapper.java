package uk.gov.companieshouse.company.profile.mapper;

import uk.gov.companieshouse.api.company.RegisteredOfficeAddress;
import uk.gov.companieshouse.api.model.company.RegisteredOfficeAddressApi;

public class RegisteredOfficeAddressMapper {

    private RegisteredOfficeAddressMapper() {
    }

    public static RegisteredOfficeAddressApi mapToApi(RegisteredOfficeAddress registeredOfficeAddress) {

        RegisteredOfficeAddressApi registeredOfficeAddressApi = new RegisteredOfficeAddressApi();
        registeredOfficeAddressApi.setAddressLine1(registeredOfficeAddress.getAddressLine1());
        registeredOfficeAddressApi.setAddressLine2(registeredOfficeAddress.getAddressLine2());
        registeredOfficeAddressApi.setCareOf(registeredOfficeAddress.getCareOf());
        registeredOfficeAddressApi.setCountry(registeredOfficeAddress.getCountry());
        registeredOfficeAddressApi.setLocality(registeredOfficeAddress.getLocality());
        registeredOfficeAddressApi.setPoBox(registeredOfficeAddress.getPoBox());
        registeredOfficeAddressApi.setPostalCode(registeredOfficeAddress.getPostalCode());
        registeredOfficeAddressApi.setPremises(registeredOfficeAddress.getPremises());
        registeredOfficeAddressApi.setRegion(registeredOfficeAddress.getRegion());
        return registeredOfficeAddressApi;
    }
}
