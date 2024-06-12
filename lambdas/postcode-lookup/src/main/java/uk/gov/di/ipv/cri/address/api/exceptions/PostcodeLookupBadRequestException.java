package uk.gov.di.ipv.cri.address.api.exceptions;

public class PostcodeLookupBadRequestException extends RuntimeException {
    public PostcodeLookupBadRequestException(String message) {
        super(message);
    }

    public PostcodeLookupBadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
