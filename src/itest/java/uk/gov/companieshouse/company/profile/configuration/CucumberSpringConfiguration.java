package uk.gov.companieshouse.company.profile.configuration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.companieshouse.company.profile.CompanyProfileApiApplication;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = CompanyProfileApiApplication.class)
@DirtiesContext
@ActiveProfiles({"test"})
public class CucumberSpringConfiguration extends AbstractIntegrationTest {
}
