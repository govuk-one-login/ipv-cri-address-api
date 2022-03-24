package uk.gov.di.ipv.cri.address.library.exception;

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ParseException;

public class AccessTokenRequestException extends ParseException {
    public AccessTokenRequestException(ErrorObject error) {
        super(error.getCode(), error);
    }

    public AccessTokenRequestException(String message, ErrorObject error) {
        super(message, error);
    }
}
