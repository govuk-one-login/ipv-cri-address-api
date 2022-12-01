Feature: Invalid postcode test

  @invalid_postcode_test
  Scenario Outline: Invalid postcode
    Given user has the test-identity <testUserDataSheetRowNumber> in the form of a signed JWT string

    #Session
    When user sends a POST request to session end point
    Then user gets a session-id

    #Postcode Lookup
    When the user performs a postcode lookup for post code "<testPostCode>"
    Then user does not get any address

    Examples:
      | testUserDataSheetRowNumber | testPostCode |
      | 197                        | XX12 12XX    |
