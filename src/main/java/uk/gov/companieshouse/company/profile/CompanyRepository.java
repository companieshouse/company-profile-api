package uk.gov.companieshouse.company.profile;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import uk.gov.companieshouse.company.profile.domain.CompanyProfileDao;

public interface CompanyRepository extends MongoRepository<CompanyProfileDao, String> {

    @Query("{'companyProfile.data.companyNumber':?0}")
    CompanyProfileDao findByCompanyNumber(String companyNumber);
}
