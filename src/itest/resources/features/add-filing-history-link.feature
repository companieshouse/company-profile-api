Feature: Add filing history link to company profile

  Scenario Outline: Add filing history link successfully

    Given the CHS Kafka API is reachable
    And   the company profile resource "<data_file>" exists for "<company_number>"
    And   the filing history link does not exist for "<company_number>"
    When  a PATCH request is sent to the add filing history link endpoint for "<company_number>"
    Then  the response code should be 200 for filing history
    And   the filing history link exists for "<company_number>"

    Examples:
      | data_file              | company_number |
      | without_links_resource | 00006400       |

  Scenario Outline: Add filing history link unsuccessfully - company profile resource does not exist

    Given a company profile resource does not exist for "<company_number>"
    When  a PATCH request is sent to the add filing history link endpoint for "<company_number>"
    Then  the response code should be 404 for filing history

    Examples:
      | company_number |
      | 00006400       |

  Scenario Outline: Add filing history link unsuccessfully - user not authenticated or authorised

    When  a PATCH request is sent to the add filing history endpoint for "<company_number>" without ERIC headers
    Then  the response code should be 401 for filing history

    Examples:
      | company_number |
      | 00006400       |

  Scenario Outline: Add filing history link unsuccessfully - link already exists

    Given the company profile resource "<data_file>" exists for "<company_number>"
    And   the filing history link exists for "<company_number>"
    When  a PATCH request is sent to the add filing history link endpoint for "<company_number>"
    Then  the response code should be 409 for filing history

    Examples:
      | data_file           | company_number |
      | with_links_resource | 00006400       |

  Scenario Outline: Add filing history link unsuccessfully - CHS Kafka API is unavailable

    Given CHS kafka API service is unavailable
    And   the company profile resource "<data_file>" exists for "<company_number>"
    When  a PATCH request is sent to the add filing history link endpoint for "<company_number>"
    Then  the response code should be 503 for filing history

    Examples:
      | data_file              | company_number |
      | without_links_resource | 00006400       |

  Scenario Outline: Add filing history link unsuccessfully - the database is unavailable

    Given the company profile database is down
    When  a PATCH request is sent to the add filing history link endpoint for "<company_number>"
    Then  the response code should be 503 for filing history

    Examples:
      | company_number |
      | 00006400       |