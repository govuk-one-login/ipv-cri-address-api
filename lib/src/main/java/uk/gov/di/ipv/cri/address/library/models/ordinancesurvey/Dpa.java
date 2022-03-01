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

    @SerializedName("BUILDING_NUMBER")
    @Expose
    private String buildingNumber;

    @SerializedName("THOROUGHFARE_NAME")
    @Expose
    private String thoroughfareName;

    @SerializedName("POST_TOWN")
    @Expose
    private String postTown;

    @SerializedName("POSTCODE")
    @Expose
    private String postcode;

    @SerializedName("RPC")
    @Expose
    private String rpc;

    @SerializedName("X_COORDINATE")
    @Expose
    private Float xCoordinate;

    @SerializedName("Y_COORDINATE")
    @Expose
    private Float yCoordinate;

    @SerializedName("STATUS")
    @Expose
    private String status;

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

    @SerializedName("BLPU_STATE_CODE")
    @Expose
    private String blpuStateCode;

    @SerializedName("BLPU_STATE_CODE_DESCRIPTION")
    @Expose
    private String blpuStateCodeDescription;

    @SerializedName("TOPOGRAPHY_LAYER_TOID")
    @Expose
    private String topographyLayerToid;

    @SerializedName("LAST_UPDATE_DATE")
    @Expose
    private String lastUpdateDate;

    @SerializedName("ENTRY_DATE")
    @Expose
    private String entryDate;

    @SerializedName("BLPU_STATE_DATE")
    @Expose
    private String blpuStateDate;

    @SerializedName("LANGUAGE")
    @Expose
    private String language;

    @SerializedName("MATCH")
    @Expose
    private Float match;

    @SerializedName("MATCH_DESCRIPTION")
    @Expose
    private String matchDescription;

    @SerializedName("DELIVERY_POINT_SUFFIX")
    @Expose
    private String deliveryPointSuffix;

    @SerializedName("BUILDING_NAME")
    @Expose
    private String buildingName;

    public String getUprn() {
        return uprn;
    }

    public void setUprn(String uprn) {
        this.uprn = uprn;
    }

    public Dpa withUprn(String uprn) {
        this.uprn = uprn;
        return this;
    }

    public String getUdprn() {
        return udprn;
    }

    public void setUdprn(String udprn) {
        this.udprn = udprn;
    }

    public Dpa withUdprn(String udprn) {
        this.udprn = udprn;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Dpa withAddress(String address) {
        this.address = address;
        return this;
    }

    public String getBuildingNumber() {
        return buildingNumber;
    }

    public void setBuildingNumber(String buildingNumber) {
        this.buildingNumber = buildingNumber;
    }

    public Dpa withBuildingNumber(String buildingNumber) {
        this.buildingNumber = buildingNumber;
        return this;
    }

    public String getThoroughfareName() {
        return thoroughfareName;
    }

    public void setThoroughfareName(String thoroughfareName) {
        this.thoroughfareName = thoroughfareName;
    }

    public Dpa withThoroughfareName(String thoroughfareName) {
        this.thoroughfareName = thoroughfareName;
        return this;
    }

    public String getPostTown() {
        return postTown;
    }

    public void setPostTown(String postTown) {
        this.postTown = postTown;
    }

    public Dpa withPostTown(String postTown) {
        this.postTown = postTown;
        return this;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public Dpa withPostcode(String postcode) {
        this.postcode = postcode;
        return this;
    }

    public String getRpc() {
        return rpc;
    }

    public void setRpc(String rpc) {
        this.rpc = rpc;
    }

    public Dpa withRpc(String rpc) {
        this.rpc = rpc;
        return this;
    }

    public Float getXCoordinate() {
        return xCoordinate;
    }

    public void setXCoordinate(Float xCoordinate) {
        this.xCoordinate = xCoordinate;
    }

    public Dpa withXCoordinate(Float xCoordinate) {
        this.xCoordinate = xCoordinate;
        return this;
    }

    public Float getYCoordinate() {
        return yCoordinate;
    }

    public void setYCoordinate(Float yCoordinate) {
        this.yCoordinate = yCoordinate;
    }

    public Dpa withYCoordinate(Float yCoordinate) {
        this.yCoordinate = yCoordinate;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Dpa withStatus(String status) {
        this.status = status;
        return this;
    }

    public String getLogicalStatusCode() {
        return logicalStatusCode;
    }

    public void setLogicalStatusCode(String logicalStatusCode) {
        this.logicalStatusCode = logicalStatusCode;
    }

    public Dpa withLogicalStatusCode(String logicalStatusCode) {
        this.logicalStatusCode = logicalStatusCode;
        return this;
    }

    public String getClassificationCode() {
        return classificationCode;
    }

    public void setClassificationCode(String classificationCode) {
        this.classificationCode = classificationCode;
    }

    public Dpa withClassificationCode(String classificationCode) {
        this.classificationCode = classificationCode;
        return this;
    }

    public String getClassificationCodeDescription() {
        return classificationCodeDescription;
    }

    public void setClassificationCodeDescription(String classificationCodeDescription) {
        this.classificationCodeDescription = classificationCodeDescription;
    }

    public Dpa withClassificationCodeDescription(String classificationCodeDescription) {
        this.classificationCodeDescription = classificationCodeDescription;
        return this;
    }

    public Integer getLocalCustodianCode() {
        return localCustodianCode;
    }

    public void setLocalCustodianCode(Integer localCustodianCode) {
        this.localCustodianCode = localCustodianCode;
    }

    public Dpa withLocalCustodianCode(Integer localCustodianCode) {
        this.localCustodianCode = localCustodianCode;
        return this;
    }

    public String getLocalCustodianCodeDescription() {
        return localCustodianCodeDescription;
    }

    public void setLocalCustodianCodeDescription(String localCustodianCodeDescription) {
        this.localCustodianCodeDescription = localCustodianCodeDescription;
    }

    public Dpa withLocalCustodianCodeDescription(String localCustodianCodeDescription) {
        this.localCustodianCodeDescription = localCustodianCodeDescription;
        return this;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public Dpa withCountryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public String getCountryCodeDescription() {
        return countryCodeDescription;
    }

    public void setCountryCodeDescription(String countryCodeDescription) {
        this.countryCodeDescription = countryCodeDescription;
    }

    public Dpa withCountryCodeDescription(String countryCodeDescription) {
        this.countryCodeDescription = countryCodeDescription;
        return this;
    }

    public String getPostalAddressCode() {
        return postalAddressCode;
    }

    public void setPostalAddressCode(String postalAddressCode) {
        this.postalAddressCode = postalAddressCode;
    }

    public Dpa withPostalAddressCode(String postalAddressCode) {
        this.postalAddressCode = postalAddressCode;
        return this;
    }

    public String getPostalAddressCodeDescription() {
        return postalAddressCodeDescription;
    }

    public void setPostalAddressCodeDescription(String postalAddressCodeDescription) {
        this.postalAddressCodeDescription = postalAddressCodeDescription;
    }

    public Dpa withPostalAddressCodeDescription(String postalAddressCodeDescription) {
        this.postalAddressCodeDescription = postalAddressCodeDescription;
        return this;
    }

    public String getBlpuStateCode() {
        return blpuStateCode;
    }

    public void setBlpuStateCode(String blpuStateCode) {
        this.blpuStateCode = blpuStateCode;
    }

    public Dpa withBlpuStateCode(String blpuStateCode) {
        this.blpuStateCode = blpuStateCode;
        return this;
    }

    public String getBlpuStateCodeDescription() {
        return blpuStateCodeDescription;
    }

    public void setBlpuStateCodeDescription(String blpuStateCodeDescription) {
        this.blpuStateCodeDescription = blpuStateCodeDescription;
    }

    public Dpa withBlpuStateCodeDescription(String blpuStateCodeDescription) {
        this.blpuStateCodeDescription = blpuStateCodeDescription;
        return this;
    }

    public String getTopographyLayerToid() {
        return topographyLayerToid;
    }

    public void setTopographyLayerToid(String topographyLayerToid) {
        this.topographyLayerToid = topographyLayerToid;
    }

    public Dpa withTopographyLayerToid(String topographyLayerToid) {
        this.topographyLayerToid = topographyLayerToid;
        return this;
    }

    public String getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(String lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public Dpa withLastUpdateDate(String lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
        return this;
    }

    public String getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(String entryDate) {
        this.entryDate = entryDate;
    }

    public Dpa withEntryDate(String entryDate) {
        this.entryDate = entryDate;
        return this;
    }

    public String getBlpuStateDate() {
        return blpuStateDate;
    }

    public void setBlpuStateDate(String blpuStateDate) {
        this.blpuStateDate = blpuStateDate;
    }

    public Dpa withBlpuStateDate(String blpuStateDate) {
        this.blpuStateDate = blpuStateDate;
        return this;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Dpa withLanguage(String language) {
        this.language = language;
        return this;
    }

    public Float getMatch() {
        return match;
    }

    public void setMatch(Float match) {
        this.match = match;
    }

    public Dpa withMatch(Float match) {
        this.match = match;
        return this;
    }

    public String getMatchDescription() {
        return matchDescription;
    }

    public void setMatchDescription(String matchDescription) {
        this.matchDescription = matchDescription;
    }

    public Dpa withMatchDescription(String matchDescription) {
        this.matchDescription = matchDescription;
        return this;
    }

    public String getDeliveryPointSuffix() {
        return deliveryPointSuffix;
    }

    public void setDeliveryPointSuffix(String deliveryPointSuffix) {
        this.deliveryPointSuffix = deliveryPointSuffix;
    }

    public Dpa withDeliveryPointSuffix(String deliveryPointSuffix) {
        this.deliveryPointSuffix = deliveryPointSuffix;
        return this;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public Dpa withBuildingName(String buildingName) {
        this.buildingName = buildingName;
        return this;
    }
}
