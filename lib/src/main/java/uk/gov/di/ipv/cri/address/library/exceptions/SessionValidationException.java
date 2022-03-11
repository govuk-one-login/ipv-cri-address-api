package uk.gov.di.ipv.cri.address.library.exceptions;

public class SessionValidationException extends Exception {
    public SessionValidationException(String message, Exception e) {
        super(message, e);
    }

    public SessionValidationException(String message) {
        super(message);
    }
}
