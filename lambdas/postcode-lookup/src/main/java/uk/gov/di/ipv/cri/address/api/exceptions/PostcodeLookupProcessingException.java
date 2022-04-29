package uk.gov.di.ipv.cri.address.api.exceptions;

public class PostcodeLookupProcessingException extends RuntimeException {
    public PostcodeLookupProcessingException() {
        super();
    }

    public PostcodeLookupProcessingException(String message) {
        super(message);
    }

    public PostcodeLookupProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public PostcodeLookupProcessingException(Throwable cause) {
        super(cause);
    }
}
