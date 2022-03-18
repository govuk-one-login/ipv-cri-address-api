package uk.gov.di.ipv.cri.address.library.models;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import uk.gov.di.ipv.cri.address.library.models.ordnancesurvey.Result;

import java.util.Date;
import java.util.Optional;

@DynamoDBDocument
public class CanonicalAddressWithResidency extends CanonicalAddress {
    private Optional<Date> residentFrom;
    private Optional<Date> residentTo;
    private Optional<Boolean> currentResidency;

    public CanonicalAddressWithResidency() {
        super();
    }

    public CanonicalAddressWithResidency(Result result) {
        super(result);
    }

    public Optional<Date> getResidentFrom() {
        return residentFrom;
    }

    public void setResidentFrom(Optional<Date> residentFrom) {
        this.residentFrom = residentFrom;
    }

    public Optional<Date> getResidentTo() {
        return residentTo;
    }

    public void setResidentTo(Optional<Date> residentTo) {
        this.residentTo = residentTo;
    }

    public Optional<Boolean> getCurrentResidency() {
        return currentResidency;
    }

    public void setCurrentResidency(Optional<Boolean> currentResidency) {
        this.currentResidency = currentResidency;
    }
}
