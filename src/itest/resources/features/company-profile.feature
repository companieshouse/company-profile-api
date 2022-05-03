Feature: Process company profile links

#  Scenario Outline: Processing company profile links successfully
#
#    Given Company profile api service is running
#    When I send PATCH request with payload "<data>" and company number "<data>"
#    Then I should receive 200 status code
#    And the expected result should match "<result>" file with company number "<data>"
#    When I send PATCH request with payload "<data>" and company number "<data>"
#    Then I should receive 200 status code
#    And the expected result should match "<result>" file with company number "<data>"

#    Examples:
#      | data     | result              |
#      | 00006400 | 00006400-collection |

  Scenario Outline: Retrieve company links successfully

    Given Company profile api service is running
    And the company links exists for "<data>"
    When I send GET request with company number "<data>"
    Then I should receive 200 status code
    And the Get call response body should match "<result>" file

    Examples:
      | data                  | result                            |
      | 00006400              | 00006400-getResponse              |
