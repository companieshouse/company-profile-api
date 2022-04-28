package uk.gov.companieshouse.company.profile.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DateFormatterTest {

    @Test
    public void shouldParseAndFormatGivenDateString() {
        LocalDate parsedValue = DateFormatter.parse("2015-06-26T08:31:35.058Z");
        assertThat(parsedValue).isNotNull();
        assertThat(parsedValue.toString()).isEqualTo("2015-06-26");
    }

    @Test
    public void throwExceptionWhenGivenWrongDate() {
        assertThrows(IllegalStateException.class, () -> DateFormatter.parse("2015 08:31:35.058Z"));
    }

    @Test
    public void shouldFormatGivenDateString() {
        String formattedDate = DateFormatter.format(LocalDate.of(2015, 06, 26));
        assertThat(formattedDate).isNotNull();
        assertThat(formattedDate).isEqualTo("2015-06-26T00:00:00Z");
    }
}
