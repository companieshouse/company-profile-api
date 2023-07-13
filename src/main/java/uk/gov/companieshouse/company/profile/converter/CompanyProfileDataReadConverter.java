package uk.gov.companieshouse.company.profile.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.convert.ReadingConverter;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.converter.ReadConverter;

@ReadingConverter
public class CompanyProfileDataReadConverter extends ReadConverter<Data> {
    public CompanyProfileDataReadConverter(ObjectMapper objectMapper, Class<Data> objectClass) {
        super(objectMapper, objectClass);
    }
}
