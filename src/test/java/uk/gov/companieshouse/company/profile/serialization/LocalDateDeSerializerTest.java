package uk.gov.companieshouse.company.profile.serialization;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.companieshouse.api.exception.BadRequestException;

import java.io.IOException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class LocalDateDeSerializerTest {

   private LocalDateDeSerializer deserializer;

   private ObjectMapper mapper;

   @BeforeEach
   void setUp() {
      deserializer = new LocalDateDeSerializer();
      mapper = new ObjectMapper();
   }

    @Test
    void dateShouldDeserialize() throws JsonParseException, IOException {
       String jsonTestString = "{\"date\":{\"$date\": \"2023-01-09T00:00:00Z\"}}";

       LocalDate returnedDate = deserialize(jsonTestString);
       assertEquals(LocalDate.of(2023, 1, 9), returnedDate);

    }

    @Test
    void deserializeWithMillisecondsTimestamp() throws JsonParseException, IOException{
       String jsonTestString = "{\"date\":{\"$date\": \"2023-01-09T18:19:39.396Z\"}}";

       LocalDate returnedDate = deserialize(jsonTestString);
       assertEquals(LocalDate.of(2023,1,9), returnedDate);
    }

    @Test
    void deserializeWith1digitMillisecondsTimestamp() throws JsonParseException, IOException{
        String jsonTestString = "{\"date\":{\"$date\": \"2023-01-09T18:19:39.3Z\"}}";

        LocalDate returnedDate = deserialize(jsonTestString);
        assertEquals(LocalDate.of(2023,1,9), returnedDate);
    }

    @Test
    void longStringReturnsLong() throws JsonParseException, IOException{
       String jsonTestString = "{\"date\":{\"$date\": {\"$numberLong\":\"-1431388800000\"}}}";

       LocalDate returnedDate = deserialize(jsonTestString);
       assertEquals(LocalDate.of(1924, 8, 23), returnedDate);

    }

    @Test
    void testParsedDateWithDateStringAsNull() throws  JsonParseException, IOException{
       String jsonTestString = "{\"date\":{\"$date\": null}}}";

       LocalDate returnedDate = deserialize(jsonTestString);
       assertEquals(null, returnedDate);

    }

    @Test
    void invalidStringReturnsError() throws JsonParseException, IOException{
       String jsonTestString = "{\"date\":{\"$date\": \"NotADate\"}}}";

       assertThrows(BadRequestException.class, ()->{
          deserialize(jsonTestString);
       });
    }

    @Test
    void nullStringReturnsError() throws JsonParseException, IOException{

        String jsonTestString = null;

        assertThrows(NullPointerException.class, ()->{
            deserialize(jsonTestString);
        });
    }

    private LocalDate deserialize(String jsonString) throws JsonParseException, IOException {
        JsonParser parser = mapper.getFactory().createParser(jsonString);
        DeserializationContext deserializationContext = mapper.getDeserializationContext();

        parser.nextToken();
        parser.nextToken();
        parser.nextToken();

        return deserializer.deserialize(parser, deserializationContext);
    }
}
