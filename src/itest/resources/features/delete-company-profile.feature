Feature: Delete company profile

  Scenario Outline: Delete company profile successfully
    Given the CHS Kafka API is reachable
    And the company profile resource "<data_file>" exists for "<company_number>"
    When a DELETE request is sent to the company profile endpoint for "<company_number>"
    And the company profile does not exist for "<company_number>"
    Then I should receive 200 status code

    Examples:
      | data_file               | company_number |
      | with_links_resource     | 00006400       |

  Scenario Outline: Delete company profile successfully - company profile resource does not exist
    Given the CHS Kafka API is reachable
    And the company profile does not exist for "<company_number>"
    When a DELETE request is sent to the company profile endpoint for "<company_number>"
    Then I should receive 200 status code
    And the CHS Kafka API is invoked successfully

    Examples:
      | company_number |
      | 00006400       |
  
  Scenario Outline: Delete company profile unsuccessfully - user not authenticated
    When a DELETE request is sent to the company profile endpoint for "<company_number>" without valid ERIC headers
    Then the response code should be 401

    Examples:
      | company_number |
      | 00006400       |


  Scenario Outline: Delete company profile unsuccessfully - forbidden request
    When a DELETE request is sent to the company profile endpoint for "<company_number>" with insufficient access
    Then the response code should be 403

    Examples:
      | company_number |
      | 00006400       |

  Scenario Outline: Delete company profile unsuccessfully - stale delta
    Given the company profile resource "<data_file>" exists for "<company_number>"
    When a DELETE request is sent to the company profile endpoint for "<company_number>" with stale delta
    Then the response code should be 409

    Examples:
      | data_file           | company_number |
      | with_links_resource | 00006400       |

  Scenario Outline: Delete company profile unsuccessfully - delta at is blank
    When a DELETE request is sent to the company profile endpoint for "<company_number>" with blank delta at
    Then the response code should be 400

    Examples:
      | company_number |
      | 00006400       |

  Scenario Outline: Delete company profile unsuccessfully while database is down
    Given Company profile api service is running
    And a company profile resource does not exist for "<company_number>"
    And the company profile database is down
    When a DELETE request is sent to the company profile endpoint for "<company_number>"
    Then I should receive 503 status code
    And the CHS Kafka API is not invoked

    Examples:
      | company_number |
      | 00006400       |


  Scenario Outline: Delete company profile successfully when kafka-api is not available
    Given Company profile api service is running
    And the company profile resource "<data_file>" exists for "<company_number>"
    And CHS kafka API service is unavailable
    When a DELETE request is sent to the company profile endpoint for "<company_number>"
    Then I should receive 503 status code
    And the company profile does not exist for "<company_number>"

    Examples:
      | data_file               | company_number |
      | with_links_resource     | 00006400       |