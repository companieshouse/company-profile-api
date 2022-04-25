package uk.gov.companieshouse.company.profile.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import uk.gov.companieshouse.company.profile.exception.BadRequestException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

public class LocalDateTimeDeSerializer extends JsonDeserializer<LocalDateTime> {

    public static final String APPLICATION_NAME_SPACE = "company-profile-api";

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    @Override
    public LocalDateTime deserialize(JsonParser jsonParser, DeserializationContext
            deserializationContext) {
        try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            JsonNode jsonNode = jsonParser.readValueAsTree();
            return LocalDateTime.parse(jsonNode.get("$date").textValue(), dateTimeFormatter);
        } catch (Exception exception) {
            LOGGER.error("LocalDateTime Deserialization failed.", exception);
            throw new BadRequestException(exception.getMessage());
        }
    }
}
