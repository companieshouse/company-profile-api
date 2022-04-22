package uk.gov.companieshouse.company.profile.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.company.Data;

@ReadingConverter
public class DataReadConverter implements Converter<Document, Data> {

    private final ObjectMapper objectMapper;

    public DataReadConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Read convertor.
     * @param source source Document.
     * @return charge object.
     */
    @Override
    public Data convert(Document source) {
        try {
            return objectMapper.readValue(source.toJson(), Data.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
