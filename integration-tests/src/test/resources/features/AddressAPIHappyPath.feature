Feature: Address API happy path test

  @address_api_happy_with_header
  Scenario: Basic Address API journey with TXMA event header
    Given user has the test-identity 197 in the form of a signed JWT string

    # Session
    When user sends a POST request to session end point with txma header
    Then user gets a session-id

    # TXMA event
    When user sends a GET request to events end point
    And a valid START event is returned in the response with txma header
    Then START TxMA event is validated against schema

    # Addresses
    When the user arrives at find your address page
    Then enter your post code is pre-populated with response from /addresses

    # Postcode lookup
    When the user performs a postcode lookup for post code "SW1A 2AA"
    Then user receives a list of addresses containing "SW1A 2AA"

    # Address
    When the user selects address
    Then the address is saved successfully

    # Authorization
    When user sends a GET request to authorization end point
    And a valid authorization code is returned in the response

    # Access Token
    When user sends a POST request to token end point
    And a valid access token code is returned in the response

    #Credential issued
    When user sends a POST request to Credential Issue end point with a valid access token
    And a valid JWT is returned in the response

  @address_api_happy
  Scenario Outline: Basic Address API journey
    Given user has the test-identity <testUserDataSheetRowNumber> in the form of a signed JWT string

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # TXMA event
    When user sends a GET request to events end point
    And a valid START event is returned in the response without txma header
    Then START TxMA event is validated against schema

    # Addresses
    When the user arrives at find your address page
    Then enter your post code is pre-populated with response from /addresses

    # Postcode lookup
    When the user performs a postcode lookup for post code "<testPostCode>"
    Then user receives a list of addresses containing "<testPostCode>"

    # Address
    When the user selects address
    Then the address is saved successfully

    # Authorization
    When user sends a GET request to authorization end point
    And a valid authorization code is returned in the response

    # Access token
    When user sends a POST request to token end point
    And a valid access token code is returned in the response

    # Credential Issued
    When user sends a POST request to Credential Issue end point with a valid access token
    And a valid JWT is returned in the response

    Examples:
      | testUserDataSheetRowNumber | testPostCode |
      | 197                        | SW1A 2AA     |
      | 23                         | CA14 5PH     |
      | 1000                       | S62 5AB      |

  @international_address_api_happy
  Scenario: Temporary Country Code Only International Address API journey
    Given user has the test-identity 197 and context of "international_user" in the form of a signed JWT string
    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # TXMA event
    When user sends a GET request to events end point
    And a valid START event is returned in the response without txma header
    Then START TxMA event is validated against schema

    # Addresses
    When user requests lands on /addresses
    Then response should contain addresses and context from the personIdentityTable

    # Postcode lookup
    When the user performs a postcode lookup for post code "SW1A 2AA"
    Then user receives a list of addresses containing "SW1A 2AA"

    # Address
    When the user selects international address
    Then the address is saved successfully

    # Authorization
    When user sends a GET request to authorization end point
    And a valid authorization code is returned in the response

    # Access token
    When user sends a POST request to token end point
    And a valid access token code is returned in the response

    # Credential Issued
    When user sends a POST request to Credential Issue end point with a valid access token
    And a valid JWT is returned in the response
