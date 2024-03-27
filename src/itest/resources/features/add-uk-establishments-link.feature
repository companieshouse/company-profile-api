Feature: Add uk-establishments link to company profile

  Scenario Outline: Add uk-establishments link successfully

    Given the CHS Kafka API is reachable
    And the company profile resource "<company_number>" exists for "<company_number>"
    And the company profile resource "<parent_company_number>" exists for "<parent_company_number>"
    And the uk-establishment link does not exist for "<parent_company_number>"
    When I send a PUT request with payload "<company_number>" file for company number "<company_number>"
    Then the response code should be 200
    And the uk-establishment link exists for "<parent_company_number>"

    Examples:
      | company_number | parent_company_number |
      | 00006400       | 00006401              |


  Scenario Outline: Add uk-establishments link unsuccessfully

    Given the CHS Kafka API is reachable
    And the company profile resource "<company_number>" exists for "<company_number>"
    And the company profile resource "<parent_company_number>" exists for "<parent_company_number>"
    And the uk-establishment link does exist for "<parent_company_number>"
    When I send a PUT request with payload "<company_number>" file for company number "<company_number>"
    Then the response code should be 409
    And the uk-establishment link exists for "<parent_company_number>"

    Examples:
      | company_number | parent_company_number |
      | 00006400       | 00006402              |