Feature: Delete uk-establishment link in company profile

  Scenario Outline: Delete uk establishments link successfully

    Given the CHS Kafka API is reachable
    And the company profile resource "<company_number>" exists for "<company_number>"
    And the company profile resource "<parent_company_number>" exists for "<parent_company_number>"
    And the uk-establishment link exists for "<parent_company_number>"
    When a DELETE request is sent to the company profile endpoint for "<company_number>"
    Then the response code should be 200
    And the uk-establishment link does not exist for "<parent_company_number>"

    Examples:
      | company_number | parent_company_number |
      | 00006400       | 00006402              |

  Scenario Outline: Delete uk-establishments link unsuccessfully

    Given the CHS Kafka API is reachable
    And the company profile resource "<company_number>" exists for "<company_number>"
    And the company profile resource "<parent_company_number>" exists for "<parent_company_number>"
    And the uk-establishment link does not exist for "<parent_company_number>"
    When a DELETE request is sent to the company profile endpoint for "<company_number>"
    Then the response code should be 200
    And the uk-establishment link does not exist for "<parent_company_number>"

    Examples:
      | company_number | parent_company_number |
      | 00006400       | 00006401              |

  Scenario Outline: Delete uk-establishments link unsuccessfully - parent company has multiple children

    Given the CHS Kafka API is reachable
    And the company profile resource "<parent_company_number>" exists for "<parent_company_number>"
    And the company profile resource "<company_number>" exists for "<company_number>"
    When a DELETE request is sent to the company profile endpoint for "<company_number>"
    Then the response code should be 200
    And the uk-establishment link does exist for "<parent_company_number>"


    Examples:
      | company_number | parent_company_number |
      | 00006400       | 00006408              |