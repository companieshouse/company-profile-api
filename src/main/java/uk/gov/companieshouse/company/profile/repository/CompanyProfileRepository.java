package uk.gov.companieshouse.company.profile.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import uk.gov.companieshouse.company.profile.domain.CompanyProfileDao;

public interface CompanyProfileRepository extends MongoRepository<CompanyProfileDao, String> {
    // company number is not a direct property of the entity so traversal is required
    Optional<CompanyProfileDao> findCompanyProfileDaoByCompanyProfile_Data_CompanyNumber(
            String companyNumber);
}
