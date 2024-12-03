package uk.gov.companieshouse.company.profile.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class DateUtilsTest {

    @Test
    void shouldReturnCurrentTimeAsStringWithCorrectFormat() {
        // given
        final String expectedTime = "2024-01-01T12:30:15";
        final Instant now = Instant.from(
                OffsetDateTime.of(2024, 1, 1, 12, 30, 15, 0, ZoneOffset.UTC));

        // when
        final String actual = DateUtils.publishedAtString(now);

        // then
        assertEquals(expectedTime, actual);
    }
}