package gov.uk.address.api.util;

import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;

import java.time.LocalDate;
import java.time.Year;

public class AddressContextMapper {
    public static CanonicalAddress mapToCanonicalAddress(AddressContext request) {
        CanonicalAddress canonicalAddress = new CanonicalAddress();

        canonicalAddress.setAddressRegion(request.getRegion());
        canonicalAddress.setAddressLocality(request.getLocality());
        canonicalAddress.setStreetName(request.getStreetName());
        canonicalAddress.setPostalCode(request.getPostcode());
        canonicalAddress.setBuildingNumber(request.getBuildingNumber());
        canonicalAddress.setBuildingName(request.getBuildingName());
        canonicalAddress.setSubBuildingName(request.getApartmentNumber());
        canonicalAddress.setValidFrom(yearFrom(request.getYearFrom()));
        canonicalAddress.setAddressCountry(request.getCountryCode());

        return canonicalAddress;
    }

    private static LocalDate yearFrom(int year) {
        return Year.of(year).atMonth(1).atDay(1);
    }
}
