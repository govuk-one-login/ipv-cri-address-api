package uk.gov.di.ipv.cri.address.api.exceptions;

public class PostcodeLookupTimeoutException extends RuntimeException {

    public PostcodeLookupTimeoutException() {
        super();
    }

    public PostcodeLookupTimeoutException(String message) {
        super(message);
    }

    public PostcodeLookupTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public PostcodeLookupTimeoutException(Throwable cause) {
        super(cause);
    }
}
