Feature: Add officers link to company profile

  Scenario Outline: Add officers link successfully

    Given the CHS Kafka API is reachable
    And   the company profile resource "<data_file>" exists for "<company_number>"
    And   the officers link does not exist for "<company_number>"
    When  a PATCH request is sent to the add officers link endpoint for "<company_number>"
    Then  the response code should be 200
    And   the officers link exists for "<company_number>"

    Examples:
      | data_file                  | company_number |
      | without_links_resource     | 00006400       |

  Scenario Outline: Add officers link unsuccessfully - company profile resource does not exist

    Given a company profile resource does not exist for "<company_number>"
    When  a PATCH request is sent to the add officers link endpoint for "<company_number>"
    Then  the response code should be 404

    Examples:
      | company_number |
      | 00006400       |

  Scenario Outline: Add officers link unsuccessfully - user not authenticated or authorised

    When  a PATCH request is sent to the add officers endpoint for "<company_number>" without ERIC headers
    Then  the response code should be 401

    Examples:
      | company_number |
      | 00006400       |

  Scenario Outline: Add officers link unsuccessfully - link already exists

    Given the company profile resource "<data_file>" exists for "<company_number>"
    And   the officers link exists for "<company_number>"
    When  a PATCH request is sent to the add officers link endpoint for "<company_number>"
    Then  the response code should be 409

    Examples:
      | data_file                | company_number |
      | with_links_resource      | 00006400       |

  @Ignored
    #    Scenario does not work correctly due to potential issue with API Client library and Apache Client 5 dependency
  Scenario Outline: Add officers link unsuccessfully - CHS Kafka API is unavailable

    Given CHS kafka API service is unavailable
    And   the company profile resource "<data_file>" exists for "<company_number>"
    When  a PATCH request is sent to the add officers link endpoint for "<company_number>"
    Then  the response code should be 503

    Examples:
      | data_file                  | company_number |
      | without_links_resource     | 00006400       |

  Scenario Outline: Add officers link unsuccessfully - the database is unavailable

    Given the company profile database is down
    When  a PATCH request is sent to the add officers link endpoint for "<company_number>"
    Then  the response code should be 503

    Examples:
      | company_number |
      | 00006400       |