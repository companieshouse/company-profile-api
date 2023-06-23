package uk.gov.companieshouse.company.profile.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uk.gov.companieshouse.company.profile.model.CompanyProfileDocument;

import java.util.Optional;

public interface CompanyProfileRepository extends MongoRepository<CompanyProfileDocument, String> {
    Optional<CompanyProfileDocument> getDataByCompanyNumber(String companyNumber);
}
