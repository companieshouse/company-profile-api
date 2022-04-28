package uk.gov.companieshouse.company.profile.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateFormatter {

    static final String strPattern = "\\d{4}-\\d{2}-\\d{2}";
    static final Pattern pattern = Pattern.compile(strPattern);

    static DateTimeFormatter WriteDateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    static DateTimeFormatter readDateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Parse date string to LocalDate.
     * @param dateStr date as string.
     * @return parsed date.
     */
    public static LocalDate parse(String dateStr) {
        Matcher matcher = pattern.matcher(dateStr);
        matcher.find();
        return LocalDate.parse(matcher.group(), readDateTimeFormatter);
    }

    /**
     * Format date.
     * @param localDate date to format.
     * @return formatted date as string.
     */
    public static String format(LocalDate localDate) {
        return localDate.atStartOfDay().format(WriteDateTimeFormatter);
    }

}
