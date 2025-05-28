Feature: CRI Decrypt and Verify JAR Request

  Scenario: Headless Core Stub encrypts requests to /session using the key returned by /well-known/jwks.json endpoint
    Given that a public /.well-known/jwks.json endpoint exists for a CRI
    When a request is made to fetch the public encryption keys
    And the feature flag is "enabled"
    Then the core stub forms a request for the CRI's /session endpoint
    And the core stub makes a call to the CRI's /well-known/jwks.json endpoint
    Then the request by the core stub is verified by the CRI

  Scenario: Headless Core Stub does NOT encrypt requests to /session using the key returned by /well-known/jwks.json endpoint
    Given that a public /.well-known/jwks.json endpoint exists for a CRI
    When a request is made to fetch the public encryption keys
    And the feature flag is "disabled"
    Then the core stub forms a request for the CRI's /session endpoint
    And the core stub does NOT make a call to the CRI's /well-known/jwks.json endpoint
    But the request by the core stub is NOT verified by the CRI