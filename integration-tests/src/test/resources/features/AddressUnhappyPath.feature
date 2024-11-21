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
