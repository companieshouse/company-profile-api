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

  Scenario Outline: Processing company profile information successfully

    Given Company profile api service is running
    When I send a PUT request with payload "<companyNumber>" file for company number "<companyNumber>"
    Then I should receive 200 status code
    And the CHS Kafka API is invoked for company number "<companyNumber>"
    And a company profile exists with id "<companyNumber>"

    Examples:
      | companyNumber     |
      | 00006402          |

  Scenario Outline: Process Company Profile when sending put request without Eric headers

    Given Company profile api service is running
    When I send a PUT request with payload "<companyNumber>" file for company number "<companyNumber>" without setting Eric headers
    Then I should receive 401 status code

    Examples:
      | companyNumber     |
      | 00006402          |
