Feature: Invalid postcode test

  @invalid_postcode_test
  Scenario Outline: Invalid postcode
    Given user has a default signed JWT

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # Postcode lookup
    When the user performs a postcode lookup for post code "<testPostCode>"
    Then user does not get any address

    Examples:
   | testPostCode |
  | XX12 12XX    |
