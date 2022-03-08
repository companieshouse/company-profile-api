package uk.gov.companieshouse.company.profile;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import uk.gov.companieshouse.company.profile.domain.CompanyProfileDao;

import java.util.Optional;

public interface CompanyRepository extends MongoRepository<CompanyProfileDao, String> {

    @Query("{'companyProfile.data.companyNumber':?0}")
    CompanyProfileDao findByCompanyNumber(String companyNumber);

    // company number is not a direct property of the entity so traversal is required
    Optional<CompanyProfileDao> findCompanyProfileDaoByCompanyProfile_Data_CompanyNumber(
            String companyNumber);
}
