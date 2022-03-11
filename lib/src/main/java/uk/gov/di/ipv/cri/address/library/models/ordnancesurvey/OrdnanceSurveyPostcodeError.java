package uk.gov.di.ipv.cri.address.library.models.ordnancesurvey;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrdnanceSurveyPostcodeError {

    @JsonProperty("error")
    private Error error;

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }
}
