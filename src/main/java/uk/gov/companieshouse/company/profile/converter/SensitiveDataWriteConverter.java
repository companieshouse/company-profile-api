package uk.gov.companieshouse.company.profile.converter;

import org.springframework.data.convert.WritingConverter;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.companieshouse.api.company.SensitiveData;
import uk.gov.companieshouse.api.converter.WriteConverter;


@WritingConverter
public class SensitiveDataWriteConverter extends WriteConverter<SensitiveData> {


    public SensitiveDataWriteConverter(ObjectMapper objectMapper) {
        super(objectMapper);
    }
}