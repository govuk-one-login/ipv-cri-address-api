package uk.gov.di.ipv.cri.address.library.exception;

public class SessionExpiredException extends Exception {

    public SessionExpiredException(String message) {
        super(message);
    }

    public SessionExpiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public SessionExpiredException(Throwable cause) {
        super(cause);
    }
}
