package uk.gov.di.ipv.cri.address.api.pii;

import java.util.regex.Pattern;

public class PiiPostcodeMasker {
    private static final Pattern POSTCODE_PATTERN =
            Pattern.compile(
                    "\\b[0-9A-Z]{1,4}\\d[A-Z\\d]?\\s?\\d[A-Z]{2}\\b|\\b[0-9A-Z]{3,8}\\b\n",
                    Pattern.CASE_INSENSITIVE);

    private PiiPostcodeMasker() {
        throw new IllegalStateException("This class is not meant to be instantiated");
    }

    public static String sanitize(String value) {
        return POSTCODE_PATTERN
                .matcher(value == null ? "" : value)
                .replaceAll(match -> "*".repeat(match.group().length()));
    }
}
