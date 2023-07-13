package uk.gov.companieshouse.company.profile.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.convert.WritingConverter;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.converter.WriteConverter;

@WritingConverter
public class CompanyProfileDataWriteConverter extends WriteConverter<Data> {
    public CompanyProfileDataWriteConverter(ObjectMapper objectMapper) {
        super(objectMapper);
    }
}
