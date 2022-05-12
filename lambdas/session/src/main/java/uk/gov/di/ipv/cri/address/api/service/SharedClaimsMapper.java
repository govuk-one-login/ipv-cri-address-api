package uk.gov.di.ipv.cri.address.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.address.api.domain.sharedclaims.SharedClaims;
import uk.gov.di.ipv.cri.address.library.domain.CanonicalAddress;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressType;
import uk.gov.di.ipv.cri.address.library.persistence.item.PersonAddress;
import uk.gov.di.ipv.cri.address.library.persistence.item.PersonIdentity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class SharedClaimsMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedClaimsMapper.class);

    PersonIdentity mapToPersonIdentity(SharedClaims sharedClaims) {
        PersonIdentity identity = new PersonIdentity();
        if (notNullAndNotEmpty(sharedClaims.getBirthDate())) {
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
            Date dob = null;
            try {
                dob = formatter.parse(sharedClaims.getBirthDate().get(0).getValue());
            } catch (ParseException e) {
                LOGGER.error("Failed to convert date from string", e);
            }
            identity.setDateOfBirth(dob);
        }
        if (notNullAndNotEmpty(sharedClaims.getNames())) {
            identity.setFirstName(sharedClaims.getNames().get(0).getNameParts().get(0).getValue());
            identity.setSurname(sharedClaims.getNames().get(0).getNameParts().get(1).getValue());
        }
        if (notNullAndNotEmpty(sharedClaims.getAddresses())) {
            identity.setAddresses(mapAddresses(sharedClaims.getAddresses()));
        }
        return identity;
    }

    private <T> boolean notNullAndNotEmpty(List<T> items) {
        return Objects.nonNull(items) && !items.isEmpty();
    }

    private List<PersonAddress> mapAddresses(List<CanonicalAddress> addresses) {
        return addresses.stream()
                .map(
                        address -> {
                            PersonAddress personAddress = new PersonAddress();
                            personAddress.setHouseNumber(address.getBuildingNumber());
                            personAddress.setStreet(address.getStreetName());
                            personAddress.setTownCity(address.getAddressLocality());
                            personAddress.setPostcode(address.getPostalCode());
                            personAddress.setAddressType(getAddressType(address));
                            return personAddress;
                        })
                .collect(Collectors.toList());
    }

    private AddressType getAddressType(CanonicalAddress address) {
        Date dateNow = new Date();
        boolean validFromInThePast = address.getValidFrom().orElse(dateNow).compareTo(dateNow) < 0;
        boolean validUntilInThePast = address.getValidFrom().orElse(dateNow).compareTo(dateNow) < 0;

        if (validFromInThePast && validUntilInThePast) {
            return AddressType.PREVIOUS;
        }
        if (validFromInThePast) {
            return AddressType.CURRENT;
        }
        return null;
    }
}
