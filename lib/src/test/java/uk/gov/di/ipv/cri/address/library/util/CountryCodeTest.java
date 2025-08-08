package uk.gov.di.ipv.cri.address.library.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CountryCodeTest {
    @Nested
    @DisplayName("isGreatBritain Tests")
    class IsGreatBritainTests {
        @ParameterizedTest(name = "Should return true for \"{0}\"")
        @ValueSource(strings = {"GB", "gb", "Gb"})
        @DisplayName("Returns true for valid GB variants")
        void returnsTrueForGbAlternatives(String input) {
            assertTrue(CountryCode.isGreatBritain(input));
        }

        @ParameterizedTest(name = "Should return false for \"{0}\"")
        @ValueSource(strings = {"GG", "JE", "IM", "UK", "NI", "", " gb ", "GBR", "g"})
        @DisplayName("Returns false for invalid or malformed inputs")
        void returnsFalseWhenNonGbOrMalformed(String input) {
            assertFalse(CountryCode.isGreatBritain(input));
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("Returns false for null input")
        void returnsFalseForNull(String input) {
            assertFalse(CountryCode.isGreatBritain(input));
        }
    }

    @Nested
    @DisplayName("isGbAndCrownDependency Tests")
    class IsGbAndCrownDependencyTests {
        @ParameterizedTest(name = "Should return true for \"{0}\"")
        @ValueSource(strings = {"GB", "gg", "Je", "im"})
        @DisplayName("Returns true for valid GB and crown dependency codes")
        void returnsTrueForValidCodes(String input) {
            assertTrue(CountryCode.isGbAndCrownDependency(input));
        }

        @ParameterizedTest(name = "Should return false for \"{0}\"")
        @ValueSource(strings = {"UK", "NI", "FR", "DE", "US", "", " gb ", "GBR", "g"})
        @DisplayName("Returns false for invalid or malformed inputs")
        void returnsFalseForInvalidCodes(String input) {
            assertFalse(CountryCode.isGbAndCrownDependency(input));
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("Returns false for null input")
        void returnsFalseForNull(String input) {
            assertFalse(CountryCode.isGbAndCrownDependency(input));
        }
    }

    @Nested
    @DisplayName("isCountryCodeAbsentForAny Tests")
    class IsCountryCodeAbsentForAnyTests {
        @Test
        @DisplayName("Returns false when all addresses have a valid country code")
        void returnsFalseWhenAllCountryCodesArePresent() {
            CanonicalAddress gb = new CanonicalAddress();
            gb.setAddressCountry("GB");

            CanonicalAddress je = new CanonicalAddress();
            je.setAddressCountry("JE");

            CanonicalAddress im = new CanonicalAddress();
            im.setAddressCountry("IM");

            assertFalse(CountryCode.isCountryCodeAbsentForAny(List.of(gb, je, im)));
        }

        @Test
        @DisplayName("Returns true when at least one address has an empty country code")
        void returnsTrueWhenOneCountryCodeIsMissing() {

            CanonicalAddress gb = new CanonicalAddress();
            gb.setAddressCountry("GB");

            CanonicalAddress je = new CanonicalAddress();
            je.setAddressCountry("");

            CanonicalAddress im = new CanonicalAddress();
            im.setAddressCountry("IM");

            assertTrue(CountryCode.isCountryCodeAbsentForAny(List.of(gb, je, im)));
        }

        @Test
        @DisplayName("Returns true when at least one address has a null country code")
        void returnsTrueWhenOneCountryCodeIsNull() {

            CanonicalAddress gb = new CanonicalAddress();
            gb.setAddressCountry("GB");
            CanonicalAddress je = new CanonicalAddress();
            je.setAddressCountry(null);
            CanonicalAddress im = new CanonicalAddress();
            im.setAddressCountry("IM");

            assertTrue(CountryCode.isCountryCodeAbsentForAny(List.of(gb, je, im)));
        }

        @Test
        @DisplayName("Returns true when all addresses have empty or null country codes")
        void returnsTrueWhenAllCountryCodesAreMissing() {
            CanonicalAddress nullCountryCode = new CanonicalAddress();
            nullCountryCode.setAddressCountry(null);

            CanonicalAddress emptyCountryCode = new CanonicalAddress();
            emptyCountryCode.setAddressCountry("");

            assertTrue(
                    CountryCode.isCountryCodeAbsentForAny(
                            List.of(nullCountryCode, emptyCountryCode)));
        }

        @Test
        @DisplayName(
                "Returns false when the list is empty (no country codes are missing because none exist)")
        void returnsFalseWhenAddressListIsEmpty() {
            List<CanonicalAddress> addresses = Collections.emptyList();

            assertFalse(CountryCode.isCountryCodeAbsentForAny(addresses));
        }
    }
}
