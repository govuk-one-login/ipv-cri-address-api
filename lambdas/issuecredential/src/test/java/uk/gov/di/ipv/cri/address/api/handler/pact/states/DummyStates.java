package uk.gov.di.ipv.cri.address.api.handler.pact.states;

import au.com.dius.pact.provider.junitsupport.State;

public interface DummyStates {
    @State("dummyApiKey is a valid api key")
    default void validDummyApiKey() {}

    @State("dummyAddressComponentId is a valid issuer")
    default void validDummyAddressComponent() {}

    @State("test-subject is a valid subject")
    default void validSubject() {}
}
