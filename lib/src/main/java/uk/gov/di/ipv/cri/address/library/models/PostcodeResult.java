package uk.gov.di.ipv.cri.address.library.models;

import uk.gov.di.ipv.cri.address.library.models.ordinancesurvey.Result;

public class PostcodeResult {
    private String address;

    public PostcodeResult(Result result) {
        this.address = result.getDpa().getAddress();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
