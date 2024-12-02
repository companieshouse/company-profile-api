Feature: Process company profile

  Scenario Outline: Get Company Profile when sending get request

    Given Company profile api service is running
    And a Company Profile exists for "<companyNumber>"
    When I send GET request to retrieve Company Profile using company number "<companyNumber>"
    Then I should receive 200 status code
    And the Get call response body should match "<result>" file

    Examples:
      | companyNumber         | result                            |
      | 00006402              | 00006402-getCompanyProfile        |

  Scenario Outline: Get Company Profile when sending get request without Eric headers

    Given Company profile api service is running
    And a Company Profile exists for "<companyNumber>"
    When I send GET request to retrieve Company Profile using company number "<companyNumber>" without setting Eric headers
    Then I should receive 401 status code

    Examples:
      | companyNumber     |
      | 00006402          |

  @Ignored
    #    Scenario does not work correctly due to potential issue with API Client library and Apache Client 5 dependency
  Scenario Outline: GET company profile unsuccessfully - company profile resource does not exist
    Given a company profile resource does not exist for "<company_number>"
    When I send GET request to retrieve Company Profile using company number "<company_number>"
    Then the response code should be 404

    Examples:
      | company_number  |
      | 00006402        |

  @Ignored
    #    Scenario does not work correctly due to potential issue with API Client library and Apache Client 5 dependency
  Scenario Outline: GET company profile unsuccessfully while database is down
    Given Company profile api service is running
    And a Company Profile exists for "<company_number>"
    And the company profile database is down
    When I send GET request to retrieve Company Profile using company number "<company_number>"
    Then the response code should be 503

    Examples:
      | company_number |
      | 00006402       |

  Scenario Outline: Processing company profile information successfully

    Given Company profile api service is running
    And the CHS Kafka API is reachable
    When I send a PUT request with payload "<companyNumber>" file for company number "<companyNumber>"
    Then I should receive 200 status code
    And a company profile exists with id "<companyNumber>"
    And the CHS Kafka API is invoked successfully

    Examples:
      | companyNumber     |
      | 00006402          |

  Scenario Outline: Processing stale company profile information returns HTTP 409 conflict

    Given Company profile api service is running
    And the CHS Kafka API is reachable
    And the company profile resource "<companyNumber>" exists for "<companyNumber>"
    When I send a PUT request with payload "<staleDeltaRequest>" file for company number "<companyNumber>"
    Then I should receive 409 status code
    And the CHS Kafka API is not invoked

    Examples:
      | companyNumber | staleDeltaRequest         |
      | 00006402      | 00006402_missing_delta_at |
      | 00006402      | 00006402_stale            |

  Scenario Outline: Processing company profile information successfully when has_charges is null

    Given Company profile api service is running
    And the CHS Kafka API is reachable
    When I send a PUT request with payload "<companyNumber>" file for company number "<companyNumber>"
    Then I should receive 200 status code
    And a company profile exists with id "<companyNumber>"
    And has_charges is false for "<companyNumber>"
    And the CHS Kafka API is invoked successfully

    Examples:
      | companyNumber     |
      | 00006402          |

  Scenario Outline: Processing company profile information with bad payload

    Given Company profile api service is running
    And the CHS Kafka API is reachable
    When I send a PUT request with payload "<companyNumber>" file for company number "<companyNumber>"
    Then I should receive 400 status code
    And the CHS Kafka API is not invoked

    Examples:
      | companyNumber         |
      | 00006402_bad_payload  |

  Scenario Outline: Process Company Profile when sending put request without Eric headers

    Given Company profile api service is running
    And the CHS Kafka API is reachable
    When I send a PUT request with payload "<companyNumber>" file for company number "<companyNumber>" without setting Eric headers
    Then I should receive 401 status code
    And the CHS Kafka API is not invoked

    Examples:
      | companyNumber     |
      | 00006402          |

  Scenario Outline: Processing company profile information with insufficient access

    Given Company profile api service is running
    And the CHS Kafka API is reachable
    When I send a PUT request with payload "<companyNumber>" file for company number "<companyNumber>" with insufficient access
    Then I should receive 403 status code
    And the CHS Kafka API is not invoked

    Examples:
      | companyNumber         |
      | 00006402_bad_payload  |

  @Ignored
    #    Scenario does not work correctly due to potential issue with API Client library and Apache Client 5 dependency
  Scenario Outline: Process company profile unsuccessfully while database is down
    Given Company profile api service is running
    And a company profile resource does not exist for "<company_number>"
    And the company profile database is down
    And the CHS Kafka API is reachable
    When I send a PUT request with payload "<company_number>" file for company number "<company_number>"
    Then the response code should be 503
    And the CHS Kafka API is not invoked

    Examples:
      | company_number |
      | 00006402       |


  Scenario Outline: Process company profile unsuccessfully when kafka-api is down
    Given Company profile api service is running
    And CHS kafka API service is unavailable
    When I send a PUT request with payload "<companyNumber>" file for company number "<companyNumber>"
    Then I should receive 503 status code

    Examples:
      | companyNumber     |
      | 00006402          |


  Scenario Outline: Get Company Profile returns previous company as null when its empty

    Given Company profile api service is running
    And a Company Profile exists for "<companyNumber>"
    When I send GET request to retrieve Company Profile using company number "<companyNumber>"
    Then I should receive 200 status code
    And the Get call response body should match "<result>" file

    Examples:
      | companyNumber         | result                            |
      | 00006408              | 00006408-getCompanyProfile        |
