package uk.gov.companieshouse.company.profile.util;

import static java.time.ZoneOffset.UTC;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS")
            .withZone(UTC);

    private DateUtils() {
    }

    public static boolean isDeltaStale(final String requestDeltaAt, final LocalDateTime existingDeltaAt) {
        return requestDeltaAt == null ||
                (existingDeltaAt != null &&
                        OffsetDateTime.parse(requestDeltaAt, FORMATTER)
                                .isBefore(existingDeltaAt.atOffset(UTC)));
    }
}