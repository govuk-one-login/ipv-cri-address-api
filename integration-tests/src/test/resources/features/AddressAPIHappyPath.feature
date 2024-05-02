Feature: Address API happy path test

  @address_api_happy
  Scenario Outline: Basic Address API journey
    Given user has the test-identity <testUserDataSheetRowNumber> in the form of a signed JWT string

    #Session
    When user sends a POST request to session end point
    Then user gets a session-id

    #Postcode Lookup
    When the user performs a postcode lookup for post code "<testPostCode>"
    Then user receives a list of addresses containing "<testPostCode>"

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

    #Assert on TXMA event
    Then TXMA event is added to the sqs queue
#    And header value is not present in the message

    Examples:
      | testUserDataSheetRowNumber | testPostCode |
      | 197                        | SW1A 2AA     |
#      | 23                         | CA14 5PH     |
#      | 1000                       | S62 5AB      |

@address_api_happy_with_header
  Scenario Outline: Basic Address API journey with TXMA event header
    Given user has the test-identity <testUserDataSheetRowNumber> in the form of a signed JWT string

    #Session
    When user sends a POST request to session end point with txma header
    Then user gets a session-id

    #Postcode Lookup
    When the user performs a postcode lookup for post code "<testPostCode>"
    Then user receives a list of addresses containing "<testPostCode>"

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

    #Assert on TXMA event
    Then TXMA event is added to the sqs queue
#    And header value is present in the message

    Examples:
      | testUserDataSheetRowNumber | testPostCode |
      | 197                        | SW1A 2AA     |