Feature: Process company details

  Scenario Outline: Get Company details when sending get request

    Given Company profile api service is running
    And a Company Profile exists for "<companyNumber>"
    When I send GET request to retrieve Company details using company number "<companyNumber>"
    Then I should receive 200 status code
    And the Get call response body should match "<result>" file for company details

    Examples:
      | companyNumber         | result                            |
      | 00006402              | 00006402-getCompanyDetail         |

  Scenario Outline: Get Company details when sending get request without Eric headers

    Given Company profile api service is running
    And a Company Profile exists for "<companyNumber>"
    When I send GET request to retrieve Company details using company number "<companyNumber>" without setting Eric headers
    Then I should receive 401 status code

    Examples:
      | companyNumber     |
      | 00006402          |


  Scenario Outline: get company details unsuccessfully - company profile resource does not exist
    Given a company profile resource does not exist for "<company_number>"
    When I send GET request to retrieve Company details using company number "<company_number>"
    Then the response code should be 404

    Examples:
      | company_number |
      | 00006400       |


  Scenario Outline: Getting company details information while database is down

    Given Company profile api service is running
    And the company profile database is down
    When I send GET request to retrieve Company details using company number "<company_number>"
    Then I should receive 503 status code

    Examples:
      | company_number     |
      | 00006400           |