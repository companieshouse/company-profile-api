package uk.gov.companieshouse.company.profile.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import uk.gov.companieshouse.company.profile.model.CompanyProfileDocument;

public interface CompanyProfileRepository extends MongoRepository<CompanyProfileDocument, String> {
    // company number is not a direct property of the entity so traversal is required
    Optional<CompanyProfileDocument> findCompanyProfileDaoByCompanyProfile_Data_CompanyNumber(
            String companyNumber);
}
