package uk.gov.companieshouse.company.profile.util;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private static final DateTimeFormatter DELTA_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS")
            .withZone(UTC);
    private static final DateTimeFormatter PUBLISHED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            .withZone(UTC);

    private DateUtils() {
    }

    public static boolean isDeltaStale(final String requestDeltaAt, final LocalDateTime existingDeltaAt) {
        return requestDeltaAt == null ||
                (existingDeltaAt != null &&
                        OffsetDateTime.parse(requestDeltaAt, DELTA_AT_FORMATTER)
                                .isBefore(existingDeltaAt.atOffset(UTC)));
    }

    public static String publishedAtString(final Instant source) {
        return source.atOffset(UTC).format(PUBLISHED_AT_FORMATTER);
    }
}