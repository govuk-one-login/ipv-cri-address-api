package uk.gov.di.ipv.cri.address.library.models.ordinancesurvey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Dpa {

    /** (Required) */
    @SerializedName("UPRN")
    @Expose
    private String uprn;
    /** (Required) */
    @SerializedName("UDPRN")
    @Expose
    private String udprn;
    /** (Required) */
    @SerializedName("ADDRESS")
    @Expose
    private String address;
    /** (Required) */
    @SerializedName("POST_TOWN")
    @Expose
    private String postTown;
    /** (Required) */
    @SerializedName("POSTCODE")
    @Expose
    private String postcode;

    @SerializedName("PO_BOX_NUMBER")
    @Expose
    private String poBoxNumber;
    /** (Required) */
    @SerializedName("RPC")
    @Expose
    private String rpc;
    /** (Required) */
    @SerializedName("X_COORDINATE")
    @Expose
    private Double xCoordinate;
    /** (Required) */
    @SerializedName("Y_COORDINATE")
    @Expose
    private Double yCoordinate;
    /** (Required) */
    @SerializedName("STATUS")
    @Expose
    private String status;
    /** (Required) */
    @SerializedName("DEPARTMENT_NAME")
    @Expose
    private String departmentName;

    @SerializedName("LOGICAL_STATUS_CODE")
    @Expose
    private String logicalStatusCode;
    /** (Required) */
    @SerializedName("CLASSIFICATION_CODE")
    @Expose
    private String classificationCode;
    /** (Required) */
    @SerializedName("CLASSIFICATION_CODE_DESCRIPTION")
    @Expose
    private String classificationCodeDescription;
    /** (Required) */
    @SerializedName("LOCAL_CUSTODIAN_CODE")
    @Expose
    private Integer localCustodianCode;
    /** (Required) */
    @SerializedName("LOCAL_CUSTODIAN_CODE_DESCRIPTION")
    @Expose
    private String localCustodianCodeDescription;
    /** (Required) */
    @SerializedName("COUNTRY_CODE")
    @Expose
    private String countryCode;
    /** (Required) */
    @SerializedName("COUNTRY_CODE_DESCRIPTION")
    @Expose
    private String countryCodeDescription;
    /** (Required) */
    @SerializedName("POSTAL_ADDRESS_CODE")
    @Expose
    private String postalAddressCode;
    /** (Required) */
    @SerializedName("POSTAL_ADDRESS_CODE_DESCRIPTION")
    @Expose
    private String postalAddressCodeDescription;
    /** (Required) */
    @SerializedName("BLPU_STATE_CODE_DESCRIPTION")
    @Expose
    private String blpuStateCodeDescription;
    /** (Required) */
    @SerializedName("TOPOGRAPHY_LAYER_TOID")
    @Expose
    private String topographyLayerToid;

    @SerializedName("PARENT_UPRN")
    @Expose
    private String parentUprn;
    /** (Required) */
    @SerializedName("LAST_UPDATE_DATE")
    @Expose
    private String lastUpdateDate;
    /** (Required) */
    @SerializedName("ENTRY_DATE")
    @Expose
    private String entryDate;
    /** (Required) */
    @SerializedName("LANGUAGE")
    @Expose
    private String language;
    /** (Required) */
    @SerializedName("MATCH")
    @Expose
    private Integer match;
    /** (Required) */
    @SerializedName("MATCH_DESCRIPTION")
    @Expose
    private String matchDescription;
    /** (Required) */
    @SerializedName("DELIVERY_POINT_SUFFIX")
    @Expose
    private String deliveryPointSuffix;

    @SerializedName("ORGANISATION_NAME")
    @Expose
    private String organisationName;

    @SerializedName("BUILDING_NUMBER")
    @Expose
    private String buildingNumber;

    @SerializedName("THOROUGHFARE_NAME")
    @Expose
    private String thoroughfareName;

    @SerializedName("BLPU_STATE_CODE")
    @Expose
    private String blpuStateCode;

    @SerializedName("BLPU_STATE_DATE")
    @Expose
    private String blpuStateDate;

    @SerializedName("BUILDING_NAME")
    @Expose
    private String buildingName;

    @SerializedName("SUB_BUILDING_NAME")
    @Expose
    private String subBuildingName;

    @SerializedName("DEPENDENT_THOROUGHFARE_NAME")
    @Expose
    private String dependentThoroughfareName;

    @SerializedName("DOUBLE_DEPENDENT_LOCALITY")
    @Expose
    private String doubleDependentLocality;

    @SerializedName("DEPENDENT_LOCALITY")
    @Expose
    private String dependentLocality;

    /** (Required) */
    public String getUprn() {
        return uprn;
    }

    /** (Required) */
    public void setUprn(String uprn) {
        this.uprn = uprn;
    }

    /** (Required) */
    public String getUdprn() {
        return udprn;
    }

    /** (Required) */
    public void setUdprn(String udprn) {
        this.udprn = udprn;
    }

    /** (Required) */
    public String getAddress() {
        return address;
    }

    /** (Required) */
    public void setAddress(String address) {
        this.address = address;
    }

    /** (Required) */
    public String getPostTown() {
        return postTown;
    }

    /** (Required) */
    public void setPostTown(String postTown) {
        this.postTown = postTown;
    }

    /** (Required) */
    public String getPostcode() {
        return postcode;
    }

    /** (Required) */
    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public String getPoBoxNumber() {
        return poBoxNumber;
    }

    public void setPoBoxNumber(String poBoxNumber) {
        this.poBoxNumber = poBoxNumber;
    }

    /** (Required) */
    public String getRpc() {
        return rpc;
    }

    /** (Required) */
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

    /** (Required) */
    public String getStatus() {
        return status;
    }

    /** (Required) */
    public void setStatus(String status) {
        this.status = status;
    }

    /** (Required) */
    public String getLogicalStatusCode() {
        return logicalStatusCode;
    }

    /** (Required) */
    public void setLogicalStatusCode(String logicalStatusCode) {
        this.logicalStatusCode = logicalStatusCode;
    }

    /** (Required) */
    public String getClassificationCode() {
        return classificationCode;
    }

    /** (Required) */
    public void setClassificationCode(String classificationCode) {
        this.classificationCode = classificationCode;
    }

    /** (Required) */
    public String getClassificationCodeDescription() {
        return classificationCodeDescription;
    }

    /** (Required) */
    public void setClassificationCodeDescription(String classificationCodeDescription) {
        this.classificationCodeDescription = classificationCodeDescription;
    }

    /** (Required) */
    public Integer getLocalCustodianCode() {
        return localCustodianCode;
    }

    /** (Required) */
    public void setLocalCustodianCode(Integer localCustodianCode) {
        this.localCustodianCode = localCustodianCode;
    }

    /** (Required) */
    public String getLocalCustodianCodeDescription() {
        return localCustodianCodeDescription;
    }

    /** (Required) */
    public void setLocalCustodianCodeDescription(String localCustodianCodeDescription) {
        this.localCustodianCodeDescription = localCustodianCodeDescription;
    }

    /** (Required) */
    public String getCountryCode() {
        return countryCode;
    }

    /** (Required) */
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    /** (Required) */
    public String getCountryCodeDescription() {
        return countryCodeDescription;
    }

    /** (Required) */
    public void setCountryCodeDescription(String countryCodeDescription) {
        this.countryCodeDescription = countryCodeDescription;
    }

    /** (Required) */
    public String getPostalAddressCode() {
        return postalAddressCode;
    }

    /** (Required) */
    public void setPostalAddressCode(String postalAddressCode) {
        this.postalAddressCode = postalAddressCode;
    }

    /** (Required) */
    public String getPostalAddressCodeDescription() {
        return postalAddressCodeDescription;
    }

    /** (Required) */
    public void setPostalAddressCodeDescription(String postalAddressCodeDescription) {
        this.postalAddressCodeDescription = postalAddressCodeDescription;
    }

    /** (Required) */
    public String getBlpuStateCodeDescription() {
        return blpuStateCodeDescription;
    }

    /** (Required) */
    public void setBlpuStateCodeDescription(String blpuStateCodeDescription) {
        this.blpuStateCodeDescription = blpuStateCodeDescription;
    }

    /** (Required) */
    public String getTopographyLayerToid() {
        return topographyLayerToid;
    }

    /** (Required) */
    public void setTopographyLayerToid(String topographyLayerToid) {
        this.topographyLayerToid = topographyLayerToid;
    }

    public String getParentUprn() {
        return parentUprn;
    }

    public void setParentUprn(String parentUprn) {
        this.parentUprn = parentUprn;
    }

    /** (Required) */
    public String getLastUpdateDate() {
        return lastUpdateDate;
    }

    /** (Required) */
    public void setLastUpdateDate(String lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    /** (Required) */
    public String getEntryDate() {
        return entryDate;
    }

    /** (Required) */
    public void setEntryDate(String entryDate) {
        this.entryDate = entryDate;
    }

    /** (Required) */
    public String getLanguage() {
        return language;
    }

    /** (Required) */
    public void setLanguage(String language) {
        this.language = language;
    }

    /** (Required) */
    public Integer getMatch() {
        return match;
    }

    /** (Required) */
    public void setMatch(Integer match) {
        this.match = match;
    }

    /** (Required) */
    public String getMatchDescription() {
        return matchDescription;
    }

    /** (Required) */
    public void setMatchDescription(String matchDescription) {
        this.matchDescription = matchDescription;
    }

    /** (Required) */
    public String getDeliveryPointSuffix() {
        return deliveryPointSuffix;
    }

    /** (Required) */
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
}
