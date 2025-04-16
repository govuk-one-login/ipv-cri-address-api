Feature: valid postcode test

  @postcode_lookup_test
  Scenario Outline: Postcode lookup
    Given user has the test-identity <testUserDataSheetRowNumber> in the form of a signed JWT string

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # Postcode lookup
    When the user performs a postcode lookup for post code "<testPostCode>"
    Then user receives a list of addresses containing "<testPostCode>"

    Examples:
      | testUserDataSheetRowNumber | testPostCode |
      | 197                        | SW1A 2AA     |

  @postcode_lookup_on_removed_get_request
  Scenario Outline: postcode lookup with get a bad Request
    Given user has the test-identity <testUserDataSheetRowNumber> in the form of a signed JWT string

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # Postcode lookup
    When the user performs a GET postcode lookup for post code "<testPostCode>"
    Then responds with missing authentication

    Examples:
      | testUserDataSheetRowNumber | testPostCode |
      | 197                        | SW1A 2AA     |

  @postcode-lookup
  Scenario Outline: Rate limit Is returned by OS API?
    Given user has the test-identity <testUserDataSheetRowNumber> in the form of a signed JWT string
    When user sends a POST request to session end point
    And the user performs a postcode lookup for post code "<testPostCode>"
    And the response HTTP status code is 404
    Then the response body is Error processing postcode lookup

    Examples:
      | testUserDataSheetRowNumber | testPostCode |
      | 197                        | E11 3BW      |
