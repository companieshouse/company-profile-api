Feature: Delete UK establishments link in company profile

  Scenario Outline: Delete Uk establishment and link (multiple establishments)

    Given the CHS Kafka API is reachable
    And UK establishments exists for parent company with number "<company_number>"
    And a UK establishment link does exist for "<company_number>"
    When a DELETE request is sent to the company profile endpoint for "<company_to_delete>"
    Then the response code should be 200
    And the UK establishment link should still exist for "<company_number>"

    Examples:
      | company_number | company_to_delete |
      | 00006406       |  00006404         |

  Scenario Outline: Delete Uk establishment and link (single establishments)

    Given the CHS Kafka API is reachable
    And a single UK establishment exist for parent company with number "<company_number>"
    And a UK establishment link does exist for "<company_number>"
    When a DELETE request is sent to the company profile endpoint for "<company_to_delete>"
    Then the response code should be 200
    And the UK establishment link should be removed from "<company_number>"

    Examples:
      | company_number | company_to_delete |
      | 00006406       | 00006404          |

  Scenario Outline: Delete UK establishments link successfully

    Given the CHS Kafka API is reachable
    And the company profile resource "<data_file>" exists for "<company_number>"
    And a UK establishment link does exist for "<company_number>"
    When a PATCH request is sent to the delete UK establishments link endpoint for "<company_number>"
    Then the response code should be 200
    And the UK establishment link should be removed from "<company_number>"

    Examples:
      | data_file               | company_number |
      | with_links_resource     | 00006400       |

  Scenario Outline: Delete UK establishments link unsuccessfully - still contains more than one UK establishment

    Given UK establishments exists for parent company with number "<company_number>"
    And a UK establishment link does exist for "<company_number>"
    When a PATCH request is sent to the delete UK establishments link endpoint for "<company_number>"
    Then the response code should be 200
    And the UK establishment link should still exist for "<company_number>"

    Examples:
      | company_number |
      | 00006406       |

  Scenario Outline: Delete UK establishments link unsuccessfully - company profile resource does not exist

    Given a company profile resource does not exist for "<company_number>"
    When a PATCH request is sent to the delete UK establishments link endpoint for "<company_number>"
    Then the response code should be 404

    Examples:
      | company_number |
      | 00006400       |

  Scenario Outline: Delete UK establishments link unsuccessfully - user not authenticated or authorised

    When a PATCH request is sent to the delete UK establishments endpoint for "<company_number>" without ERIC headers
    Then the response code should be 401

    Examples:
      | company_number |
      | 00006400       |

  Scenario Outline: Delete UK establishments link unsuccessfully - link already does not exist

    Given the company profile resource "<data_file>" exists for "<company_number>"
    And a UK establishment link does not exist for "<company_number>"
    When a PATCH request is sent to the delete UK establishments link endpoint for "<company_number>"
    Then the response code should be 409

    Examples:
      | data_file                   | company_number |
      | without_links_resource      | 00006400       |

  Scenario Outline: Delete UK establishments link unsuccessfully - CHS Kafka API is unavailable

    Given CHS kafka API service is unavailable
    And the company profile resource "<data_file>" exists for "<company_number>"
    When a PATCH request is sent to the delete UK establishments link endpoint for "<company_number>"
    Then the response code should be 503

    Examples:
      | data_file               | company_number |
      | with_links_resource     | 00006400       |

  Scenario Outline: Delete UK establishments link unsuccessfully - the database is unavailable

    Given the company profile database is down
    When a PATCH request is sent to the delete UK establishments link endpoint for "<company_number>"
    Then the response code should be 503

    Examples:
      | company_number |
      | 00006400       |