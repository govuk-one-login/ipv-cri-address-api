package uk.gov.di.ipv.cri.address.library.models;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import uk.gov.di.ipv.cri.address.library.models.ordnancesurvey.Result;

import java.util.Date;
import java.util.Optional;

@DynamoDBDocument
public class CanonicalAddressWithResidency extends CanonicalAddress {
    private Date residentFrom;
    private Date residentTo;
    private Boolean currentResidency;

    public CanonicalAddressWithResidency() {
        super();
    }

    public CanonicalAddressWithResidency(Result result) {
        super(result);
    }

    public Optional<Date> getResidentFrom() {
        return Optional.ofNullable(residentFrom);
    }

    public void setResidentFrom(Date residentFrom) {
        this.residentFrom = residentFrom;
    }

    public Optional<Date> getResidentTo() {
        return Optional.ofNullable(residentTo);
    }

    public void setResidentTo(Date residentTo) {
        this.residentTo = residentTo;
    }

    public Optional<Boolean> getCurrentResidency() {
        return Optional.ofNullable(currentResidency);
    }

    public void setCurrentResidency(Boolean currentResidency) {
        this.currentResidency = currentResidency;
    }
}
