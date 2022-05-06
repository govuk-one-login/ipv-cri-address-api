package uk.gov.di.ipv.cri.address.library.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ErrorResponse {
    MISSING_QUERY_PARAMETERS(1001, "Missing query parameters for auth request"),
    FAILED_TO_PARSE_OAUTH_QUERY_STRING_PARAMETERS(
            1002, "Failed to parse oauth2-specific query string parameters"),
    MISSING_SHARED_ATTRIBUTES_JWT(1008, "Missing shared attributes JWT from request body"),
    FAILED_TO_PARSE_SHARED_ATTRIBUTES_JWT(1009, "Failed to parse shared attributes JWT"),
    MISSING_CLIENT_ID_QUERY_PARAMETER(1010, "Missing client_id query parameter"),
    FAILED_TO_RETRIEVE_CERTIFICATE(1011, "Failed to retrieve client certificate from SSM"),
    FAILED_TO_VERIFY_SIGNATURE(1012, "Failed to verify the signature of the JWT"),
    JWT_SIGNATURE_IS_INVALID(1013, "Signature of the shared attribute JWT is invalid"),
    INVALID_REDIRECT_URL(1014, "Provided redirect URL is not in those configured for client"),
    UNKNOWN_CLIENT_ID(1015, "Unknown client id provided in request params"),
    INVALID_REQUEST_PARAM(1016, "Invalid request param"),
    SERVER_ERROR(1017, "Postcode search failed due to a server error"),
    INVALID_POSTCODE(1018, "Invalid postcode param"),
    SESSION_VALIDATION_ERROR(1019, "Session Validation Exception"),
    SERVER_CONFIG_ERROR(1020, "Server Configuration Error"),
    MISSING_ADDRESS_SESSION_ITEM(1021, "Missing address session item"),
    MISSING_AUTHORIZATION_HEADER(1022, "Missing Authorization Header"),
    TOKEN_VALIDATION_ERROR(1023, "Token validation error"),
    VERIFIABLE_CREDENTIAL_ERROR(1024, "Verifiable Credential error");

    private final int code;
    private final String message;

    ErrorResponse(
            @JsonProperty(required = true, value = "code") int code,
            @JsonProperty(required = true, value = "message") String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
