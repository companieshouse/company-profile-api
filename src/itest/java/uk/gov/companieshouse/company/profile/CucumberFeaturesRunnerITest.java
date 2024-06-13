package uk.gov.companieshouse.company.profile;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestPropertySource;
import uk.gov.companieshouse.company.profile.configuration.AbstractIntegrationTest;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/itest/resources/features",
        plugin = {"pretty", "json:target/cucumber-report.json"},
        tags = "not @Ignored")
@CucumberContextConfiguration
@TestPropertySource(properties = {"mongodb.transactional = true"})
public class CucumberFeaturesRunnerITest extends AbstractIntegrationTest {
}
