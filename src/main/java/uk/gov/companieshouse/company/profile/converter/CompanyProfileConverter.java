package uk.gov.companieshouse.company.profile.converter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.company.Data;

@ReadingConverter
@Component
public class CompanyProfileConverter implements Converter<Document, Data> {

    private final ObjectMapper mapper;

    public CompanyProfileConverter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Data convert(Document source) {
        // Exclude serializing properties with null values
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            // use a custom converter for the ISO datetime stamps
            JsonWriterSettings writerSettings = JsonWriterSettings
                    .builder()
                    .dateTimeConverter(new JsonDateTimeConverter())
                    .build();
            return mapper.readValue(source.toJson(writerSettings), Data.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
