package uk.gov.companieshouse.company.profile.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDate;
import uk.gov.companieshouse.company.profile.util.DateFormatter;

public class LocalDateSerializer extends JsonSerializer<LocalDate> {

    @Override
    public void serialize(LocalDate localDate, JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider) throws IOException {
        if (localDate == null) {
            jsonGenerator.writeNull();
        } else {
            String format = DateFormatter.format(localDate);
            jsonGenerator.writeRawValue("ISODate(\"" + format + "\")");
        }
    }
}
