package uk.gov.di.ipv.cri.address.api.handler.pact.states;

import au.com.dius.pact.provider.junitsupport.State;

public interface MultipleAddressStates {
    @State("Valid credential request for VC")
    default void validCredentialGenerated() {}

    @State("Invalid credential request")
    default void inValidCredentialGenerated() {}

    @State("buildingName is 221B")
    default void buildingName() {}

    @State("streetName is BAKER STREET")
    default void validStreetName() {}

    @State("postalCode is NW1 6XE")
    default void postCode() {}

    @State("addressLocality is LONDON")
    default void validAddressLocality() {}

    @State("validFrom is 1987-01-01")
    default void validFromDate() {}

    @State("second buildingName is 122")
    default void secondBuildingName() {}

    @State("second streetName is BURNS CRESCENT")
    default void secondValidStreetName() {}

    @State("second postalCode is EH1 9GP")
    default void secondPostCode() {}

    @State("second addressLocality is EDINBURGH")
    default void secondValidAddressLocality() {}

    @State("second validFrom is 2017-01-01")
    default void secondValidFromDate() {}
}
