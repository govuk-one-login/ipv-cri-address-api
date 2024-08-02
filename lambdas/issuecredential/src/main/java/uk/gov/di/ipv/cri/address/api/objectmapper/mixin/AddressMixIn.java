package uk.gov.di.ipv.cri.address.api.objectmapper.mixin;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;

@JsonPropertyOrder({
    "addressCountry",
    "buildingName",
    "streetName",
    "postalCode",
    "buildingNumber",
    "addressLocality",
    "validFrom"
})
@ExcludeFromGeneratedCoverageReport
public abstract class AddressMixIn {}
