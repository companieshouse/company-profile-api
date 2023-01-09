Feature: Add officers link to company profile

  Scenario Outline: Add officers link successfully

    Given CHS Kafka API is available
    And   the company profile resource "<data_file>" exists for "<company_number>"
    And   the company profile resource for "<company_number>" does not already have an officers link
    When  a PATCH request is sent to the add officers link endpoint for "<company_number>"
    Then  the response code should be 200
    And   the officers link exists for "<company_number>"

    Examples:
      | data_file                          | company_number |
      | without_officers_link_resource     | 00006400       |

  Scenario Outline: Add officers link unsuccessfully - company profile resource does not exist

    When  a PATCH request is sent to the add officers link endpoint for "<company_number>"
    Then  the response code should be 404

    Examples:
      | company_number |
      | 00006400       |

  Scenario Outline: Add officers link unsuccessfully - user not authenticated or authorised

    When  a PATCH request is sent to "<company_number>" without ERIC headers
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
      | data_file                        | company_number |
      | with_officers_link_resource      | 00006400       |

  Scenario Outline: Add officers link unsuccessfully - CHS Kafka API is unavailable

    Given CHS Kafka API is unavailable
    And   the company profile resource "<data_file>" exists for "<company_number>"
    When  a PATCH request is sent to the add officers link endpoint for "<company_number>"
    Then  the response code should be 503

    Examples:
      | data_file                          | company_number |
      | without_officers_link_resource     | 00006400       |

  Scenario Outline: Add officers link unsuccessfully - MongoDB is unavailable

    Given MongoDB is unavailable
    When  a PATCH request is sent to the add officers link endpoint for "<company_number>"
    Then  the response code should be 503

    Examples:
      | company_number |
      | 00006400       |