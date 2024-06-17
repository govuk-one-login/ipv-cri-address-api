package uk.gov.di.ipv.cri.address.api.exceptions;

public class PostcodeValidationException extends RuntimeException {
    public PostcodeValidationException(String message) {
        super(message);
    }
}
