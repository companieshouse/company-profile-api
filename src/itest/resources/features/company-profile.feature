Feature: Process company profile links

  Scenario Outline: Processing company profile links successfully

    Given Company profile api service is running
    And the company links exists for "<data>"
    When I send PATCH request with payload "<data>" and company number "<data>"
    Then the CHS Kafka API is invoked successfully
    And I should receive 200 status code

    Examples:
      | data     |
      | 11748564 |

  Scenario Outline: Processing company profile links without setting Eric headers

    Given Company profile api service is running
    And the company links exists for "<data>"
    When I send PATCH request with payload "<data>" and company number "<data>" without setting Eric headers
    Then I should receive 401 status code

    Examples:
      | data     |
      | 11748564 |

  Scenario Outline: Retrieve company links successfully

    Given Company profile api service is running
    And the company links exists for "<data>"
    When I send GET request with company number "<data>"
    Then I should receive 200 status code
    And the Get call response body should match "<result>" file

    Examples:
      | data                  | result                            |
      | 00006400              | 00006400-getResponse              |
      | 00006401              | 00006401-getResponse              |

  Scenario Outline: Retrieve company links without setting Eric headers

    Given Company profile api service is running
    And the company links exists for "<data>"
    When I send GET request with company number "<data>" without setting Eric headers
    Then I should receive 401 status code

    Examples:
      | data                  |
      | 00006400              |

  Scenario Outline: Get Company Profile when sending get request

    Given Company profile api service is running
    And a Company Profile exists for "<companyNumber>"
    When I send GET request to retrieve Company Profile using company number "<companyNumber>"
    Then I should receive 200 status code
    And the Get call response body should match "<result>" file

    Examples:
      | companyNumber         | result                            |
      | 00006402              | 00006402                          |



  Scenario Outline: Get Company Profile when sending get request without Eric headers

    Given Company profile api service is running
    And a Company Profile exists for "<companyNumber>"
    When I send GET request to retrieve Company Profile using company number "<companyNumber>" without setting Eric headers
    Then I should receive 401 status code
    
    Examples:
      | companyNumber         |
      | 00006400              |
