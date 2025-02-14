package uk.gov.di.ipv.cri.address.api.pii;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PiiPostcodeMaskerTest {
    @Test
    void validPostcodeCanBeMasked() {
        assertEquals("********", PiiPostcodeMasker.sanitize("SW1A 1AA"));
        assertEquals("********", PiiPostcodeMasker.sanitize("EC1A 1BB"));
        assertEquals("*******", PiiPostcodeMasker.sanitize("W1A 0AX"));
    }

    @Test
    void postcodeInSentenceCanBeMasked() {
        assertEquals(
                "My address is ********", PiiPostcodeMasker.sanitize("My address is SW1A 1AA"));
        assertEquals("******** is in London", PiiPostcodeMasker.sanitize("EC1A 1BB is in London"));
    }

    @Test
    void multiplePostcodesInSentenceCanBeMasked() {
        assertEquals(
                "******* and ******** are valid postcodes",
                PiiPostcodeMasker.sanitize("W1A 0AX and SW1A 2AA are valid postcodes"));
    }

    @Test
    void nullInputReturnsEmptyString() {
        assertEquals("", PiiPostcodeMasker.sanitize(""));
    }

    @Test
    void emptyStringReturnsEmptyString() {
        assertEquals("", PiiPostcodeMasker.sanitize(""));
    }

    @Test
    void noPostcodeInSentenceReturnsSentenceWithoutAnyMasking() {
        assertEquals(
                "This is just a normal sentence.",
                PiiPostcodeMasker.sanitize("This is just a normal sentence."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"B16 9BL", "sw1a 1aa", "EC1A 1BB"})
    void lowerCasePostcodeCanBeMasked(String postcode) {
        assertEquals("*".repeat(postcode.length()), PiiPostcodeMasker.sanitize(postcode));
    }

    @Test
    void postcodeWithExtraSpacesCanBeMasked() {
        assertEquals("   ********   ", PiiPostcodeMasker.sanitize("   SW1A 1AA   "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"B16 9BL", "sw1a 1aa", "EC1A 1BB", "ABC", "123"})
    void postcodeLikeValueCanBeMasked(String postcode) {
        assertEquals(
                "SO1. Requested postcode was " + "*".repeat(postcode.length()) + "",
                PiiPostcodeMasker.sanitize(
                        String.format("SO1. Requested postcode was %s", postcode)));
    }
}
