package uk.gov.di.ipv.cri.address.library.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ErrorResponse {
    LOOKUP_TIMEOUT(4010, "time out error"),
    LOOK_ERROR(4011, "lookup processing"),
    LOOKUP_SERVER(4012, "lookup server");
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

    public String getErrorSummary() {
        return getCode() + ": " + getMessage();
    }
}
