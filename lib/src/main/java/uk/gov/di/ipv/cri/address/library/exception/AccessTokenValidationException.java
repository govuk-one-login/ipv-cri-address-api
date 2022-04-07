package uk.gov.di.ipv.cri.address.library.exception;

public class AccessTokenValidationException extends Exception {
    public AccessTokenValidationException(Throwable cause) {
        super(cause);
    }

    public AccessTokenValidationException(String message) {
        super(message);
    }
}
