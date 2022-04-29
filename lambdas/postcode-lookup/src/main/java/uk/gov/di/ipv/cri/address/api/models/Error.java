package uk.gov.di.ipv.cri.address.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Error {

    @JsonProperty("statuscode")
    private Integer statuscode;

    @JsonProperty("message")
    private String message;

    public Integer getStatuscode() {
        return statuscode;
    }

    public void setStatuscode(Integer statuscode) {
        this.statuscode = statuscode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
