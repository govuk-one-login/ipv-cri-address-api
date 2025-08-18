package uk.gov.di.ipv.cri.address.api.exceptions;

public class ClientIdNotSupportedException extends RuntimeException {
    public ClientIdNotSupportedException(String message) {
        super(message);
    }

    public ClientIdNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }
}
