Feature: Well-Known Jwks is accessible

  Scenario: /.well-known/jwks.json endpoint is accessible with initial set of keys
    Given that a public /.well-known/jwks.json endpoint exists for CRI
    When a request is made to fetch the public encryption keys
    Then the response from the endpoint includes the public JWK keyset
    And each key has an associated kid
    And at least one key use is "enc" for "encryption"
