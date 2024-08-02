package uk.gov.di.ipv.cri.address.api.handler.pact.states;

import au.com.dius.pact.provider.junitsupport.State;

public interface SingleAddressBuildingNameStates {
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

    @State("validFrom is 1887-01-01")
    default void validFromDate() {}
}
