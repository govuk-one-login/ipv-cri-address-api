package uk.gov.di.ipv.cri.address.library.exceptions;

public class ServerException extends Exception {
    public ServerException(String message, Exception e) {
        super(message, e);
    }
}
