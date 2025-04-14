Feature: Address API unhappy path test

  @no_country_code
  Scenario: No country code sent for address
    Given user has the test-identity 197 in the form of a signed JWT string

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # TXMA event
    When user sends a GET request to events end point for "IPV_ADDRESS_CRI_START"
    And a valid START event is returned in the response without txma header

    # Postcode lookup
    When the user performs a postcode lookup for post code "SW1A 2AA"
    Then user receives a list of addresses containing "SW1A 2AA"

    # Address
    When the user selects address without country code
    Then the address is not saved


  @no_session_id
  Scenario: No session_id header
    Given a request is made to the addresses endpoint and it does not include a session_id header
    Then the endpoint should return a 400 HTTP status code
    And the response body contains no session id error

  @with_postcode @with_no_session_id
  Scenario: With postcode and no session_id in the request body
    Given a request is made to the postcode-lookup endpoint with postcode in the request body with no session id header
    Then the endpoint should return a 400 HTTP status code
    And the response body contains no session id error

  @no_postcode @with_session_id
  Scenario: No postcode and with session_id in the request body
    Given user has the test-identity 197 in the form of a signed JWT string
    When user sends a POST request to session end point
    Then user gets a session-id
    Given a request is made to the postcode-lookup endpoint without a postcode in the body and with session id in the header
    Then the endpoint should return a 400 HTTP status code
    And the response body contains no postcode error
