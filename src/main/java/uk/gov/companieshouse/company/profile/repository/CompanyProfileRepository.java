package uk.gov.companieshouse.company.profile.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import uk.gov.companieshouse.api.model.CompanyProfileDocument;

public interface CompanyProfileRepository extends MongoRepository<CompanyProfileDocument, String> {
    @Query("{'parent_company_number' : '?0'}")
    List<CompanyProfileDocument> findAllByParentCompanyNumber(String parentCompanyNumber);
}
