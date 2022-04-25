package uk.gov.companieshouse.company.profile.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import uk.gov.companieshouse.company.profile.exception.BadRequestException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;




public class OffsetDateTimeDeSerializer extends JsonDeserializer<OffsetDateTime> {

    public static final String APPLICATION_NAME_SPACE = "company-profile-api";

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    @Override
    public OffsetDateTime deserialize(JsonParser jsonParser, DeserializationContext
            deserializationContext) {
        try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            JsonNode jsonNode = jsonParser.readValueAsTree();
            return OffsetDateTime.parse(jsonNode.get("$date")
                    .textValue(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception exception) {
            LOGGER.error("OffsetDateTime Deserialization failed.", exception);
            throw new BadRequestException(exception.getMessage());
        }
    }
}
