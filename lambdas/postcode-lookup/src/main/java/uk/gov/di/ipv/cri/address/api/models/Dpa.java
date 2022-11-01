package uk.gov.di.ipv.cri.address.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Dpa {

    @JsonProperty("UPRN")
    private String uprn;

    @JsonProperty("UDPRN")
    private String udprn;

    @JsonProperty("ADDRESS")
    private String address;

    @JsonProperty("POST_TOWN")
    private String postTown;

    @JsonProperty("POSTCODE")
    private String postcode;

    @JsonProperty("PO_BOX_NUMBER")
    private String poBoxNumber;

    @JsonProperty("RPC")
    private String rpc;

    @JsonProperty("X_COORDINATE")
    private Double xCoordinate;

    @JsonProperty("Y_COORDINATE")
    private Double yCoordinate;

    @JsonProperty("STATUS")
    private String status;

    @JsonProperty("DEPARTMENT_NAME")
    private String departmentName;

    @JsonProperty("LOGICAL_STATUS_CODE")
    private String logicalStatusCode;

    @JsonProperty("CLASSIFICATION_CODE")
    private String classificationCode;

    @JsonProperty("CLASSIFICATION_CODE_DESCRIPTION")
    private String classificationCodeDescription;

    @JsonProperty("LOCAL_CUSTODIAN_CODE")
    private Integer localCustodianCode;

    @JsonProperty("LOCAL_CUSTODIAN_CODE_DESCRIPTION")
    private String localCustodianCodeDescription;

    @JsonProperty("COUNTRY_CODE")
    private String countryCode;

    @JsonProperty("COUNTRY_CODE_DESCRIPTION")
    private String countryCodeDescription;

    @JsonProperty("POSTAL_ADDRESS_CODE")
    private String postalAddressCode;

    @JsonProperty("POSTAL_ADDRESS_CODE_DESCRIPTION")
    private String postalAddressCodeDescription;

    @JsonProperty("BLPU_STATE_CODE_DESCRIPTION")
    private String blpuStateCodeDescription;

    @JsonProperty("TOPOGRAPHY_LAYER_TOID")
    private String topographyLayerToid;

    @JsonProperty("PARENT_UPRN")
    private String parentUprn;

    @JsonProperty("LAST_UPDATE_DATE")
    private String lastUpdateDate;

    @JsonProperty("ENTRY_DATE")
    private String entryDate;

    @JsonProperty("LANGUAGE")
    private String language;

    @JsonProperty("MATCH")
    private Integer match;

    @JsonProperty("MATCH_DESCRIPTION")
    private String matchDescription;

    @JsonProperty("DELIVERY_POINT_SUFFIX")
    private String deliveryPointSuffix;

    @JsonProperty("ORGANISATION_NAME")
    private String organisationName;

    @JsonProperty("BUILDING_NUMBER")
    private String buildingNumber;

    @JsonProperty("THOROUGHFARE_NAME")
    private String thoroughfareName;

    @JsonProperty("BLPU_STATE_CODE")
    private String blpuStateCode;

    @JsonProperty("BLPU_STATE_DATE")
    private String blpuStateDate;

    @JsonProperty("BUILDING_NAME")
    private String buildingName;

    @JsonProperty("SUB_BUILDING_NAME")
    private String subBuildingName;

    @JsonProperty("DEPENDENT_THOROUGHFARE_NAME")
    private String dependentThoroughfareName;

    @JsonProperty("DOUBLE_DEPENDENT_LOCALITY")
    private String doubleDependentLocality;

    @JsonProperty("DEPENDENT_LOCALITY")
    private String dependentLocality;

    public String getUprn() {
        return uprn;
    }

    public void setUprn(String uprn) {
        this.uprn = uprn;
    }

    public String getUdprn() {
        return udprn;
    }

    public void setUdprn(String udprn) {
        this.udprn = udprn;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPostTown() {
        return postTown;
    }

    public void setPostTown(String postTown) {
        this.postTown = postTown;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public String getPoBoxNumber() {
        return poBoxNumber;
    }

    public void setPoBoxNumber(String poBoxNumber) {
        this.poBoxNumber = poBoxNumber;
    }

    public String getRpc() {
        return rpc;
    }

    public void setRpc(String rpc) {
        this.rpc = rpc;
    }

    public Double getXCoordinate() {
        return xCoordinate;
    }

    public void setXCoordinate(Double xCoordinate) {
        this.xCoordinate = xCoordinate;
    }

    public Double getYCoordinate() {
        return yCoordinate;
    }

    public void setYCoordinate(Double yCoordinate) {
        this.yCoordinate = yCoordinate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLogicalStatusCode() {
        return logicalStatusCode;
    }

    public void setLogicalStatusCode(String logicalStatusCode) {
        this.logicalStatusCode = logicalStatusCode;
    }

    public String getClassificationCode() {
        return classificationCode;
    }

    public void setClassificationCode(String classificationCode) {
        this.classificationCode = classificationCode;
    }

    public String getClassificationCodeDescription() {
        return classificationCodeDescription;
    }

    public void setClassificationCodeDescription(String classificationCodeDescription) {
        this.classificationCodeDescription = classificationCodeDescription;
    }

    public Integer getLocalCustodianCode() {
        return localCustodianCode;
    }

    public void setLocalCustodianCode(Integer localCustodianCode) {
        this.localCustodianCode = localCustodianCode;
    }

    public String getLocalCustodianCodeDescription() {
        return localCustodianCodeDescription;
    }

    public void setLocalCustodianCodeDescription(String localCustodianCodeDescription) {
        this.localCustodianCodeDescription = localCustodianCodeDescription;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryCodeDescription() {
        return countryCodeDescription;
    }

    public void setCountryCodeDescription(String countryCodeDescription) {
        this.countryCodeDescription = countryCodeDescription;
    }

    public String getPostalAddressCode() {
        return postalAddressCode;
    }

    public void setPostalAddressCode(String postalAddressCode) {
        this.postalAddressCode = postalAddressCode;
    }

    public String getPostalAddressCodeDescription() {
        return postalAddressCodeDescription;
    }

    public void setPostalAddressCodeDescription(String postalAddressCodeDescription) {
        this.postalAddressCodeDescription = postalAddressCodeDescription;
    }

    public String getBlpuStateCodeDescription() {
        return blpuStateCodeDescription;
    }

    public void setBlpuStateCodeDescription(String blpuStateCodeDescription) {
        this.blpuStateCodeDescription = blpuStateCodeDescription;
    }

    public String getTopographyLayerToid() {
        return topographyLayerToid;
    }

    public void setTopographyLayerToid(String topographyLayerToid) {
        this.topographyLayerToid = topographyLayerToid;
    }

    public String getParentUprn() {
        return parentUprn;
    }

    public void setParentUprn(String parentUprn) {
        this.parentUprn = parentUprn;
    }

    public String getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(String lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public String getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(String entryDate) {
        this.entryDate = entryDate;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getMatch() {
        return match;
    }

    public void setMatch(Integer match) {
        this.match = match;
    }

    public String getMatchDescription() {
        return matchDescription;
    }

    public void setMatchDescription(String matchDescription) {
        this.matchDescription = matchDescription;
    }

    public String getDeliveryPointSuffix() {
        return deliveryPointSuffix;
    }

    public void setDeliveryPointSuffix(String deliveryPointSuffix) {
        this.deliveryPointSuffix = deliveryPointSuffix;
    }

    public String getOrganisationName() {
        return organisationName;
    }

    public void setOrganisationName(String organisationName) {
        this.organisationName = organisationName;
    }

    public String getBuildingNumber() {
        return buildingNumber;
    }

    public void setBuildingNumber(String buildingNumber) {
        this.buildingNumber = buildingNumber;
    }

    public String getThoroughfareName() {
        return thoroughfareName;
    }

    public void setThoroughfareName(String thoroughfareName) {
        this.thoroughfareName = thoroughfareName;
    }

    public String getBlpuStateCode() {
        return blpuStateCode;
    }

    public void setBlpuStateCode(String blpuStateCode) {
        this.blpuStateCode = blpuStateCode;
    }

    public String getBlpuStateDate() {
        return blpuStateDate;
    }

    public void setBlpuStateDate(String blpuStateDate) {
        this.blpuStateDate = blpuStateDate;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getSubBuildingName() {
        return subBuildingName;
    }

    public void setSubBuildingName(String subBuildingName) {
        this.subBuildingName = subBuildingName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDependentThoroughfareName() {
        return dependentThoroughfareName;
    }

    public void setDependentThoroughfareName(String dependentThoroughfareName) {
        this.dependentThoroughfareName = dependentThoroughfareName;
    }

    public String getDoubleDependentLocality() {
        return doubleDependentLocality;
    }

    public void setDoubleDependentLocality(String doubleDependentLocality) {
        this.doubleDependentLocality = doubleDependentLocality;
    }

    public String getDependentLocality() {
        return dependentLocality;
    }

    public void setDependentLocality(String dependentLocality) {
        this.dependentLocality = dependentLocality;
    }

    public CanonicalAddress toCanonicalAddress() {
        CanonicalAddress canonicalAddress = new CanonicalAddress();

        if (this.getUprn() != null && !this.getUprn().isEmpty()) {
            canonicalAddress.setUprn(Long.parseLong(this.getUprn()));
        }
        canonicalAddress.setOrganisationName(this.getOrganisationName());
        canonicalAddress.setDepartmentName(this.getDepartmentName());
        canonicalAddress.setSubBuildingName(this.getSubBuildingName());
        canonicalAddress.setBuildingNumber(this.getBuildingNumber());
        canonicalAddress.setDependentStreetName(this.getDependentThoroughfareName());
        canonicalAddress.setDoubleDependentAddressLocality(this.getDoubleDependentLocality());
        canonicalAddress.setDependentAddressLocality(this.getDependentLocality());
        canonicalAddress.setBuildingName(this.getBuildingName());
        canonicalAddress.setStreetName(this.getThoroughfareName());
        canonicalAddress.setAddressLocality(this.getPostTown());
        canonicalAddress.setPostalCode(this.getPostcode());
        // All addresses returned by this service can only possibly
        // exist within the United Kingdom, so set to GB
        canonicalAddress.setAddressCountry("GB");
        return canonicalAddress;
    }
}
