package uk.gov.companieshouse.company.profile.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uk.gov.companieshouse.company.profile.model.CompanyProfileDocument;

public interface CompanyProfileRepository extends MongoRepository<CompanyProfileDocument, String> {
}
