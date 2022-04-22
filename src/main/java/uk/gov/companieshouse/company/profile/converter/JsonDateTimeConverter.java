package uk.gov.companieshouse.company.profile.converter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import org.bson.json.Converter;
import org.bson.json.StrictJsonWriter;
import uk.gov.companieshouse.company.profile.exception.BadRequestException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

public class JsonDateTimeConverter implements Converter<Long> {

    public static final String APPLICATION_NAME_SPACE = "company-profile-api";
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"));

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
            LOGGER.error("ISODate deserialisation failed", ex);
            throw new BadRequestException(ex.getMessage());
        }
    }
}