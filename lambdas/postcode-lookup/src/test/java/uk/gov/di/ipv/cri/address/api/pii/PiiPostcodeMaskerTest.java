package uk.gov.di.ipv.cri.address.api.pii;

import org.junit.jupiter.api.Test;

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
        assertEquals("", PiiPostcodeMasker.sanitize(null));
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

    @Test
    void lowerCasePostcodeCanBeMasked() {
        assertEquals("********", PiiPostcodeMasker.sanitize("sw1a 1aa"));
    }

    @Test
    void postcodeWithExtraSpacesCanBeMasked() {
        assertEquals("   ********   ", PiiPostcodeMasker.sanitize("   SW1A 1AA   "));
    }

    @Test
    void postcodeLikeValueCanBeMasked() {
        assertEquals(
                "Requested postcode was *******",
                PiiPostcodeMasker.sanitize("Requested postcode was 5WF12LZ"));
    }
}
