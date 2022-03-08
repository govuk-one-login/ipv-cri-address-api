package uk.gov.di.ipv.cri.address.library.models.ordinancesurvey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Dpa {

    @SerializedName("UPRN")
    @Expose
    private String uprn;

    @SerializedName("UDPRN")
    @Expose
    private String udprn;

    @SerializedName("ADDRESS")
    @Expose
    private String address;

    @SerializedName("POST_TOWN")
    @Expose
    private String postTown;

    @SerializedName("POSTCODE")
    @Expose
    private String postcode;

    @SerializedName("PO_BOX_NUMBER")
    @Expose
    private String poBoxNumber;

    @SerializedName("RPC")
    @Expose
    private String rpc;

    @SerializedName("X_COORDINATE")
    @Expose
    private Double xCoordinate;

    @SerializedName("Y_COORDINATE")
    @Expose
    private Double yCoordinate;

    @SerializedName("STATUS")
    @Expose
    private String status;

    @SerializedName("DEPARTMENT_NAME")
    @Expose
    private String departmentName;

    @SerializedName("LOGICAL_STATUS_CODE")
    @Expose
    private String logicalStatusCode;

    @SerializedName("CLASSIFICATION_CODE")
    @Expose
    private String classificationCode;

    @SerializedName("CLASSIFICATION_CODE_DESCRIPTION")
    @Expose
    private String classificationCodeDescription;

    @SerializedName("LOCAL_CUSTODIAN_CODE")
    @Expose
    private Integer localCustodianCode;

    @SerializedName("LOCAL_CUSTODIAN_CODE_DESCRIPTION")
    @Expose
    private String localCustodianCodeDescription;

    @SerializedName("COUNTRY_CODE")
    @Expose
    private String countryCode;

    @SerializedName("COUNTRY_CODE_DESCRIPTION")
    @Expose
    private String countryCodeDescription;

    @SerializedName("POSTAL_ADDRESS_CODE")
    @Expose
    private String postalAddressCode;

    @SerializedName("POSTAL_ADDRESS_CODE_DESCRIPTION")
    @Expose
    private String postalAddressCodeDescription;

    @SerializedName("BLPU_STATE_CODE_DESCRIPTION")
    @Expose
    private String blpuStateCodeDescription;

    @SerializedName("TOPOGRAPHY_LAYER_TOID")
    @Expose
    private String topographyLayerToid;

    @SerializedName("PARENT_UPRN")
    @Expose
    private String parentUprn;

    @SerializedName("LAST_UPDATE_DATE")
    @Expose
    private String lastUpdateDate;

    @SerializedName("ENTRY_DATE")
    @Expose
    private String entryDate;

    @SerializedName("LANGUAGE")
    @Expose
    private String language;

    @SerializedName("MATCH")
    @Expose
    private Integer match;

    @SerializedName("MATCH_DESCRIPTION")
    @Expose
    private String matchDescription;

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
}
