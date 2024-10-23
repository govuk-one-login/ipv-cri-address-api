package uk.gov.di.ipv.cri.address.api.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;

import java.time.LocalDate;
import java.util.Date;
import java.util.Objects;

public class VCAddress {
    private Long uprn;
    private String organisationName;
    private String departmentName;
    private String subBuildingName;
    private String buildingNumber;
    private String buildingName;
    private String dependentStreetName;
    private String streetName;
    private String doubleDependentAddressLocality;
    private String dependentAddressLocality;
    private String addressLocality;
    private String postalCode;
    private String addressCountry;
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy"
    )
    private Date validFrom;
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy"
    )
    private Date validUntil;

    public VCAddress(CanonicalAddress canonicalAddress) {
        this.uprn = canonicalAddress.getUprn();
        this.organisationName = canonicalAddress.getOrganisationName();
        this.departmentName = canonicalAddress.getDepartmentName();
        this.subBuildingName = canonicalAddress.getSubBuildingName();
        this.buildingName = canonicalAddress.getBuildingName();
        this.buildingNumber = canonicalAddress.getBuildingNumber();
        this.dependentStreetName = canonicalAddress.getDependentStreetName();
        this.streetName = canonicalAddress.getStreetName();
        this.doubleDependentAddressLocality = canonicalAddress.getDoubleDependentAddressLocality();
        this.addressLocality = canonicalAddress.getAddressLocality();
        this.dependentAddressLocality = canonicalAddress.getDependentAddressLocality();
        this.postalCode = canonicalAddress.getPostalCode();
        this.addressCountry = canonicalAddress.getAddressCountry();
        if(canonicalAddress.getValidFrom() != null) {
            this.validFrom = java.sql.Date.valueOf(canonicalAddress.getValidFrom());
        }
        if(canonicalAddress.getValidUntil() != null) {
            this.validUntil = java.sql.Date.valueOf(canonicalAddress.getValidUntil());
        }
    }

    public Long getUprn() {
        return this.uprn;
    }

    public void setUprn(Long uprn) {
        this.uprn = uprn;
    }

    public String getOrganisationName() {
        return this.organisationName;
    }

    public void setOrganisationName(String organisationName) {
        this.organisationName = organisationName;
    }

    public String getDepartmentName() {
        return this.departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getSubBuildingName() {
        return this.subBuildingName;
    }

    public void setSubBuildingName(String subBuildingName) {
        this.subBuildingName = subBuildingName;
    }

    public String getBuildingNumber() {
        return this.buildingNumber;
    }

    public void setBuildingNumber(String buildingNumber) {
        this.buildingNumber = buildingNumber;
    }

    public String getDependentStreetName() {
        return this.dependentStreetName;
    }

    public void setDependentStreetName(String dependentStreetName) {
        this.dependentStreetName = dependentStreetName;
    }

    public String getDoubleDependentAddressLocality() {
        return this.doubleDependentAddressLocality;
    }

    public void setDoubleDependentAddressLocality(String doubleDependentAddressLocality) {
        this.doubleDependentAddressLocality = doubleDependentAddressLocality;
    }

    public String getDependentAddressLocality() {
        return this.dependentAddressLocality;
    }

    public void setDependentAddressLocality(String dependentAddressLocality) {
        this.dependentAddressLocality = dependentAddressLocality;
    }

    public String getBuildingName() {
        return this.buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getStreetName() {
        return this.streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public String getAddressLocality() {
        return this.addressLocality;
    }

    public void setAddressLocality(String addressLocality) {
        this.addressLocality = addressLocality;
    }

    public String getPostalCode() {
        return this.postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getAddressCountry() {
        return this.addressCountry;
    }

    public void setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
    }

    public LocalDate getValidFrom() {
        return Objects.nonNull(this.validFrom) ? (new java.sql.Date(this.validFrom.getTime())).toLocalDate() : null;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = Objects.nonNull(validFrom) ? java.sql.Date.valueOf(validFrom) : null;
    }

    public LocalDate getValidUntil() {
        return Objects.nonNull(this.validUntil) ? (new java.sql.Date(this.validUntil.getTime())).toLocalDate() : null;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = Objects.nonNull(validUntil) ? java.sql.Date.valueOf(validUntil) : null;
    }
}
