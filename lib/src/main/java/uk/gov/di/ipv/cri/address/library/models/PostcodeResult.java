package uk.gov.di.ipv.cri.address.library.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import uk.gov.di.ipv.cri.address.library.models.ordnancesurvey.Result;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostcodeResult {
    private final String uprn;
    private final String organisationName;
    private final String departmentName;
    private final String subBuildingName;
    private final String buildingNumber;
    private final String dependentThoroughfare;
    private final String doubleDependentLocality;
    private final String dependentLocality;
    private final String buildingName;
    private final String thoroughfareName;
    private final String postTown;
    private final String postcode;
    private final String countryCode;

    public PostcodeResult(Result result) {
        var dpa = result.getDpa();
        this.uprn = dpa.getUprn();
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

    public String getUprn() {
        return uprn;
    }

    public String getOrganisationName() {
        return organisationName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getSubBuildingName() {
        return subBuildingName;
    }

    public String getBuildingNumber() {
        return buildingNumber;
    }

    public String getDependentThoroughfare() {
        return dependentThoroughfare;
    }

    public String getDoubleDependentLocality() {
        return doubleDependentLocality;
    }

    public String getDependentLocality() {
        return dependentLocality;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public String getThoroughfareName() {
        return thoroughfareName;
    }

    public String getPostTown() {
        return postTown;
    }

    public String getPostcode() {
        return postcode;
    }

    public String getCountryCode() {
        return countryCode;
    }
}
