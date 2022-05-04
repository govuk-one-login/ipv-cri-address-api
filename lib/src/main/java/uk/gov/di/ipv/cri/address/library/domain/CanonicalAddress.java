package uk.gov.di.ipv.cri.address.library.domain;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;
import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DynamoDBDocument
public class CanonicalAddress {
    private Long uprn;
    private String organisationName;
    private String departmentName;
    private String subBuildingName;
    private String buildingNumber;
    private String buildingName;

    @JsonAlias("dependentThoroughfare")
    private String dependentStreetName;

    @JsonAlias("thoroughfareName")
    private String streetName;

    @JsonAlias("doubleDependentLocality")
    private String doubleDependentAddressLocality;

    @JsonAlias("dependentLocality")
    private String dependentAddressLocality;

    @JsonAlias("postTown")
    private String addressLocality;

    @JsonAlias("postcode")
    private String postalCode;

    @JsonAlias("countryCode")
    private String addressCountry;

    @JsonAlias("residentFrom")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date validFrom;

    @JsonAlias("residentTo")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date validUntil;

    public CanonicalAddress() {
        // Default constructor
    }

    @DynamoDBAttribute(attributeName = "UPRN")
    public Optional<Long> getUprn() {
        return Optional.ofNullable(this.uprn);
    }

    public void setUprn(Long uprn) {
        this.uprn = uprn;
    }

    @DynamoDBAttribute(attributeName = "OrganisationName")
    public String getOrganisationName() {
        return organisationName;
    }

    public void setOrganisationName(String organisationName) {
        this.organisationName = organisationName;
    }

    @DynamoDBAttribute(attributeName = "DepartmentName")
    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    @DynamoDBAttribute(attributeName = "SubBuildingName")
    public String getSubBuildingName() {
        return subBuildingName;
    }

    public void setSubBuildingName(String subBuildingName) {
        this.subBuildingName = subBuildingName;
    }

    @DynamoDBAttribute(attributeName = "BuildingNumber")
    public String getBuildingNumber() {
        return buildingNumber;
    }

    public void setBuildingNumber(String buildingNumber) {
        this.buildingNumber = buildingNumber;
    }

    @DynamoDBAttribute(attributeName = "DependentStreetName")
    public String getDependentStreetName() {
        return dependentStreetName;
    }

    public void setDependentStreetName(String dependentStreetName) {
        this.dependentStreetName = dependentStreetName;
    }

    @DynamoDBAttribute(attributeName = "DoubleDependentAddressLocality")
    public String getDoubleDependentAddressLocality() {
        return doubleDependentAddressLocality;
    }

    public void setDoubleDependentAddressLocality(String doubleDependentAddressLocality) {
        this.doubleDependentAddressLocality = doubleDependentAddressLocality;
    }

    @DynamoDBAttribute(attributeName = "DependentAddressLocality")
    public String getDependentAddressLocality() {
        return dependentAddressLocality;
    }

    public void setDependentAddressLocality(String dependentAddressLocality) {
        this.dependentAddressLocality = dependentAddressLocality;
    }

    @DynamoDBAttribute(attributeName = "BuildingName")
    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    @DynamoDBAttribute(attributeName = "StreetName")
    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    @DynamoDBAttribute(attributeName = "AddressLocality")
    public String getAddressLocality() {
        return addressLocality;
    }

    public void setAddressLocality(String addressLocality) {
        this.addressLocality = addressLocality;
    }

    @DynamoDBAttribute(attributeName = "PostalCode")
    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    @DynamoDBAttribute(attributeName = "AddressCountry")
    public String getAddressCountry() {
        return addressCountry;
    }

    public void setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
    }

    public Optional<Date> getValidFrom() {
        return Optional.ofNullable(validFrom);
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Optional<Date> getValidUntil() {
        return Optional.ofNullable(validUntil);
    }

    public void setValidUntil(Date validUntil) {
        this.validUntil = validUntil;
    }
}
