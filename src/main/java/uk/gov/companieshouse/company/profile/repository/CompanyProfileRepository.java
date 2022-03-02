package uk.gov.companieshouse.company.profile.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uk.gov.companieshouse.api.model.company.CompanyProfileApi;

// TODO Update with newly generated CompanyProfile object once DSND-524 is completed
public interface CompanyProfileRepository extends MongoRepository<CompanyProfileApi, String> {
}
