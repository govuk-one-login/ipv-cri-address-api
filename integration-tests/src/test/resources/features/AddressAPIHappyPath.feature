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

    #Address
    When the user selects address
    Then the address is saved successfully

    #Authorization
    When user sends a GET request to authorization end point
    And a valid authorization code is returned in the response

    #Access Token
    When user sends a POST request to token end point
    And a valid access token code is returned in the response

    #Credential Issue
    When user sends a POST request to Credential Issue end point with a valid access token
    And a valid JWT is returned in the response

    #Get_Addresses
    When user sends a GET request to Addresses end point
    Then user should receive a valid response

