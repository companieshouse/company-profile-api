Feature: Healthcheck API endpoint

  Scenario: Client invokes GET /healthcheck endpoint
    Given Company profile api service is running
    When the client invokes '/company-profile-api/healthcheck' endpoint
    Then the client receives a status code of 200
    And the client receives a response body of '{"status":"UP"}'