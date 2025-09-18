package uk.gov.companieshouse.company.profile.mapper;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

import uk.gov.companieshouse.api.company.RegisteredOfficeAddress;
import uk.gov.companieshouse.api.model.company.RegisteredOfficeAddressApi;

public class RegisteredOfficeAddressMapperTest {

    private static final String ADDRESS_LINE_1 = "addressLine1";
    private static final String ADDRESS_LINE_2 = "addressLine2";
    private static final String CARE_OF = "careOf";
    private static final String COUNTRY = "country";
    private static final String LOCALITY = "locality";
    private static final String PO_BOX = "poBox";
    private static final String POSTAL_CODE = "postalCode";
    private static final String PREMISES = "premises";
    private static final String REGION = "region";

    @Test
    void testMapToApiAllValuesAreAllowedToBeNull() {
        RegisteredOfficeAddress registeredOfficeAddress = new RegisteredOfficeAddress();
        RegisteredOfficeAddressApi registeredOfficeAddressApi = RegisteredOfficeAddressMapper.mapToApi(registeredOfficeAddress);
        assertEquals(registeredOfficeAddressApi.getAddressLine1(), null);
        assertEquals(registeredOfficeAddressApi.getAddressLine2(), null);
        assertEquals(registeredOfficeAddressApi.getCareOf(), null);
        assertEquals(registeredOfficeAddressApi.getCountry(), null);
        assertEquals(registeredOfficeAddressApi.getLocality(), null);
        assertEquals(registeredOfficeAddressApi.getPoBox(), null);
        assertEquals(registeredOfficeAddressApi.getPostalCode(), null);
        assertEquals(registeredOfficeAddressApi.getPremises(), null);
        assertEquals(registeredOfficeAddressApi.getRegion(), null);
    }

    @Test
    void testMapToApiAllValuesAreMapped() {
        RegisteredOfficeAddress registeredOfficeAddress = new RegisteredOfficeAddress();
        registeredOfficeAddress.setAddressLine1(ADDRESS_LINE_1);
        registeredOfficeAddress.setAddressLine2(ADDRESS_LINE_2);
        registeredOfficeAddress.setCareOf(CARE_OF);
        registeredOfficeAddress.setCountry(COUNTRY);
        registeredOfficeAddress.setLocality(LOCALITY);
        registeredOfficeAddress.setPoBox(PO_BOX);
        registeredOfficeAddress.setPostalCode(POSTAL_CODE);
        registeredOfficeAddress.setPremises(PREMISES);
        registeredOfficeAddress.setRegion(REGION);
        RegisteredOfficeAddressApi registeredOfficeAddressApi = RegisteredOfficeAddressMapper.mapToApi(registeredOfficeAddress);
        assertEquals(registeredOfficeAddressApi.getAddressLine1(), ADDRESS_LINE_1);
        assertEquals(registeredOfficeAddressApi.getAddressLine2(), ADDRESS_LINE_2);
        assertEquals(registeredOfficeAddressApi.getCareOf(), CARE_OF);
        assertEquals(registeredOfficeAddressApi.getCountry(), COUNTRY);
        assertEquals(registeredOfficeAddressApi.getLocality(), LOCALITY);
        assertEquals(registeredOfficeAddressApi.getPoBox(), PO_BOX);
        assertEquals(registeredOfficeAddressApi.getPostalCode(), POSTAL_CODE);
        assertEquals(registeredOfficeAddressApi.getPremises(), PREMISES);
        assertEquals(registeredOfficeAddressApi.getRegion(), REGION);
    }
}
