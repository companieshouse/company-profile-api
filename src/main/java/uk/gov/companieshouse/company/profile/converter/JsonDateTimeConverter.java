package uk.gov.companieshouse.company.profile.converter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import org.bson.json.Converter;
import org.bson.json.StrictJsonWriter;

public class JsonDateTimeConverter implements Converter<Long> {

    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_INSTANT
            .withZone(ZoneId.of("UTC"));

    /**
     * Called internally by classes annotated with @ReadingConverter.
     * Converts the ISODate() mongo value to a string with a given format.
     * @param value the ISODate value
     * @param writer the JsonWriter used to write the data as a string
     */
    @Override
    public void convert(Long value, StrictJsonWriter writer) {
        try {
            Instant instant = new Date(value).toInstant();
            String formattedDate = DATE_TIME_FORMATTER.format(instant);
            writer.writeString(formattedDate);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}