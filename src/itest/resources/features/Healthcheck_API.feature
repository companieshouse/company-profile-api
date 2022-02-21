Feature: Healthcheck API endpoint

  Scenario: Client invokes GET /healthcheck endpoint
    Given the application is running
    When the client invokes '/healthcheck' endpoint
    Then the client receives a status code of 200
    And the client receives a response body of 'OK'