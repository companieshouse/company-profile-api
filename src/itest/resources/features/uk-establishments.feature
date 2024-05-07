Feature: Process uk establishments

  Scenario Outline: Sending UK establishments GET request

    Given Company profile api service is running
    And UK establishments exists for parent company with number "<companyNumber>"
    When I send a GET request to retrieve UK establishments using company number "<companyNumber>"
    Then I should receive 200 status code
    And the GET call response body for the list of uk establishments should match "<result>"

    Examples:
    | companyNumber | result                       |
    | 00006401      | 00006401-getUkEstablishments |


  Scenario Outline: Sending UK establishments GET request without ERIC headers

  Given Company profile api service is running
  And a Company Profile exists for "<companyNumber>"
  When I send a GET request to retrieve UK establishments using company number "<companyNumber>" without Eric headers
  Then I should receive 401 status code

  Examples:
  | companyNumber |
  | 00006401      |


  Scenario Outline: Sending UK establishments GET request fails due to non existent company

  Given a company profile resource does not exist for "<company_number>"
  When I send a GET request to retrieve UK establishments using company number "<company_number>"
  Then the response code should be 404

  Examples:
  | company_number |
  | 00006401       |


  Scenario Outline: Sending UK establishments GET request fails due MongoDB being unavailable

  Given Company profile api service is running
  And the company profile database is down
  When I send a GET request to retrieve UK establishments using company number "<company_number>"
  Then I should receive 503 status code

  Examples:
  | company_number |
  | 00006401       |