package uk.gov.di.ipv.cri.address.api.handler.pact.states;

import au.com.dius.pact.provider.junitsupport.State;

public interface SingleAddressBuildingNumberStates {
    @State("Valid credential request for Experian VC")
    default void validCredentialGenerated() {}

    @State("Invalid credential request")
    default void inValidCredentialGenerated() {}

    @State("addressCountry is GB")
    default void validAddressCountry() {}

    @State("streetName is HADLEY ROAD")
    default void validStreetName() {}

    @State("buildingNumber is 8")
    default void validBuildingNumber() {}

    @State("addressLocality is BATH")
    default void validAddressLocality() {}

    @State("validFrom is 2000-01-01")
    default void validFromDate() {}
}
