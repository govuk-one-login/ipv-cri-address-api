package uk.gov.di.ipv.cri.address.api.pii;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PiiPostcodeMasker {
    private static final List<Pattern> POSTCODE_PATTERNS =
            List.of(
                    Pattern.compile(
                            "\\b[0-9A-Z]{1,4}\\d[A-Z\\d]?\\s?\\d[A-Z]{2}\\b|\\b[0-9A-Z]{3,8}\\b\n",
                            Pattern.CASE_INSENSITIVE),
                    Pattern.compile(
                            "(\\s*SO1\\.\\s*Requested\\s+postcode\\s+was\\s*)([A-Z0-9]+)",
                            Pattern.CASE_INSENSITIVE));

    private PiiPostcodeMasker() {
        throw new IllegalStateException("This class is not meant to be instantiated");
    }

    public static String sanitize(String postcodePhrase) {
        if (postcodePhrase == null || postcodePhrase.isEmpty()) {
            return postcodePhrase;
        }

        for (Pattern pattern : POSTCODE_PATTERNS) {
            Matcher matcher = pattern.matcher(postcodePhrase);

            postcodePhrase =
                    matcher.replaceAll(
                            match -> {
                                if (match.groupCount() > 1) {
                                    return match.group(1) + "*".repeat(match.group(2).length());
                                } else {
                                    return "*".repeat(match.group().length());
                                }
                            });
        }
        return postcodePhrase;
    }
}
