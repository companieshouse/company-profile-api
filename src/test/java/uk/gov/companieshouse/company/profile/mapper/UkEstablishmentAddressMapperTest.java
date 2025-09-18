package uk.gov.companieshouse.company.profile.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
 
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.company.RegisteredOfficeAddress;
import uk.gov.companieshouse.api.model.ukestablishments.PrivateUkEstablishmentsAddressApi;
import uk.gov.companieshouse.company.profile.model.VersionedCompanyProfileDocument;

@ExtendWith(MockitoExtension.class)
public class UkEstablishmentAddressMapperTest {
    
    private static final String POSTAL_CODE = "TE57 1NG";
    private static final String LINE_1 = "123 Test St";
    private static final String COMPANY_NUMBER = "12345678";
    @Mock
    private RegisteredOfficeAddressMapper registeredOfficeAddressMapper;

    @Test
    void testMapToUkEstablishmentAddress() {
        RegisteredOfficeAddress registeredOfficeAddress = new RegisteredOfficeAddress();
        registeredOfficeAddress.setAddressLine1(LINE_1);
        registeredOfficeAddress.setPostalCode(POSTAL_CODE);

        VersionedCompanyProfileDocument versionedCompanyProfileDocument = new VersionedCompanyProfileDocument();
        Data data = new Data();
        data.setCompanyNumber(COMPANY_NUMBER);
        data.setRegisteredOfficeAddress(registeredOfficeAddress);
        versionedCompanyProfileDocument.setCompanyProfile(data);

        PrivateUkEstablishmentsAddressApi ukEstablishmentAddress = UkEstablishmentAddressMapper.mapToUkEstablishmentAddress(versionedCompanyProfileDocument);
        assertNotNull(ukEstablishmentAddress);
        assertEquals(COMPANY_NUMBER, ukEstablishmentAddress.getCompanyNumber());
        assertEquals(LINE_1, ukEstablishmentAddress.getRegisteredOfficeAddress().getAddressLine1());
        assertEquals(POSTAL_CODE, ukEstablishmentAddress.getRegisteredOfficeAddress().getPostalCode());
    }

    @Test
    void testMapToUkEstablishmentAddressWithNullInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            UkEstablishmentAddressMapper.mapToUkEstablishmentAddress(null);
        });
    }

    @Test 
    void testMapToUkEstablishmentAddressWithNullCompanyProfile() {
        VersionedCompanyProfileDocument versionedCompanyProfileDocument = new VersionedCompanyProfileDocument();
        versionedCompanyProfileDocument.setCompanyProfile(null);

        assertThrows(IllegalArgumentException.class, () -> {
            UkEstablishmentAddressMapper.mapToUkEstablishmentAddress(versionedCompanyProfileDocument);
        });
    }
}
