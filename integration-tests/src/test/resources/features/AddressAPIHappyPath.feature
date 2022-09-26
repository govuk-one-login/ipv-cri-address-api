Feature: Address API happy path test
  @address_api_happy
  Scenario: Basic Address API journey
    Given user has the user identity in the form of a signed JWT string

    #Session
    When user sends a POST request to session end point
    Then user gets a session-id

    #Postcode Lookup
    When the user performs a postcode lookup
    Then user receives a list of addresses


