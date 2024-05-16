Feature: Add charges link to company profile

  Scenario Outline: Add charges link successfully

    Given the CHS Kafka API is reachable
    And   the company profile resource "<data_file>" exists for "<company_number>"
    And   the charges link does not exist for "<company_number>"
    When  a PATCH request is sent to the add charges link endpoint for "<company_number>"
    Then  the response code should be 200 for charges
    And   the charges link exists for "<company_number>"

    Examples:
      | data_file                  | company_number |
      | without_links_resource     | 00006400       |

  Scenario Outline: Add charges link unsuccessfully - company profile resource does not exist

    Given a company profile resource does not exist for "<company_number>"
    When  a PATCH request is sent to the add charges link endpoint for "<company_number>"
    Then  the response code should be 404 for charges

    Examples:
      | company_number |
      | 00006400       |

  Scenario Outline: Add charges link unsuccessfully - user not authenticated or authorised

    When  a PATCH request is sent to the add charges endpoint for "<company_number>" without ERIC headers
    Then  the response code should be 401 for charges

    Examples:
      | company_number |
      | 00006400       |

  Scenario Outline: Add charges link unsuccessfully - link already exists

    Given the company profile resource "<data_file>" exists for "<company_number>"
    And   the charges link exists for "<company_number>"
    When  a PATCH request is sent to the add charges link endpoint for "<company_number>"
    Then  the response code should be 409 for charges

    Examples:
      | data_file                | company_number |
      | with_links_resource      | 00006400       |

  Scenario Outline: Add charges link unsuccessfully - CHS Kafka API is unavailable

    Given CHS kafka API service is unavailable
    And   the company profile resource "<data_file>" exists for "<company_number>"
    When  a PATCH request is sent to the add charges link endpoint for "<company_number>"
    Then  the response code should be 503 for charges

    Examples:
      | data_file                  | company_number |
      | without_links_resource     | 00006400       |

  Scenario Outline: Add charges link unsuccessfully - the database is unavailable

    Given the company profile database is down
    When  a PATCH request is sent to the add charges link endpoint for "<company_number>"
    Then  the response code should be 503 for charges

    Examples:
      | company_number |
      | 00006400       |