package uk.gov.di.ipv.cri.address.api.domain.sharedclaims;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Name {

    @JsonProperty("nameParts")
    List<NameParts> nameParts;

    public List<NameParts> getNameParts() {
        return nameParts;
    }

    public void setNameParts(List<NameParts> nameParts) {
        this.nameParts = nameParts;
    }
}
