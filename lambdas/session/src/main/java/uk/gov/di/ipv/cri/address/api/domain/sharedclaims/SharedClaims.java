package uk.gov.di.ipv.cri.address.api.domain.sharedclaims;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.di.ipv.cri.address.library.domain.CanonicalAddress;

import java.util.List;

public class SharedClaims {
    @JsonProperty("name")
    private List<Name> names;

    @JsonProperty("birthDate")
    private List<BirthDate> birthDate;

    @JsonProperty("@context")
    private List<String> context;

    @JsonProperty("addresses")
    private List<CanonicalAddress> addresses;

    public List<Name> getNames() {
        return names;
    }

    public void setNames(List<Name> names) {
        this.names = names;
    }

    public List<BirthDate> getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(List<BirthDate> birthDate) {
        this.birthDate = birthDate;
    }

    public List<String> getContext() {
        return context;
    }

    public void setContext(List<String> context) {
        this.context = context;
    }

    public List<CanonicalAddress> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<CanonicalAddress> addresses) {
        this.addresses = addresses;
    }
}
