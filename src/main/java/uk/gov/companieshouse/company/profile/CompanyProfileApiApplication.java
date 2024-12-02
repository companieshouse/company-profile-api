package uk.gov.companieshouse.company.profile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories
public class CompanyProfileApiApplication {

    public static final String APPLICATION_NAME_SPACE = "company-profile-api";

    public static void main(String[] args) {
        SpringApplication.run(CompanyProfileApiApplication.class, args);
    }

}