package uk.gov.companieshouse.company.profile.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@ReadingConverter
@Component
public class CompanyProfileConverter implements Converter<Document, Data> {

    private final ObjectMapper mapper;

    public static final String APPLICATION_NAME_SPACE = "company-profile-api";
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    public CompanyProfileConverter(@Qualifier("mongoConverterMapper") ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Data convert(Document source) {
        try {
            // Use a custom converter for the ISO datetime stamps
            JsonWriterSettings writerSettings = JsonWriterSettings
                    .builder()
                    .dateTimeConverter(new JsonDateTimeConverter())
                    .build();
            return mapper.readValue(source.toJson(writerSettings), Data.class);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}
