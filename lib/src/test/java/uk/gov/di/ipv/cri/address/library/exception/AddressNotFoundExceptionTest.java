package uk.gov.di.ipv.cri.address.library.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AddressNotFoundExceptionTest {
    @Test
    void shouldCreateExceptionWithMessage() {
        String expectedMessage = "Address not found";
        AddressNotFoundException exception = new AddressNotFoundException(expectedMessage);

        assertNotNull(exception);
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void shouldThrowAndCatchCorrectly() {
        String expectedMessage = "Address not found";

        Exception thrown =
                assertThrows(
                        AddressNotFoundException.class,
                        () -> {
                            throw new AddressNotFoundException(expectedMessage);
                        });

        assertEquals(expectedMessage, thrown.getMessage());
    }
}
