package uk.gov.di.ipv.cri.address.library.models;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.fasterxml.jackson.annotation.JsonInclude;
import uk.gov.di.ipv.cri.address.library.models.ordnancesurvey.Result;

import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DynamoDBDocument
public class CanonicalAddress {
    private Optional<Integer> uprn;
    private String organisationName;
    private String departmentName;
    private String subBuildingName;
    private String buildingNumber;
    private String dependentThoroughfare;
    private String doubleDependentLocality;
    private String dependentLocality;
    private String buildingName;
    private String thoroughfareName;
    private String postTown;
    private String postcode;
    private String countryCode;

    public CanonicalAddress(Result result) {
        var dpa = result.getDpa();
        if (dpa.getUprn() != null && !dpa.getUprn().isEmpty()) {
            this.uprn = Optional.of(Integer.parseInt(dpa.getUprn()));
        } else {
            this.uprn = Optional.empty();
        }
        this.organisationName = dpa.getOrganisationName();
        this.departmentName = dpa.getDepartmentName();
        this.subBuildingName = dpa.getSubBuildingName();
        this.buildingNumber = dpa.getBuildingNumber();
        this.dependentThoroughfare = dpa.getDependentThoroughfareName();
        this.doubleDependentLocality = dpa.getDoubleDependentLocality();
        this.dependentLocality = dpa.getDependentLocality();
        this.buildingName = dpa.getBuildingName();
        this.thoroughfareName = dpa.getThoroughfareName();
        this.postTown = dpa.getPostTown();
        this.postcode = dpa.getPostcode();
        this.countryCode =
                "GBR"; // All addresses returned by this service MUST be within the United Kingdom
    }

    public CanonicalAddress() {
        // Default constructor
    }

    @DynamoDBAttribute(attributeName = "UPRN")
    public Optional<Integer> getUprn() {
        return uprn;
    }

    public void setUprn(Optional<Integer> uprn) {
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

    @DynamoDBAttribute(attributeName = "DependentThoroughfare")
    public String getDependentThoroughfare() {
        return dependentThoroughfare;
    }

    public void setDependentThoroughfare(String dependentThoroughfare) {
        this.dependentThoroughfare = dependentThoroughfare;
    }

    @DynamoDBAttribute(attributeName = "DoubleDependentLocality")
    public String getDoubleDependentLocality() {
        return doubleDependentLocality;
    }

    public void setDoubleDependentLocality(String doubleDependentLocality) {
        this.doubleDependentLocality = doubleDependentLocality;
    }

    @DynamoDBAttribute(attributeName = "DependentLocality")
    public String getDependentLocality() {
        return dependentLocality;
    }

    public void setDependentLocality(String dependentLocality) {
        this.dependentLocality = dependentLocality;
    }

    @DynamoDBAttribute(attributeName = "BuildingName")
    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    @DynamoDBAttribute(attributeName = "ThoroughfareName")
    public String getThoroughfareName() {
        return thoroughfareName;
    }

    public void setThoroughfareName(String thoroughfareName) {
        this.thoroughfareName = thoroughfareName;
    }

    @DynamoDBAttribute(attributeName = "PostTown")
    public String getPostTown() {
        return postTown;
    }

    public void setPostTown(String postTown) {
        this.postTown = postTown;
    }

    @DynamoDBAttribute(attributeName = "Postcode")
    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    @DynamoDBAttribute(attributeName = "CountryCode")
    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
