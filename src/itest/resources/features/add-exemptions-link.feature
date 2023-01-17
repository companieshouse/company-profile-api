Feature: Add exemptions link to company profile

  Scenario Outline: Add exemptions link successfully

    Given the CHS Kafka API is reachable
    And   the company profile resource "<data_file>" exists for "<company_number>"
    And   the company profile resource for "<company_number>" does not already have an exemptions link
    When  a PATCH request is sent to the add exemptions link endpoint for "<company_number>"
    Then  the response code should be 200
    And   the exemptions link exists for "<company_number>"

    Examples:
      | data_file                  | company_number |
      | without_links_resource     | 00006400       |

  Scenario Outline: Add exemptions link unsuccessfully - company profile resource does not exist

    Given a company profile resource does not exist for "<company_number>"
    When  a PATCH request is sent to the add exemptions link endpoint for "<company_number>"
    Then  the response code should be 404

    Examples:
      | company_number |
      | 00006400       |

  Scenario Outline: Add exemptions link unsuccessfully - user not authenticated or authorised

    When  a PATCH request is sent to the add exemptions endpoint for "<company_number>" without ERIC headers
    Then  the response code should be 401

    Examples:
      | company_number |
      | 00006400       |

  Scenario Outline: Add exemptions link unsuccessfully - link already exists

    Given the company profile resource "<data_file>" exists for "<company_number>"
    And   the exemptions link exists for "<company_number>"
    When  a PATCH request is sent to the add exemptions link endpoint for "<company_number>"
    Then  the response code should be 409

    Examples:
      | data_file                | company_number |
      | with_links_resource      | 00006400       |

  Scenario Outline: Add exemptions link unsuccessfully - CHS Kafka API is unavailable

    Given CHS kafka API service is unavailable
    And   the company profile resource "<data_file>" exists for "<company_number>"
    When  a PATCH request is sent to the add exemptions link endpoint for "<company_number>"
    Then  the response code should be 503

    Examples:
      | data_file                  | company_number |
      | without_links_resource     | 00006400       |

  Scenario Outline: Add exemptions link unsuccessfully - the database is unavailable

    Given the company profile database is down
    When  a PATCH request is sent to the add exemptions link endpoint for "<company_number>"
    Then  the response code should be 503

    Examples:
      | company_number |
      | 00006400       |