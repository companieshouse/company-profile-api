package uk.gov.companieshouse.company.profile.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.company.RegisteredOfficeAddress;
import uk.gov.companieshouse.api.model.company.RegisteredOfficeAddressApi;

class RegisteredOfficeAddressMapperTest {

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

        assertNull(registeredOfficeAddressApi.getAddressLine1());
        assertNull(registeredOfficeAddressApi.getAddressLine2());
        assertNull(registeredOfficeAddressApi.getCareOf());
        assertNull(registeredOfficeAddressApi.getCountry());
        assertNull(registeredOfficeAddressApi.getLocality());
        assertNull(registeredOfficeAddressApi.getPoBox());
        assertNull(registeredOfficeAddressApi.getPostalCode());
        assertNull(registeredOfficeAddressApi.getPremises());
        assertNull(registeredOfficeAddressApi.getRegion());
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
        assertEquals(ADDRESS_LINE_1, registeredOfficeAddressApi.getAddressLine1());
        assertEquals(ADDRESS_LINE_2, registeredOfficeAddressApi.getAddressLine2());
        assertEquals(CARE_OF, registeredOfficeAddressApi.getCareOf());
        assertEquals(COUNTRY, registeredOfficeAddressApi.getCountry());
        assertEquals(LOCALITY, registeredOfficeAddressApi.getLocality());
        assertEquals(PO_BOX, registeredOfficeAddressApi.getPoBox());
        assertEquals(POSTAL_CODE, registeredOfficeAddressApi.getPostalCode());
        assertEquals(PREMISES, registeredOfficeAddressApi.getPremises());
        assertEquals(REGION, registeredOfficeAddressApi.getRegion());
    }
}
