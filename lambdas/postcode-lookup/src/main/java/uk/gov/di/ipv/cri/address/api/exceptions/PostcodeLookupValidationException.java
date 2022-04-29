package uk.gov.di.ipv.cri.address.api.exceptions;

public class PostcodeLookupValidationException extends RuntimeException {
    public PostcodeLookupValidationException() {
        super();
    }

    public PostcodeLookupValidationException(String message) {
        super(message);
    }

    public PostcodeLookupValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public PostcodeLookupValidationException(Throwable cause) {
        super(cause);
    }
}
