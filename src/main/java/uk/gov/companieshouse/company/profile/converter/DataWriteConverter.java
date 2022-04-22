package uk.gov.companieshouse.company.profile.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import uk.gov.companieshouse.api.company.Data;

@WritingConverter
public class DataWriteConverter implements Converter<Data, BasicDBObject> {

    private final ObjectMapper objectMapper;

    public DataWriteConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Write convertor.
     * @param source source Document.
     * @return charge BSON object.
     */
    @Override
    public BasicDBObject convert(Data source) {
        try {
            return BasicDBObject.parse(objectMapper.writeValueAsString(source));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
