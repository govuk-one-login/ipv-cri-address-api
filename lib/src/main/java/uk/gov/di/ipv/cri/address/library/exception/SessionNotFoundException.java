package uk.gov.di.ipv.cri.address.library.exception;

public class SessionNotFoundException extends Exception {

    public SessionNotFoundException(String message) {
        super(message);
    }

    public SessionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public SessionNotFoundException(Throwable cause) {
        super(cause);
    }
}
