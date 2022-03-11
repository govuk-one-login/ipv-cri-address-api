package uk.gov.di.ipv.cri.address.library.exception;

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
