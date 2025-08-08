package uk.gov.di.ipv.cri.address.library.util;

import software.amazon.awssdk.utils.StringUtils;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;

import java.util.List;
import java.util.Set;

public class CountryCode {

    private CountryCode() {}

    private static final Set<String> GB_AND_CROWN_DEPENDENCIES = Set.of("GB", "GG", "JE", "IM");

    public static boolean isGreatBritain(final String code) {
        return "GB".equalsIgnoreCase(code);
    }

    public static boolean isGbAndCrownDependency(final String code) {
        return code != null && GB_AND_CROWN_DEPENDENCIES.contains(code.toUpperCase());
    }

    public static boolean isCountryCodeAbsentForAny(List<CanonicalAddress> addresses) {
        return addresses.stream().anyMatch(a -> StringUtils.isEmpty(a.getAddressCountry()));
    }
}
