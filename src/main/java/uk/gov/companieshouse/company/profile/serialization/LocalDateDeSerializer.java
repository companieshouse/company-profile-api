package uk.gov.companieshouse.company.profile.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

import uk.gov.companieshouse.company.profile.exception.BadRequestException;
import uk.gov.companieshouse.company.profile.util.DateFormatter;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;


public class LocalDateDeSerializer extends JsonDeserializer<LocalDate> {

    public static final String APPLICATION_NAME_SPACE = "company-profile-api";

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    @Override
    public LocalDate deserialize(JsonParser jsonParser,
            DeserializationContext deserializationContext) throws IOException {
        JsonNode jsonNode = jsonParser.readValueAsTree();
        try {
            if (JsonNodeType.STRING.equals(jsonNode.getNodeType())) {
                var dateStr = jsonNode.textValue();
                return DateFormatter.parse(dateStr);
            } else {
                var dateJsonNode = jsonNode.get("$date");

                if (dateJsonNode.get("$numberLong") == null) {
                    return DateFormatter.parse(dateJsonNode.textValue());
                } else {
                    var longDate = dateJsonNode.get("$numberLong").asLong();
                    var dateStr = Instant.ofEpochMilli(new Date(longDate).getTime()).toString();
                    return DateFormatter.parse(dateStr);
                }
            }
        } catch (Exception exception) {
            LOGGER.error("LocalDate Deserialization failed.", exception);
            throw new BadRequestException(exception.getMessage());
        }
    }
}
