package uk.gov.companieshouse.company.profile.configuration;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.companieshouse.company.profile.CompanyProfileApiApplication;

@CucumberContextConfiguration
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = CompanyProfileApiApplication.class)
@DirtiesContext
@ActiveProfiles({"test"})
public class CucumberSpringConfiguration extends AbstractMongoConfig {
}
