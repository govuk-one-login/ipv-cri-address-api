package uk.gov.di.ipv.cri.address.library.exceptions;

public class ValidationException extends Exception {
    public ValidationException(String message, Exception e) {
        super(message, e);
    }

    public ValidationException(String message) {
        super(message);
    }
}
