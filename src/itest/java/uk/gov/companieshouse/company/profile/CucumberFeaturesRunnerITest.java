package uk.gov.companieshouse.company.profile;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.runner.RunWith;
import uk.gov.companieshouse.company.profile.configuration.AbstractIntegrationTest;
import uk.gov.companieshouse.company.profile.configuration.CucumberSpringConfiguration;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/itest/resources/features",
        plugin = {"pretty",
                "json:target/cucumber-report.json"
        })
@CucumberContextConfiguration
public class CucumberFeaturesRunnerITest extends AbstractIntegrationTest {
}
