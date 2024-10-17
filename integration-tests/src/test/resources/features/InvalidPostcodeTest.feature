Feature: Invalid postcode test

  @invalid_postcode_test
  Scenario Outline: Invalid postcode
    Given user has the test-identity <testUserDataSheetRowNumber> in the form of a signed JWT string

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # Postcode lookup
    When the user performs a postcode lookup for post code "<testPostCode>"
    Then user does not get any address

    # TxMA events
    And 3 events are deleted from the audit events SQS queue

    Examples:
      | testUserDataSheetRowNumber | testPostCode |
      | 197                        | XX12 12XX    |

  @no_country_code
  Scenario: No country code send for address
    Given user has the test-identity 197 in the form of a signed JWT string

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # TXMA event
    Then TXMA event is added to the SQS queue not containing device information header

    # Postcode lookup
    When the user performs a postcode lookup for post code "SW1A 2AA"
    Then user receives a list of addresses containing "SW1A 2AA"

    # Address
    When the user selects address without country code
    Then the address is not saved

    # TxMA events
    And 3 events are deleted from the audit events SQS queue