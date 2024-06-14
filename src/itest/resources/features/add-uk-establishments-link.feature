Feature: Add uk-establishments link to company profile

  Scenario Outline: Add uk-establishments link successfully

    Given the CHS Kafka API is reachable
    And the company profile resource "<company_number>" exists for "<company_number>"
    And the company profile resource "<parent_company_number>" exists for "<parent_company_number>"
    And a UK establishment link does not exist for "<parent_company_number>"
    When I send a PUT request with payload "<company_number>" file for company number "<company_number>"
    Then the response code should be 200
    And a UK establishment link should be added for "<parent_company_number>"
    And an Overseas link should be added in "<company_number>" to "<parent_company_number>"

    Examples:
      | company_number | parent_company_number |
      | 00006400       | 00006401              |


  Scenario Outline: Add uk-establishments link unsuccessfully

    Given the CHS Kafka API is reachable
    And the company profile resource "<company_number>" exists for "<company_number>"
    And the company profile resource "<parent_company_number>" exists for "<parent_company_number>"
    And a UK establishment link does exist for "<parent_company_number>"
    When I send a PUT request with payload "<company_number>" file for company number "<company_number>"
    Then the response code should be 200
    And the UK establishment link should still exist for "<parent_company_number>"

    Examples:
      | company_number | parent_company_number |
      | 00006403       | 00006402              |

  Scenario Outline: Add uk-establishments link unsuccessfully - parent company does not exist

    Given the CHS Kafka API is reachable
    Given a company profile resource does not exist for "<parent_company_number>"
    And the company profile resource "<company_number>" exists for "<company_number>"
    When I send a PUT request with payload "<company_number>" file for company number "<company_number>"
    Then the response code should be 200
    And the UK establishment link should still exist for "<parent_company_number>"

    Examples:
      | company_number | parent_company_number |
      | 00006400       | 00006401              |