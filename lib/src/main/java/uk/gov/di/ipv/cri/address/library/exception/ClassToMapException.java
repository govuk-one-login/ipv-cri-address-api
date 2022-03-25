package uk.gov.di.ipv.cri.address.library.exception;

public class ClassToMapException extends RuntimeException {
    public ClassToMapException() {
        super();
    }

    public ClassToMapException(String message) {
        super(message);
    }

    public ClassToMapException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClassToMapException(Throwable cause) {
        super(cause);
    }
}
