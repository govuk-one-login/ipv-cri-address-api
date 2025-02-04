Feature: Invalid postcode test

  @postcode_lookup_GET_test
  Scenario Outline: Postcode GET lookup
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

  @invalid_postcode_GET_test
  Scenario Outline: Invalid postcode GET lookup
    Given user has the test-identity <testUserDataSheetRowNumber> in the form of a signed JWT string

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # Postcode lookup
    When the user performs a postcode lookup for post code "<testPostCode>"
    Then user does not get any address

    Examples:
      | testUserDataSheetRowNumber | testPostCode |
      | 197                        | XX12 12XX    |