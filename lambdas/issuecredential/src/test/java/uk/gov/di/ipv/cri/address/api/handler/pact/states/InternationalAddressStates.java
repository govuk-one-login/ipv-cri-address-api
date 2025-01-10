package uk.gov.di.ipv.cri.address.api.handler.pact.states;

import au.com.dius.pact.provider.junitsupport.State;

public interface InternationalAddressStates {
    @State("Valid credential request for VC")
    default void validCredentialGenerated() {}

    @State("addressCountry is CD")
    default void addressCountry() {}

    @State("addressRegion is North Kivu")
    default void addressRegion() {}

    @State("buildingName is Immeuble Commercial Plaza")
    default void buildingName() {}

    @State("buildingNumber is 4")
    default void buildingNumber() {}

    @State("subBuildingName is 3")
    default void subBuildingName() {}

    @State("streetName is Boulevard Kanyamuhanga")
    default void streetName() {}

    @State("postalCode is 243")
    default void postalCode() {}

    @State("addressLocality is Goma")
    default void addressLocality() {}

    @State("validFrom is 2020-01-01")
    default void validFromDate() {}
}
