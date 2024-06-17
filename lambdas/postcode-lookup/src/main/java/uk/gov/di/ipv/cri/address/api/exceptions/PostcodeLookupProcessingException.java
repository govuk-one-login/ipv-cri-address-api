package uk.gov.di.ipv.cri.address.api.exceptions;

public class PostcodeLookupProcessingException extends RuntimeException {
    public PostcodeLookupProcessingException(String message) {
        super(message);
    }

    public PostcodeLookupProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
