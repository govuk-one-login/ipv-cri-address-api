Feature: Address API happy path test

  @address_api_happy_with_header
  Scenario: Basic Address API journey with TXMA event header
    Given user has a default signed JWT

    # Session
    When user sends a POST request to session end point with txma header
    Then user gets a session-id

    # TXMA event
    When user sends a GET request to events end point for "IPV_ADDRESS_CRI_START"
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
    When user sends a POST request to token end point with "https://review-a.dev.account.gov.uk"
    And a valid access token code is returned in the response

    #Credential issued
    When user sends a POST request to Credential Issue end point with a valid access token
    And a valid JWT is returned in the response

    # TXMA event
    When user sends a GET request to events end point for "IPV_ADDRESS_CRI_VC_ISSUED"
    Then VC_ISSUED TxMA event is validated against schema with isUkAddress "true"

  @address_api_happy
  Scenario Outline: Basic Address API journey
    Given user has an overridden signed JWT using "<sharedClaims>"

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # TXMA event
    When user sends a GET request to events end point for "IPV_ADDRESS_CRI_START"
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
    When user sends a POST request to token end point with "https://review-a.dev.account.gov.uk"
    And a valid access token code is returned in the response

    # Credential Issued
    When user sends a POST request to Credential Issue end point with a valid access token
    And a valid JWT is returned in the response

    # TXMA event
    When user sends a GET request to events end point for "IPV_ADDRESS_CRI_VC_ISSUED"
    Then VC_ISSUED TxMA event is validated against schema with isUkAddress "true"

    Examples:
      | sharedClaims       | testPostCode |
      | ALBERT_AKRIL.json  | CA14 5PH     |
      | SUZIE_SHREEVE.json | S62 5AB      |

  @international_address_api_happy_with_header
  Scenario: International Address API journey
    Given user has an overridden signed JWT using "INTERNATIONAL.json"

    # Session
    When user sends a POST request to session end point with txma header
    Then user gets a session-id

    # TXMA event
    When user sends a GET request to events end point for "IPV_ADDRESS_CRI_START"
    And a valid START event is returned in the response with txma header
    Then START TxMA event is validated against schema

    # Addresses
    When user requests lands on /addresses
    Then response should contain addresses and context from the personIdentityTable

    # Address
    When the user enters international address successfully
      | Field           | Value                  |
      | apartmentNumber | 4                      |
      | buildingNumber  | 10                     |
      | buildingName    | Kilimanjaro Apartments |
      | streetName      | Ngong Road             |
      | country         | KE                     |
      | region          | Nairobi County         |
      | locality        | Nairobi                |
      | postalCode      | 00100                  |
      | yearFrom        | 2020                   |

    Then the address is saved successfully

    # Authorization
    When user sends a GET request to authorization end point
    And a valid authorization code is returned in the response

    # Access token
    When user sends a POST request to token end point with "https://review-a.dev.account.gov.uk"
    And a valid access token code is returned in the response

    # Credential Issued
    When user sends a POST request to Credential Issue end point with a valid access token
    And a valid JWT is returned in the response

    # TXMA event
    When user sends a GET request to events end point for "IPV_ADDRESS_CRI_VC_ISSUED"
    Then VC_ISSUED TxMA event is validated against schema with isUkAddress "false"

  @international_address_api_happy
  Scenario: International Address API journey
    Given user has an overridden signed JWT using "INTERNATIONAL.json"
    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # TXMA event
    When user sends a GET request to events end point for "IPV_ADDRESS_CRI_START"
    And a valid START event is returned in the response without txma header
    Then START TxMA event is validated against schema

    # Addresses
    When user requests lands on /addresses
    Then response should contain addresses and context from the personIdentityTable

    # Address
    When the user enters international address successfully
      | Field           | Value                  |
      | apartmentNumber | 4                      |
      | buildingNumber  | 10                     |
      | buildingName    | Kilimanjaro Apartments |
      | streetName      | Ngong Road             |
      | country         | KE                     |
      | region          | Nairobi County         |
      | locality        | Nairobi                |
      | postalCode      | 00100                  |
      | yearFrom        | 2020                   |

    Then the address is saved successfully

    # Authorization
    When user sends a GET request to authorization end point
    And a valid authorization code is returned in the response

    # Access token
    When user sends a POST request to token end point with "https://review-a.dev.account.gov.uk"
    And a valid access token code is returned in the response

    # Credential Issued
    When user sends a POST request to Credential Issue end point with a valid access token
    And a valid JWT is returned in the response

    # TXMA event
    When user sends a GET request to events end point for "IPV_ADDRESS_CRI_VC_ISSUED"
    Then VC_ISSUED TxMA event is validated against schema with isUkAddress "false"

  @multiple_addresses_api_happy_with_header
  Scenario: Multiple Addresses API journey with TXMA event header
    Given user has a default signed JWT

    # Session
    When user sends a POST request to session end point with txma header
    Then user gets a session-id

    # TXMA event
    When user sends a GET request to events end point for "IPV_ADDRESS_CRI_START"
    And a valid START event is returned in the response with txma header
    Then START TxMA event is validated against schema

    # Addresses
    When the user arrives at find your address page
    Then enter your post code is pre-populated with response from /addresses

    # Postcode lookup for previous address
    When the user performs a postcode lookup for post code "SW1A 2AA"
    Then user receives a list of addresses containing "SW1A 2AA"

    # Address for previous address
    When the user selects previous address
    Then the address is saved successfully

    # Authorization
    When user sends a GET request to authorization end point
    And a valid authorization code is returned in the response

    # Access Token
    When user sends a POST request to token end point with "https://review-a.dev.account.gov.uk"
    And a valid access token code is returned in the response

    #Credential issued
    When user sends a POST request to Credential Issue end point with a valid access token
    And a valid JWT is returned in the multiple addresses response

    # TXMA event
    When user sends a GET request to events end point for "IPV_ADDRESS_CRI_VC_ISSUED"
    Then VC_ISSUED TxMA event is validated against schema with isUkAddress "true"

    # schema validation
    Then user sends a GET request to events end point for "IPV_ADDRESS_CRI_END"
    Then the IPV_ADDRESS_CRI_END event is emitted and validated against schema