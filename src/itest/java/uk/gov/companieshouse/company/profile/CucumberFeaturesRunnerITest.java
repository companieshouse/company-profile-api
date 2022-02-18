package uk.gov.companieshouse.company.profile;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/itest/resources/features",
        plugin = {"pretty",
                "json:target/cucumber-report.json"
        },
        glue = {"uk.gov.companieshouse.company.profile.steps",
                "uk.gov.companieshouse.company.profile.configuration"})
public class CucumberFeaturesRunnerITest {
}
