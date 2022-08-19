Feature: Error and retry scenarios for company profile

  Scenario Outline: Processing company profile information unsuccessfully

    Given Company profile api service is running
    When I send PATCH request with raw payload "<data>" and company number "<company_number>"
    Then I should receive <response_code> status code
    And the CHS Kafka API is not invoked
    And nothing is persisted in the database

    Examples:
      | data                               | company_number     | response_code |
      | bad_request_payload                | 11748564           | 400           |

  Scenario Outline: Processing company profile information unsuccessfully when payload field is null

    Given Company profile api service is running
    And the company links exists for "<company_number>"
    When I send PATCH request with payload "<data>" and company number "<company_number>"
    Then I should receive 500 status code

    Examples:
      | data                  | company_number     |
      | internal_server_error | 00006400           |


  Scenario Outline: Retrieve company profile unsuccessfully as the company number does not exist

    Given Company profile api service is running
    When I send GET request with company number "<data>"
    Then I should receive 404 status code

    Examples:
      | data     |
      | 00006400 |

  Scenario Outline: Processing company profile information while database is down

    Given Company profile api service is running
    And the company profile database is down
    When I send PATCH request with payload "<data>" and company number "<data>"
    Then the CHS Kafka API is not invoked
    And I should receive 503 status code

    Examples:
      | data     |
      | 11748564 |

  Scenario Outline: Getting company profile information while database is down

    Given Company profile api service is running
    And the company profile database is down
    When I send GET request with company number "<data>"
    Then I should receive 503 status code

    Examples:
      | data     |
      | 00006400 |

  Scenario Outline: Processing company profile information unsuccessfully and data is not persisted

    Given Company profile api service is running
    When CHS kafka API service is unavailable
    And the company links exists for "<data>"
    And I send PATCH request with payload "<data>" and company number "<data>" and CHS Kafka API unavailable
    Then I should receive 503 status code
    And save operation is not invoked

    Examples:
      | data     |
      | 11748564 |
