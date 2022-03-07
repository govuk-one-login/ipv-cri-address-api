package uk.gov.di.ipv.cri.address.library.models.ordinancesurvey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class OrdinanceSurveyPostcodeError {

    @SerializedName("error")
    @Expose
    private Error error;

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }
}
