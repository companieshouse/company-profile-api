package uk.gov.companieshouse.company.profile.converter;

import org.springframework.data.convert.ReadingConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.companieshouse.api.company.SensitiveData;
import uk.gov.companieshouse.api.converter.ReadConverter;

@ReadingConverter
public class SensitiveDataReadConverter extends ReadConverter<SensitiveData> {

    public SensitiveDataReadConverter(ObjectMapper objectMapper,  Class<SensitiveData> objectClass) {
        super(objectMapper,objectClass);
    }
}