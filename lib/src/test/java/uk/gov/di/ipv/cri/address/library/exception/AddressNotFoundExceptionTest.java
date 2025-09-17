package uk.gov.di.ipv.cri.address.library.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AddressNotFoundExceptionTest {
    private static final String EXPECTED_MESSAGE = "Address not found";

    @Test
    void createsExceptionWithMessage() {
        AddressNotFoundException exception = new AddressNotFoundException(EXPECTED_MESSAGE);

        assertNotNull(exception);
        assertEquals(EXPECTED_MESSAGE, exception.getMessage());
    }

    @Test
    void createsAddressNotFoundExceptionWithMessageAndException() {
        AddressNotFoundException addressNotFoundException =
                new AddressNotFoundException(EXPECTED_MESSAGE, new RuntimeException());

        assertNotNull(addressNotFoundException);
        assertEquals(EXPECTED_MESSAGE, addressNotFoundException.getMessage());
    }

    @Test
    void throwsAndCatchCorrectly() {
        Exception thrown =
                assertThrows(
                        AddressNotFoundException.class,
                        () -> {
                            throw new AddressNotFoundException(EXPECTED_MESSAGE);
                        });

        assertEquals(EXPECTED_MESSAGE, thrown.getMessage());
    }
}
