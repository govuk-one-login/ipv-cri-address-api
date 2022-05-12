package uk.gov.di.ipv.cri.address.library.persistence.item;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;

import java.time.LocalDate;

@DynamoDBDocument
public class PersonAddress {
    private String houseNumber;

    private String houseName;

    private String flat;

    // @NotBlank(message = "{personAddress.street.required}")
    private String street;

    // @NotBlank(message = "{personAddress.townCity.required}")
    private String townCity;

    // @NotBlank(message = "{personAddress.postcode.required}")
    private String postcode;

    private String district;

    // @NotNull(message = "{personAddress.addressType.required}")
    private AddressType addressType;

    private LocalDate dateMovedOut;

    public String getHouseNumber() {
        return houseNumber;
    }

    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }

    public String getHouseName() {
        return houseName;
    }

    public void setHouseName(String houseName) {
        this.houseName = houseName;
    }

    public String getFlat() {
        return flat;
    }

    public void setFlat(String flat) {
        this.flat = flat;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getTownCity() {
        return townCity;
    }

    public void setTownCity(String townCity) {
        this.townCity = townCity;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public AddressType getAddressType() {
        return addressType;
    }

    public void setAddressType(AddressType addressType) {
        this.addressType = addressType;
    }

    public LocalDate getDateMovedOut() {
        return dateMovedOut;
    }

    public void setDateMovedOut(LocalDate dateMovedOut) {
        this.dateMovedOut = dateMovedOut;
    }
}
