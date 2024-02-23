package uk.gov.companieshouse.company.profile.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import uk.gov.companieshouse.api.exception.BadRequestException;
import uk.gov.companieshouse.company.profile.CompanyProfileApiApplication;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;


public class LocalDateDeSerializer extends JsonDeserializer<LocalDate> {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            CompanyProfileApiApplication.APPLICATION_NAME_SPACE);

    @Override
    public LocalDate deserialize(JsonParser jsonParser, DeserializationContext
            deserializationContext) {
        try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd'T'HH:mm[:ss[.SSS]]'Z'");
            JsonNode jsonNode = jsonParser.readValueAsTree();
            JsonNode dateNode = jsonNode.get("$date");

            LocalDate parsedDate = null;

            if (dateNode != null && !dateNode.isNull()) {
                if (dateNode.isTextual() && dateNode.textValue() != null) {
                    parsedDate = LocalDate.parse(dateNode.textValue(), dateTimeFormatter);
                } else if (dateNode.has("$numberLong")) {
                    long epochMillis = dateNode.get("$numberLong").asLong();
                    parsedDate = LocalDate.ofInstant(
                            Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
                }
            }

            parsedDate = Optional.ofNullable(parsedDate).orElse(LocalDate.now());

            /** If textValue() returns a value we received a string of
             * format yyyy-MM-dd'T'HH:mm:ss'Z
             * and use dateTimeFormatter to return LocalDate.
             *
             * Otherwise we received a long of milliseconds away
             * from 01/01/1970 and need to return
             * a LocalDate without dateTimeFormatter.
             */
            return parsedDate;
        } catch (Exception exception) {
            LOGGER.error("Deserialization failed.", exception);
            throw new BadRequestException(exception.getMessage());
        }
    }
}
